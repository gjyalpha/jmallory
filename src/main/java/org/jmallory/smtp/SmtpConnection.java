package org.jmallory.smtp;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.jmallory.Main;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.io.SocketRR;
import org.jmallory.model.Connection;
import org.jmallory.swing.EncoderDecoderTextEditor;
import org.jmallory.swing.JTextAreaX;
import org.jmallory.util.Utils;

public class SmtpConnection extends Thread implements Connection {

    SmtpListener      listener;

    volatile String   state        = "Active";
    volatile boolean  active;
    volatile boolean  error;
    volatile String   fromHost     = "";
    volatile String   time         = "";
    volatile String   emailFrom    = "<from>";
    volatile String   emailTitle   = "<title>";
    volatile long     elapsedTime;

    JTextArea         inputText    = null;
    JScrollPane       inputScroll  = null;
    JTextArea         outputText   = null;
    JScrollPane       outputScroll = null;

    Socket            inSocket     = null;
    Socket            outSocket    = null;
    SocketRR          rr1;
    SocketRR          rr2;

    Thread            clientThread = null;
    Thread            serverThread = null;

    InputStream       inputStream  = null;
    OutputStream      outputStream = null;

    MockSmtpd         smtpd        = null;

    SlowLinkSimulator slowLink;

    /**
     * Constructor Connection
     * 
     * @param l
     */
    public SmtpConnection(SmtpListener l) {
        listener = l;
        slowLink = l.slowLink;
    }

    /**
     * Constructor Connection
     * 
     * @param l
     * @param s
     */
    public SmtpConnection(SmtpListener l, Socket s) {
        this(l);
        inSocket = s;
        start();
    }

    /**
     * Constructor Connection
     * 
     * @param l
     * @param in
     */
    public SmtpConnection(SmtpListener l, InputStream in) {
        this(l);
        inputStream = in;
        start();
    }

    /**
     * Constructor Connection
     * 
     * @param l
     * @param in
     */
    public SmtpConnection(SmtpListener l, InputStream in, OutputStream out) {
        this(l);
        inputStream = in;
        outputStream = out;
        start();
    }

    /**
     * Method run
     */
    public void run() {
        try {
            active = true;
            if (inSocket != null) {
                fromHost = (inSocket.getInetAddress()).getHostName();
            } else {
                fromHost = "resend";
            }
            String dateformat = "yyyy-MM-dd HH:mm:ss";
            DateFormat df = new SimpleDateFormat(dateformat);
            time = df.format(new Date());

            // we populate these Swing components in current thread, since they will used soon
            inputText = new JTextAreaX(null, null, 20, 80);
            inputText.setEditable(false);
            inputText.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(Main
                    .getFrame(), inputText, null, true));
            inputScroll = new JScrollPane();
            inputScroll.setViewportView(inputText);
            outputText = new JTextAreaX(null, null, 20, 80);
            outputText.setEditable(false);
            outputText.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(Main
                    .getFrame(), outputText, null, true));
            outputScroll = new JScrollPane();
            outputScroll.setViewportView(outputText);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ListSelectionModel lsm = listener.connectionTable.getSelectionModel();
                    if ((listener.tableModel.size() == 0) || (lsm.getLeadSelectionIndex() == 0)) {
                        listener.outPane.setVisible(false);
                        int divLoc = listener.outPane.getDividerLocation();
                        listener.setLeft(inputScroll);
                        listener.setRight(outputScroll);
                        listener.removeButton.setEnabled(false);
                        listener.removeAllButton.setEnabled(true);
                        listener.saveButton.setEnabled(true);
                        listener.resendButton.setEnabled(true);
                        listener.outPane.setDividerLocation(divLoc);
                        listener.outPane.setVisible(true);
                    }
                    boolean shrinked = listener.tableModel.addConnection(SmtpConnection.this);
                    if (shrinked) {
                        // the table got changed, select the recent row
                        listener.connectionTable.getSelectionModel().setSelectionInterval(0, 0);
                    }
                }
            });
            String targetHost = listener.hostField.getText();
            int targetPort = Integer.parseInt(listener.tPortField.getText());
            if (targetPort == -1) {
                targetPort = 25;
            }

            InputStream tmpIn1 = inputStream;
            OutputStream tmpOut1 = outputStream;
            InputStream tmpIn2 = null;
            OutputStream tmpOut2 = null;
            if (tmpIn1 == null) {
                tmpIn1 = inSocket.getInputStream();
            }
            if (inSocket != null && tmpOut1 == null) {
                tmpOut1 = inSocket.getOutputStream();
            }

            final long start = System.currentTimeMillis();

            if (listener.mockSmtpCheckBox.isSelected()) {
                smtpd = new MockSmtpd(new SmtpFilterInputStream(tmpIn1, this), tmpOut1, inputText,
                        outputText);
                smtpd.run(); // run the smtpd in the current thread.
            } else {
                outSocket = new Socket();
                outSocket.setReuseAddress(true);
                outSocket.connect(new InetSocketAddress(targetHost, targetPort));

                tmpIn2 = outSocket.getInputStream();
                tmpOut2 = outSocket.getOutputStream();

                // this is the channel to the endpoint
                rr1 = new SocketRR(this, inSocket, new SmtpFilterInputStream(tmpIn1, this),
                        outSocket, tmpOut2, inputText, listener.tableModel, slowLink);

                // create the response slow link from the inbound slow link
                SlowLinkSimulator responseLink = new SlowLinkSimulator(slowLink);

                // this is the channel from the endpoint
                rr2 = new SocketRR(this, outSocket, tmpIn2, inSocket, tmpOut1, outputText, null,
                        responseLink);

                while ((rr1 != null) || (rr2 != null)) {

                    // Only loop as long as the connection to the target
                    // machine is available - once that's gone we can stop.
                    // The old way, loop until both are closed, left us
                    // looping forever since no one closed the 1st one.

                    if ((null != rr1) && rr1.isDone()) {
                        if (rr2 != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    state = "Resp";
                                    listener.tableModel.fireTableRowsUpdated(SmtpConnection.this);
                                }
                            });
                        }
                        rr1 = null;
                    }

                    if ((null != rr2) && rr2.isDone()) {
                        if (rr1 != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    state = "Req";
                                    listener.tableModel.fireTableRowsUpdated(SmtpConnection.this);
                                }
                            });
                        }
                        rr2 = null;
                    }

                    synchronized (this) {
                        this.wait(100); // Safety just incase we're not told to wake up.
                    }
                }
            }
            final long end = System.currentTimeMillis();

            active = false;
            elapsedTime = end - start;

        } catch (Exception e) {
            e.printStackTrace();
            final StringWriter st = new StringWriter();
            PrintWriter wr = new PrintWriter(st);
            error = true;
            e.printStackTrace(wr);
            wr.close();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (outputText != null) {
                        outputText.append(st.toString());
                        outputText.setCaretPosition(0);
                    }
                }
            });
        } finally {
            halt();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    state = error ? "Error" : (smtpd != null ? "Mock" : "Done");
                    listener.tableModel.fireTableRowsUpdated(SmtpConnection.this);
                }
            });
        }
    }

    /**
     * Method wakeUp
     */
    public synchronized void wakeUp() {
        this.notifyAll();
    }

    /**
     * Method halt
     */
    public void halt() {
        try {
            if (inSocket != null) {
                inSocket.close();
            }
            inSocket = null;
            if (outSocket != null) {
                outSocket.close();
            }
            outSocket = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class SmtpFilterInputStream extends FilterInputStream {
        private SmtpConnection        conn;
        private boolean               from  = false;
        private boolean               title = false;
        private ByteArrayOutputStream baos  = new ByteArrayOutputStream(1024);

        public SmtpFilterInputStream(InputStream in, SmtpConnection conn) {
            super(in);
            this.conn = conn;
        }

        @Override
        public int read() throws IOException {
            int read = in.read();
            onByte(read);
            return read;
        }

        private void onByte(int read) throws UnsupportedEncodingException {
            if (read == -1) {
                String line = new String(baos.toByteArray(), Utils.CHARSET);
                if (line.length() != 0) {
                    onSmtpLine(line);
                }
                baos.reset();
            } else if (read == '\r') {
                // do nothing
            } else if (read == '\n') {
                // we got a line
                onSmtpLine(new String(baos.toByteArray(), Utils.CHARSET));
                baos.reset();
            } else {
                baos.write(read);
            }
        }

        private void onSmtpLine(String line) {
            if (!from && line.startsWith(MockSmtpd.MAIL_FROM)) {
                int start = Math.min(MockSmtpd.MAIL_FROM.length() + 2, line.length());
                String emailFrom = line.substring(start);
                conn.emailFrom = emailFrom;
                from = true; // we have done from
            }

            if (!title && line.startsWith(MockSmtpd.SUBJECT)) {
                // we got the subject, set it into table
                // =?UTF-8?B?ZHNhZ2FzZGdhcw==?=
                String[] parts = line.split("\\?");
                try {
                    if (parts.length == 5) {
                        if ("B".equalsIgnoreCase(parts[2])) {
                            // base64
                            conn.emailTitle = new String(Base64.decodeBase64(parts[3].getBytes()),
                                    parts[1]);
                        } else if ("Q".equalsIgnoreCase(parts[2])) {
                            // quoted-printable
                            conn.emailTitle = new String(
                                    QuotedPrintableCodec.decodeQuotedPrintable(parts[3].getBytes()),
                                    parts[1]);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    conn.emailTitle = "<" + e.getMessage() + ">";
                }
                title = true;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = in.read(b, off, len);

            for (int i = off, length = off + read; i < length; i++) {
                onByte(b[i]);
            }
            return read;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }
}

package org.jmallory.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jmallory.Main;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.io.SocketRR2;
import org.jmallory.model.Connection;
import org.jmallory.swing.EncoderDecoderTextEditor;
import org.jmallory.swing.JTextAreaX;

public class TcpConnection extends Thread implements Connection {

    TcpListener               listener;

    volatile String           state        = "Active";
    volatile boolean          active;
    boolean                   error;

    String                    fromHost;
    int                       incomeBytes;
    int                       outcomeBytes;

    String                    time;
    long                      elapsedTime;

    JTextAreaX                inputText    = null;
    JScrollPane               inputScroll  = null;

    JTextAreaX                outputText   = null;
    JScrollPane               outputScroll = null;

    Socket                    inSocket     = null;
    Socket                    outSocket    = null;

    Thread                    clientThread = null;
    Thread                    serverThread = null;

    InputStream               inputStream  = null;
    OutputStream              outputStream = null;

    private SlowLinkSimulator slowLink;

    private SocketRR2         rr1;
    private SocketRR2         rr2;

    /**
     * Constructor Connection
     * 
     * @param l
     */
    public TcpConnection(TcpListener l) {
        listener = l;
        slowLink = l.slowLink;
    }

    /**
     * Constructor Connection
     * 
     * @param l
     * @param s
     */
    public TcpConnection(TcpListener l, Socket s) {
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
    public TcpConnection(TcpListener l, InputStream in) {
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
    public TcpConnection(TcpListener l, InputStream in, OutputStream out) {
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
                fromHost = inSocket.getInetAddress().getHostAddress() + ":" + inSocket.getPort();
            } else {
                fromHost = "resend";
            }
            String dateformat = "yyyy-MM-dd HH:mm:ss";
            DateFormat df = new SimpleDateFormat(dateformat);
            time = df.format(new Date());

            // we populate these Swing components in current thread, since they will used soon
            inputText = new JTextAreaX(null, null, 20, 80);
            inputText.setEditable(false);
            inputText.setLineWrap(true);
            inputText.setLimitCharacters(80000);
            inputText.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(Main
                    .getFrame(), inputText, null, true));
            inputScroll = new JScrollPane();
            inputScroll.setViewportView(inputText);
            outputText = new JTextAreaX(null, null, 20, 80);
            outputText.setEditable(false);
            outputText.setLineWrap(true);
            outputText.setLimitCharacters(80000);
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
                        listener.outPane.setDividerLocation(divLoc);
                        listener.outPane.setVisible(true);
                    }
                    boolean shrinked = listener.tableModel.addConnection(TcpConnection.this);
                    if (shrinked) {
                        // the table got changed, select the recent row
                        listener.connectionTable.getSelectionModel().setSelectionInterval(0, 0);
                    }
                }
            });

            String targetHost = listener.hostField.getText();
            int targetPort = Integer.parseInt(listener.tPortField.getText());

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

            outSocket = new Socket();
            outSocket.setReuseAddress(true);
            outSocket.connect(new InetSocketAddress(targetHost, targetPort));

            tmpIn2 = outSocket.getInputStream();
            tmpOut2 = outSocket.getOutputStream();

            final CountDownLatch countDown = new CountDownLatch(2);

            long start = System.currentTimeMillis();

            rr1 = new SocketRR2(tmpIn1, tmpOut2, new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    incomeBytes += len;
                    if (slowLink != null) {
                        slowLink.pump(len);
                    }
                    inputText.append(new String(b, off, len));
                }

            }, new SocketRR2.Listener() {
                @Override
                public void done() {
                    state = "Income Done";
                    countDown.countDown();
                }
            });

            rr2 = new SocketRR2(tmpIn2, tmpOut1, new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    outcomeBytes += len;
                    if (slowLink != null) {
                        slowLink.pump(len);
                    }
                    outputText.append(new String(b, off, len));
                }

            }, new SocketRR2.Listener() {
                @Override
                public void done() {
                    state = "Outcome Done";
                    countDown.countDown();
                }
            });

            rr1.start();
            rr2.start();

            countDown.await();

            long end = System.currentTimeMillis();

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

        if (rr1 != null) {
            rr1.halt();
            rr1 = null;
        }

        if (rr2 != null) {
            rr2.halt();
            rr2 = null;
        }

        if (inSocket != null) {
            try {
                inSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            inSocket = null;
        }

        if (outSocket != null) {
            try {
                outSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            outSocket = null;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                state = error ? "Error" : "Done";
                active = false;
                listener.tableModel.fireTableRowsUpdated(TcpConnection.this);
            }
        });
    }

    @Override
    public boolean isActive() {
        return active;
    }
}

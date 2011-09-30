package org.jmallory.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jmallory.Main;
import org.jmallory.io.LineInputStream;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.model.Connection;
import org.jmallory.swing.EncoderDecoderTextEditor;
import org.jmallory.swing.JTextAreaX;
import org.jmallory.util.Utils;

public class HttpConnection extends Thread implements Connection {

    volatile String           state          = "Active";

    HttpListener              listener;

    /**
     * Field active
     */
    boolean                   active;
    boolean                   error;

    String                    time           = "";
    String                    fromHost       = "";
    String                    targetHost     = "";
    volatile String           requestSummary = "";

    volatile long             elapsedTime;

    JTextArea                 inputText      = null;
    JScrollPane               inputScroll    = null;

    JTextArea                 outputText     = null;
    JScrollPane               outputScroll   = null;

    Socket                    inSocket       = null;
    Socket                    outSocket      = null;

    Thread                    clientThread   = null;
    Thread                    serverThread   = null;

    InputStream               inputStream    = null;

    HttpRequest               request;
    String                    requestString;
    HttpResponse              response;
    String                    responseString;

    /**
     * Field slowLink
     */
    private SlowLinkSimulator slowLink;

    boolean                   mockResponse   = false;

    private String            mockDataProfileName;

    /**
     * Constructor Connection
     * 
     * @param l
     */
    public HttpConnection(HttpListener l) {
        listener = l;
        slowLink = l.slowLink;
    }

    /**
     * Constructor Connection
     * 
     * @param l
     * @param s
     */
    public HttpConnection(HttpListener l, Socket s) {
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
    public HttpConnection(HttpListener l, InputStream in) {
        this(l);
        inputStream = in;
        start();
    }

    /**
     * Method run
     */
    public void run() {
        try {
            active = true;

            if (inSocket != null) {
                fromHost = (inSocket.getInetAddress()).getHostAddress();
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
                        listener.saveToClipboardButton.setEnabled(true);
                        listener.addToProfileButton.setEnabled(true);
                        listener.profileComboBox.setEnabled(true);
                        listener.resendButton.setEnabled(true);
                        listener.outPane.setDividerLocation(divLoc);
                        listener.outPane.setVisible(true);
                    }
                    boolean shrinked = listener.tableModel.addConnection(HttpConnection.this);
                    if (shrinked) {
                        // the table got changed, select the recent row
                        listener.connectionTable.getSelectionModel().setSelectionInterval(0, 0);
                    }
                }
            });

            targetHost = listener.hostField.getText();
            int targetPort = Integer.parseInt(listener.tPortField.getText());
            if (targetPort == -1) {
                targetPort = 80;
            }

            InputStream tmpIn1 = inputStream;
            OutputStream tmpOut1 = null;
            InputStream tmpIn2 = null;
            OutputStream tmpOut2 = null;
            if (tmpIn1 == null) {
                tmpIn1 = inSocket.getInputStream();
            }
            if (inSocket != null) {
                tmpOut1 = inSocket.getOutputStream();
            }

            // now we need to turn the old stream to stream manner to store-and-forward manner, since we need to first
            // get the entire request, then check if we could handle this request with mock data. If it can't be handled,
            // we will forward it as usual

            LineInputStream requestReader = new LineInputStream(tmpIn1, true);
            request = Utils.readRequest(requestReader);

            if (listener.useProxy != null && listener.useProxy.isSelected()
                    && listener.HTTPProxyHost != null) {
                // we need to use a proxy
                StringBuilder newRequestUrl = new StringBuilder(HttpMessage.HTTP_PROTOCOL_PREFIX)
                        .append(targetHost);
                if (targetPort != 80) {
                    newRequestUrl.append(HttpMessage.HTTP_PORT_SEPARATOR).append(targetPort);
                }
                newRequestUrl.append(request.getRequestUrl());
                request.setRequestLine(request.getMethod(), newRequestUrl.toString(),
                        request.getHttpVersion());

                targetHost = listener.HTTPProxyHost;
                targetPort = listener.HTTPProxyPort;
            }

            if (!listener.cloneHostHeader.isSelected()) {
                if (80 == targetPort) {
                    request.addHeader(HttpMessage.HTTP_HOST, listener.hostField.getText());
                } else {
                    request.addHeader(HttpMessage.HTTP_HOST, listener.hostField.getText()
                            + HttpMessage.HTTP_PORT_SEPARATOR + targetPort);
                }
            }

            request.normalizeContentLength();

            requestString = request.toUrlDecodedString(listener.jsonFormatBox.isSelected());

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    inputText.setText(requestString);
                    inputText.setCaretPosition(0);
                    if (request.getRequestUrl() != null) {
                        String requestLine = request.getMethodRequestUrl();
                        int idx = (requestLine.length() < 50) ? requestLine.length() : 50;
                        requestSummary = requestLine.substring(0, idx);
                    }

                    HttpConnection.this.state = "Req";
                }
            });

            final long start = System.currentTimeMillis();

            Profile[] profiles = listener.mockDataProfiles;
            if (listener.useProfilesCheckBox.isSelected() && profiles != null) {
                for (int i = 0, length = profiles.length; i < length; i++) {
                    Profile profile = profiles[i];
                    HttpResponse result = profile.getDataMap().get(request);
                    if (result != null) {
                        response = result;
                        mockDataProfileName = profile.getName();
                        mockResponse = true;
                        break;
                    }
                }
            }

            if (response == null) {
                // do the tunnel as usual

                boolean useSSLTarget = listener.httpsTarget.isSelected();
                if (useSSLTarget) {
                    SocketFactory socketFactory = SSLSocketFactory.getDefault();
                    outSocket = socketFactory.createSocket();
                } else {
                    outSocket = new Socket();
                }

                outSocket.setReuseAddress(true);
                outSocket.connect(new InetSocketAddress(targetHost, targetPort));

                tmpIn2 = outSocket.getInputStream();
                tmpOut2 = outSocket.getOutputStream();

                // write request to outSocket
                byte[] b = request.toByteArray();
                tmpOut2.write(b);
                slowLink.pump(b.length);

                // read response from outSocket
                LineInputStream responseReader = new LineInputStream(tmpIn2, true);
                response = Utils.readResponse(responseReader);
            }

            // we are mocking this request, send back the response directly
            byte[] responseBytes = response.toByteArray();
            slowLink.pump(responseBytes.length);
            final long end = System.currentTimeMillis();
            if (tmpOut1 != null) {
                tmpOut1.write(responseBytes);
            }

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
                    state = error ? "Error" : (mockResponse ? "[" + mockDataProfileName + "]"
                            : "Done");
                    if (outputText != null && response != null) {
                        outputText.append(response.toString(listener.jsonFormatBox.isSelected()));
                        outputText.setCaretPosition(0);
                    }
                    active = false;
                    listener.tableModel.fireTableRowsUpdated(HttpConnection.this);
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

    @Override
    public boolean isActive() {
        return active;
    }
}

package org.jmallory.io;

import javax.swing.JTextArea;
import javax.swing.table.TableModel;

import org.jmallory.model.Connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

public class SocketRR extends Thread {

    /**
     * Field inSocket
     */
    Socket            inSocket     = null;

    /**
     * Field outSocket
     */
    Socket            outSocket    = null;

    /**
     * Field textArea
     */
    JTextArea         textArea;

    /**
     * Field in
     */
    InputStream       in           = null;

    /**
     * Field out
     */
    OutputStream      out          = null;

    /**
     * Field done
     */
    volatile boolean  done         = false;

    /**
     * Field tmodel
     */
    volatile long     elapsed      = 0;

    /**
     * Field tmodel
     */
    TableModel        tmodel       = null;

    /**
     * Field myConnection
     */
    Connection        myConnection = null;

    /**
     * Field slowLink
     */
    SlowLinkSimulator slowLink;

    /**
     * Constructor SocketRR
     * 
     * @param c
     * @param inputSocket
     * @param inputStream
     * @param outputSocket
     * @param outputStream
     * @param _textArea
     * @param format
     * @param tModel
     * @param index
     * @param type
     * @param slowLink
     */
    public SocketRR(Connection c, Socket inputSocket, InputStream inputStream, Socket outputSocket,
                    OutputStream outputStream, JTextArea _textArea, TableModel tModel,
                    SlowLinkSimulator slowLink) {
        inSocket = inputSocket;
        in = inputStream;
        outSocket = outputSocket;
        out = outputStream;
        textArea = _textArea;
        tmodel = tModel;
        myConnection = c;
        this.slowLink = slowLink;
        start();
    }

    /**
     * Method isDone
     * 
     * @return boolean
     */
    public boolean isDone() {
        return done;
    }

    public long getElapsed() {
        return elapsed;
    }

    /**
     * Method run
     */
    public void run() {
        try {
            byte[] buffer = new byte[4096];
            int len = 0;
            long start = System.currentTimeMillis();

            // Used to be 1, but if we block it doesn't matter
            // however 1 will break with some servers, including apache
            while ((len = in.read(buffer)) != -1) {
                if (out != null) {
                    slowLink.pump(len);
                    out.write(buffer, 0, len);
                }
                if (textArea != null) {
                    textArea.append(new String(buffer, 0, len));
                }
            }
            elapsed = System.currentTimeMillis() - start;

        } catch (Exception e) {
            e.printStackTrace();
            if (textArea != null) {
                StringWriter st = new StringWriter();
                PrintWriter wr = new PrintWriter(st);
                e.printStackTrace(wr);
                textArea.append(st.toString());
                wr.close();
            }
        } finally {
            done = true;
            try {
                if (out != null) {
                    out.flush();
                    if (null != outSocket && !outSocket.isClosed()) {
                        outSocket.shutdownOutput();
                    }
                    if (out != null) {
                        out.close();
                    }
                    out = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (in != null) {
                    if (inSocket != null && !outSocket.isClosed()) {
                        inSocket.shutdownInput();
                    }
                    if (in != null) {
                        in.close();
                    }
                    in = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            myConnection.wakeUp();
        }
    }

    /**
     * Method halt
     */
    public void halt() {
        try {
            if (inSocket != null) {
                inSocket.close();
            }
            if (outSocket != null) {
                outSocket.close();
            }
            inSocket = null;
            outSocket = null;
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            in = null;
            out = null;
            done = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

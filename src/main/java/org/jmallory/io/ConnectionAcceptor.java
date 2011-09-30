package org.jmallory.io;

import java.awt.Color;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JLabel;

import org.jmallory.model.ConnectionListener;

public class ConnectionAcceptor extends Thread {

    /**
     * Field sSocket
     */
    ServerSocket       sSocket    = null;

    /**
     * Field listener
     */
    ConnectionListener listener;

    /**
     * Field port
     */
    int                port;

    /**
     * Field pleaseStop
     */
    boolean            pleaseStop = false;

    /**
     * Constructor ConnectionAcceptor
     * 
     * @param l
     * @param p
     */
    public ConnectionAcceptor(ConnectionListener l, int p) {
        listener = l;
        port = p;
        start();
    }

    /**
     * Method run
     */
    public void run() {
        try {
            listener.setLeft(new JLabel(" Waiting for Connection..."));
            listener.repaint();
            sSocket = new ServerSocket();
            sSocket.setReuseAddress(true);
            sSocket.bind(new InetSocketAddress(port));

            for (;;) {
                Socket inSocket = sSocket.accept();
                inSocket.setReuseAddress(true);
                if (pleaseStop) {
                    inSocket.close();
                    break;
                }
                listener.newConnection(inSocket);
                inSocket = null;
            }
        } catch (Exception exp) {
            if (!"socket closed".equals(exp.getMessage())) {
                JLabel tmp = new JLabel(exp.toString());
                tmp.setForeground(Color.red);
                listener.setLeft(tmp);
                listener.setRight(new JLabel(""));
                listener.stop();
            }
        }
    }

    /**
     * force a halt by connecting to self and then closing the server socket
     */
    public void halt() {
        try {
            pleaseStop = true;
            new Socket("127.0.0.1", port);
            if (sSocket != null) {
                sSocket.close();
            }
        } catch (Exception e) {
        }
    }
}

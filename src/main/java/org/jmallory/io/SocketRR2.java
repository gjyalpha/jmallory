package org.jmallory.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class SocketRR2 extends Thread {

    public static interface Listener {
        public void done();
    }

    final static int BUFSIZ = 1000;

    byte             buf[]  = new byte[BUFSIZ];
    InputStream      in;
    OutputStream     out;
    OutputStream     os;

    boolean          done   = false;
    Listener         listener;

    public SocketRR2(InputStream in, OutputStream out, OutputStream os, Listener listener) {
        this.in = in;
        this.out = out;
        this.os = os;
        this.listener = listener;
    }

    public void run() {
        int n;
        try {
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                out.flush();
                if (os != null) {
                    os.write(buf, 0, n);
                    os.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(os));
        } finally {
            halt();
        }
    }

    public synchronized void halt() {
        if (!done) {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            done = true;
            if (listener != null) {
                listener.done();
                listener = null;
            }
        }
    }
}

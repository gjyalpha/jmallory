package org.jmallory.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LineInputStream extends InputStream {
    private static final int _CR   = 13;
    private static final int _LF   = 10;
    private int              _ch   = -1;    // currently read char  
    private InputStream      in;
    private boolean          lengthByHeader;

    public LineInputStream(InputStream i, boolean lengthByHeader) {
        this.in = new BufferedInputStream(i);
        this.lengthByHeader = lengthByHeader;
    }

    /**
     * Read a line of data from the underlying inputstream
     * 
     * @return a line stripped of line terminators
     */
    public String readLine() throws IOException {
        _ch = in.read();
        if (_ch == -1) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        while (_ch != -1) {
            if (_ch == _CR) {
                // we got a CR, check if the next one is LF
                in.mark(1);
                _ch = in.read();
                if (_ch == _LF) {
                    // we got a line, break
                    break;
                } else {
                    // we only get a CR, but on LF following, append CR to line
                    in.reset();
                }
            } else if (_ch == _LF) {
                // we got a line with only LF, break
                break;
            }
            sb.append((char) _ch);
            _ch = in.read();
        }
        return (new String(sb));
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close() throws IOException {
        in.close();
    }

    public boolean equals(Object obj) {
        return in.equals(obj);
    }

    public int hashCode() {
        return in.hashCode();
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public int read() throws IOException {
        return in.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    public void reset() throws IOException {
        in.reset();
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public String toString() {
        return in.toString();
    }

    public boolean isLengthByHeader() {
        return lengthByHeader;
    }

    public void setLengthByHeader(boolean lengthByHeader) {
        this.lengthByHeader = lengthByHeader;
    }
}

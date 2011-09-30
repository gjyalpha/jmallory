package org.jmallory.smtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;

import org.jmallory.io.LineInputStream;
import org.jmallory.util.Utils;

public class SmtpSender {

    private LineInputStream   emailInputStream;
    private InputStream       requestStream;
    private PipedOutputStream requestPipelineStream;
    private OutputStream      responseStream;

    public SmtpSender(String emailSmtpRequest) {
        try {
            emailInputStream = new LineInputStream(new ByteArrayInputStream(
                    emailSmtpRequest.getBytes(Utils.CHARSET)), false);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        requestPipelineStream = new PipedOutputStream();
        try {
            requestStream = new PipedInputStream(requestPipelineStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        responseStream = new SmtpSenderOutputStream();
    }

    public InputStream getInputStream() {
        return requestStream;
    }

    public OutputStream getOutputStream() {
        return responseStream;
    }

    protected void onSmtpResponseLine(String line) {
        try {
            if (line.startsWith("220 ") || line.startsWith("221 ") || line.startsWith("250 ")) {
                String emailLine = emailInputStream.readLine();
                if (emailLine != null) {
                    requestPipelineStream.write((emailLine + MockSmtpd.CRLF)
                            .getBytes(Utils.CHARSET));
                    if (MockSmtpd.RSET.equals(emailLine)) {
                        // we are sending a RSET command, close the stream
                        requestPipelineStream.close();
                    }
                }
            } else if (line.startsWith("354 ")) {
                // we entered the DATA area
                String emailLine = null;
                while ((emailLine = emailInputStream.readLine()) != null) {
                    requestPipelineStream.write((emailLine + MockSmtpd.CRLF)
                            .getBytes(Utils.CHARSET));
                    if (MockSmtpd.DATA_END.equals(emailLine)) {
                        // we reached the end of DATA, break to wait for 250 Data Ok
                        break;
                    }
                }
            } else {
                // we encountered error code, close the stream
                requestPipelineStream.write((MockSmtpd.RSET + MockSmtpd.CRLF)
                        .getBytes(Utils.CHARSET));
                requestPipelineStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class SmtpSenderOutputStream extends FilterOutputStream {

        private ByteArrayOutputStream baos;

        public SmtpSenderOutputStream() {
            this(new ByteArrayOutputStream());
        }

        public SmtpSenderOutputStream(ByteArrayOutputStream out) {
            super(out);
            this.baos = out;
        }

        @Override
        public void write(int b) throws IOException {
            if (b == '\r') {
                // do nothing
            } else if (b == '\n') {
                // we got a line
                onSmtpResponseLine(new String(baos.toByteArray(), Utils.CHARSET));
                baos.reset();
            } else {
                baos.write(b);
            }
        }
    }
}

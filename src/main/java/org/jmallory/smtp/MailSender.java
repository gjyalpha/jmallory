package org.jmallory.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.jmallory.io.LineInputStream;
import org.jmallory.util.Utils;


public class MailSender implements Runnable {

    public static interface MailSenderListener {
        void done(Exception e);
    }

    boolean            done;

    String             host;
    int                port;

    String             from;
    String             password;
    String             to;

    String             title;
    Date               date;

    Socket             socket;
    LineInputStream    in;
    OutputStream       out;

    MailSenderListener listener;

    public MailSender(String host, int port, String from, String password, String to, String title,
                      Date date, MailSenderListener listner) {
        if (host == null || port <= 0 || from == null || to == null || title == null
                || date == null) {
            throw new IllegalArgumentException("paramter is null");
        }

        this.host = host;
        this.port = port;
        this.from = from;
        this.password = password;
        this.to = to;
        this.title = title;
        this.date = date;
        this.listener = listner;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(2000);

            in = new LineInputStream(socket.getInputStream(), true);
            out = socket.getOutputStream();

            expect(220);

            sendLine("HELO jmallory");
            expect(250);

            if (password == null || !password.trim().isEmpty()) {
                sendLine("AUTH LOGIN");
                expect(334);

                sendLine(new String(Base64.encodeBase64(from.getBytes())));
                expect(334);

                sendLine(new String(Base64.encodeBase64(password.getBytes())));
                expect(235);
            }

            sendLine("MAIL FROM: <" + from + ">");
            expect(250);

            sendLine("RCPT TO: <" + to + ">");
            expect(250);

            sendLine("DATA");
            expect(354);

            // send data
            sendLine("Message-Id: 1234567890");
            sendDate(date);
            sendLine("X-Priority: 3");
            sendLine("From: =?UTF-8?B?" + new String(Base64.encodeBase64(from.getBytes())) + "?=<"
                    + from + ">");
            sendLine("To: " + to);
            sendLine("Subject: =?UTF-8?B?" + new String(Base64.encodeBase64(title.getBytes()))
                    + "?=");
            sendLine("MIME-Version: 1.0");
            sendLine("");
            sendLine(title);
            sendLine("");
            sendLine(".");
            expect(250);

            sendLine("RSET");
            expect(250);

            if (listener != null) {
                listener.done(null);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.done(e);
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected void sendLine(String line) throws Exception {
        if (line == null || out == null) {
            return;
        }
        out.write((line + MockSmtpd.CRLF).getBytes(Utils.CHARSET));
    }

    protected void sendDate(Date date) throws Exception {
        if (date == null || this.in == null) {
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        sendLine("Date: " + format.format(date));
    }

    protected void expect(Object returnCode) throws Exception {
        if (returnCode == null || in == null) {
            throw new Exception("returnCode or in is null");
        }

        String line = in.readLine();
        if (line == null || !line.startsWith(returnCode + " ")) {
            throw new Exception("Return code error, expected: " + returnCode + ", but actual: "
                    + line);
        }
    }
}

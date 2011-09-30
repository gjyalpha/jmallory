package org.jmallory.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JTextArea;

import org.jmallory.io.LineInputStream;


/**
 * This should be run in the same thread, instead of a separate thread.
 * 
 * @author gerry
 */
public class MockSmtpd implements Runnable {
    public static final String CRLF          = "\r\n";

    public static final String HELO          = "HELO";
    public static final String EHLO          = "EHLO";
    public static final String MAIL_FROM     = "MAIL FROM";
    public static final String RCPT_TO       = "RCPT TO";
    public static final String DATA          = "DATA";
    public static final String DATA_END      = ".";
    public static final String RSET          = "RSET";
    public static final String NOOP          = "NOOP";
    public static final String QUIT          = "QUIT";

    public static final String S220_HELO     = "220 jmallory mock smtp server\r\n";
    public static final String S250_OK       = "250 Ok\r\n";
    public static final String S250_MAIL_OK  = "250 Mail Ok\r\n";
    public static final String S250_RCPT_OK  = "250 Rcpt Ok\r\n";
    public static final String S250_NOOP_OK  = "250 Noop Ok\r\n";
    public static final String S354_DATA     = "354 End data with <CR><LF>.<CR><LF>\r\n";
    public static final String S250_DATA_OK  = "250 Data Ok: queued as freedom\r\n";
    public static final String S250_RESET_OK = "250 Reset Ok\r\n";
    public static final String S221_BYE      = "221 Bye\r\n";
    public static final String S500_BAD      = "500 Error: bad syntax\r\n";

    public static final String SUBJECT       = "Subject";

    enum State {
        INIT,
        HELO,
        MAIL,
        RCPT,
        DATA,
        RESET
    };

    private LineInputStream inputStream;
    private OutputStream    outputStream;

    private JTextArea       inputText;
    private JTextArea       outputText;

    private State           currentState = State.INIT;

    public MockSmtpd(InputStream in, OutputStream out, JTextArea inputText, JTextArea outputText) {
        this.inputStream = new LineInputStream(in, false);
        this.outputStream = out;
        this.inputText = inputText;
        this.outputText = outputText;
    }

    public void run() {

        if (!State.INIT.equals(currentState)) {
            if (outputText != null) {
                outputText.append("State is not INIT!");
            } else {
                throw new RuntimeException("State is not INIT!");
            }
        }

        try {
            if (outputStream != null) {
                writeOutput(S220_HELO);
            }
            currentState = State.HELO;

            String line = null;
            outter_loop: while ((line = inputStream.readLine()) != null) {
                if (inputText != null) {
                    inputText.append(line + "\r\n");
                }

                if (line.startsWith(NOOP)) {
                    writeOutput(S250_NOOP_OK);
                    continue;
                } else if (line.startsWith(QUIT)) {
                    writeOutput(S221_BYE);
                    break;
                }

                switch (currentState) {
                    case HELO:
                        if (line.startsWith(HELO) || line.startsWith(EHLO)) {
                            writeOutput(S250_OK);
                            currentState = State.MAIL;
                        } else {
                            writeOutput(S500_BAD);
                            break outter_loop;
                        }
                        break;
                    case MAIL:
                        if (line.startsWith(MAIL_FROM)) {
                            writeOutput(S250_MAIL_OK);
                            currentState = State.RCPT;
                        } else {
                            writeOutput(S500_BAD);
                            break outter_loop;
                        }
                        break;
                    case RCPT:
                        if (line.startsWith(RCPT_TO)) {
                            writeOutput(S250_RCPT_OK);
                        } else if (line.startsWith(DATA)) {
                            writeOutput(S354_DATA);
                            currentState = State.DATA;
                        } else {
                            writeOutput(S500_BAD);
                            break outter_loop;
                        }
                        break;
                    case DATA:
                        if (DATA_END.equals(line)) {
                            writeOutput(S250_DATA_OK);
                            currentState = State.RESET;
                        }
                        break;
                    case RESET:
                        if (line.startsWith(RSET)) {
                            writeOutput(S250_RESET_OK);
                            currentState = State.HELO;
                        } else {
                            writeOutput(S500_BAD);
                        }
                        // we force the connection to be closed by break the outter loop, the caller of this
                        // run method should close the socket.
                        break outter_loop;
                    default:
                        if (outputText != null) {
                            outputText.append("Unexpected state: " + currentState + "\r\n");
                        }
                        break;
                }
            }

        } catch (IOException e) {
            StringWriter st = new StringWriter();
            PrintWriter wr = new PrintWriter(st);
            e.printStackTrace(wr);
            wr.close();
            if (outputText != null) {
                outputText.append(st.toString());
            } else {
                // something went wrong before we had the output area
                System.out.println(st.toString());
            }
        }
    }

    private void writeOutput(String response) throws IOException {
        if (outputText != null) {
            outputText.append(response);
        }
        if (outputStream != null) {
            outputStream.write(response.getBytes());
        }
    }
}

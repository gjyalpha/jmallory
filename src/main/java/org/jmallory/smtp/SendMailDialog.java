package org.jmallory.smtp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jmallory.smtp.MailSender.MailSenderListener;
import org.jmallory.swing.HostnameField;
import org.jmallory.swing.NumberField;

public class SendMailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private SendMail          sendMail         = null;

    public SendMailDialog(Window parent) {
        this(parent, "127.0.0.1", 2500);
    }

    public SendMailDialog(Window parent, String smtpHost, int smtpPort) {
        super(parent, "Send Mail", ModalityType.APPLICATION_MODAL);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                stop();
            }
        });

        JPanel mainPane = new JPanel(new GridBagLayout());
        mainPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        c.gridwidth = 1;
        mainPane.add(new RLabel("Mails:"), c);
        final NumberField mailsText = new NumberField(4);
        mailsText.setText("1");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(mailsText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("Threads:"), c);
        final NumberField threadsText = new NumberField(4);
        threadsText.setText("1");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(threadsText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("Smtp Host:"), c);
        final HostnameField smtpText = new HostnameField((20));
        smtpText.setText(smtpHost);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(smtpText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("Smtp Port:"), c);
        final NumberField portText = new NumberField(4);
        portText.setText(String.valueOf(smtpPort));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(portText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("From:"), c);
        final JTextField fromText = new JTextField((20));
        fromText.setText("test@test.com");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(fromText, c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("Pass:"), c);
        final JTextField passwordText = new JTextField((10));
        passwordText.setText("password");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(passwordText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("To:"), c);
        final JTextField toText = new JTextField((20));
        toText.setText("test@test.com");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(toText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("Title"), c);
        final JTextField titleText = new JTextField((20));
        titleText.setText("Hi there");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(titleText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("<html>Date Interval &nbsp;&nbsp;<br/> In Minutes:</html>"), c);
        final NumberField intervalText = new NumberField(4);
        intervalText.setText("120");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(intervalText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        c.gridwidth = 1;
        mainPane.add(new RLabel("<html>Pause Interval &nbsp;&nbsp;<br/> In Millisecdons:</html>"),
                c);
        final NumberField pauseText = new NumberField(4);
        pauseText.setText("0");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(pauseText, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        final JButton sendButton = new JButton("Send");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        mainPane.add(sendButton, c);

        final JButton cancelButton = new JButton("Cancel");
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(cancelButton, c);

        mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

        final JProgressBar progress = new JProgressBar();
        progress.setPreferredSize(new Dimension(200, 20));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainPane.add(progress, c);

        c.gridwidth = 1;
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPane, BorderLayout.CENTER);

        sendButton.addActionListener(new ActionListener() {
            private int parseInteger(String text) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder error = new StringBuilder();

                final int mails = parseInteger(mailsText.getText());
                final int threads = parseInteger(threadsText.getText());
                final String host = smtpText.getText();
                final int port = parseInteger(portText.getText());
                final String from = fromText.getText();
                final String password = passwordText.getText();
                final String to = toText.getText();
                final String title = titleText.getText();
                final int interval = parseInteger(intervalText.getText());
                final int pause = parseInteger(pauseText.getText());

                if (mails <= 0 || mails > 1000) {
                    error.append("Invalid mails, should be 1 - 1000\n");
                }
                if (threads <= 0 || threads > 50) {
                    error.append("Invalid threads, should be 1 - 50\n");
                }
                if (host.trim().length() == 0) {
                    error.append("Empty Smtp Host\n");
                }
                if (port <= 0) {
                    error.append("Invalid Smtp Port");
                }
                if (from.trim().length() == 0) {
                    error.append("Invalid from");
                }
                if (to.trim().length() == 0) {
                    error.append("Invalid to");
                }
                if (title.trim().length() == 0) {
                    error.append("Invalid title");
                }
                if (interval < 0 || interval > 100000) {
                    error.append("Invalid Date Interval In Minutes, should be 0 - 100000\n");
                }
                if (pause < 0 || pause > 60000) {
                    error.append("Invalid Pause Interval In Minutes, should be 0 - 60000\n");
                }

                if (error.length() != 0) {
                    // we have som error
                    JOptionPane.showMessageDialog(SendMailDialog.this, error.toString());
                    return;
                }

                sendButton.setEnabled(false);

                // everything is OK, let start rock roll

                progress.setStringPainted(true);
                progress.setMinimum(0);
                progress.setMaximum(mails);
                progress.setString("0/0");
                progress.setValue(0);

                sendMail = new SendMail(mails, threads, host, port, from, password, to, title,
                        new Date(), interval, pause, new MailSenderListener() {
                            protected int succeed;
                            protected int done;

                            @Override
                            public void done(final Exception e) {
                                if (e != null) {
                                    e.printStackTrace();
                                    stop();
                                    try {
                                        SwingUtilities.invokeAndWait(new Runnable() {
                                            @Override
                                            public void run() {
                                                JOptionPane.showMessageDialog(SendMailDialog.this,
                                                        "Failed to connect to " + host + ":" + port
                                                                + ", due to " + e.getMessage(),
                                                        "Sending Emails Failed",
                                                        JOptionPane.ERROR_MESSAGE);
                                            }
                                        });
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    sendButton.setEnabled(true);
                                } else {
                                    succeed++;
                                    done++;
                                }
                                if (done >= mails) {
                                    sendMail.stop(); // done
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setString(succeed + "/" + done);
                                            progress.setValue(mails);
                                            sendButton.setEnabled(true);
                                        }
                                    });
                                } else {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setString(succeed + "/" + done);
                                            progress.setValue(done);
                                        }
                                    });
                                }

                            }
                        });
                sendMail.send(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sendButton.isEnabled()) {
                    SendMailDialog.this.setVisible(false);
                } else {
                    stop();
                    sendButton.setEnabled(true);
                }
            }
        });

        this.pack();
        this.setSize(550, 500);

        Rectangle rec = null;
        if (parent != null) {
            rec = parent.getBounds();
        } else {
            rec = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        }

        int x = rec.x + rec.width / 2 - this.getWidth() / 2 + 25;
        int y = rec.y + rec.height / 2 - this.getHeight() / 2 + 25;
        this.setBounds(x, y, this.getWidth(), this.getHeight());

    }

    protected synchronized void stop() {
        if (sendMail != null) {
            sendMail.stop();
            sendMail = null;
        }
    }

    private static class RLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        public RLabel(String text) {
            super(text);
            this.setHorizontalAlignment(JLabel.RIGHT);
        }
    }

    public static void main(String[] args) {
        SendMailDialog dialog = new SendMailDialog(null) {
            private static final long serialVersionUID = 1L;

            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (!visible) {
                    System.exit(0);
                }
            }
        };

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
        dialog.setVisible(true);
    }
}

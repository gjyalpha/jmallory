package org.jmallory.smtp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.jmallory.Main;
import org.jmallory.http.ProfileManager;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.io.ConnectionAcceptor;
import org.jmallory.model.ConnectionListener;
import org.jmallory.model.ConnectionListener.ConnectionTableModel;
import org.jmallory.swing.ClosableTabComponent;
import org.jmallory.swing.EditingCallback;
import org.jmallory.swing.EncoderDecoderTextEditor;

public class SmtpListener extends JPanel implements ConnectionListener {

    private static final long       serialVersionUID = 1L;

    /**
     * Field inputSocket
     */
    public Socket                   inputSocket      = null;

    /**
     * Field outputSocket
     */
    public Socket                   outputSocket     = null;

    public JLabel                   typeLabel        = null;

    /**
     * Field portField
     */
    public JTextField               portField        = null;

    /**
     * Field hostField
     */
    public JTextField               hostField        = null;

    /**
     * Field tPortField
     */
    public JTextField               tPortField       = null;

    public JCheckBox                mockSmtpCheckBox = null;

    /**
     * Field stopButton
     */
    public JButton                  stopButton       = null;

    /**
     * Field removeButton
     */
    public JButton                  removeButton     = null;

    /**
     * Field removeAllButton
     */
    public JButton                  removeAllButton  = null;

    public JButton                  saveButton       = null;
    public JButton                  resendButton     = null;
    public JButton                  sendMailButton   = null;

    /**
     * Field switchButton
     */
    public JButton                  switchButton     = null;

    /**
     * Field closeButton
     */
    public JButton                  closeButton      = null;

    /**
     * Field connectionTable
     */
    public JTable                   connectionTable  = null;

    /**
     * Field tableModel
     */
    public SmtpConnectionTableModel tableModel       = null;

    /**
     * Field outPane
     */
    public JSplitPane               outPane          = null;

    /**
     * Field sSocket
     */
    public ServerSocket             sSocket          = null;

    /**
     * Field sw
     */
    public ConnectionAcceptor       sw               = null;

    /**
     * Field leftPanel
     */
    public JPanel                   leftPanel        = null;

    /**
     * Field rightPanel
     */
    public JPanel                   rightPanel       = null;

    public ClosableTabComponent     closeTab         = null;
    /**
     * Field notebook
     */
    public JTabbedPane              notebook         = null;

    /**
     * Field delayBytes
     */
    public int                      delayBytes       = 0;

    /**
     * Field delayTime
     */
    public int                      delayTime        = 0;

    /**
     * Field slowLink
     */
    public SlowLinkSimulator        slowLink;

    private String                  listenerName;

    /**
     * create a listener
     * 
     * @param _notebook
     * @param name
     * @param listenPort
     * @param host
     * @param targetPort
     * @param isProxy
     * @param slowLink optional reference to a slow connection
     */
    public SmtpListener(JTabbedPane _notebook, String name, int listenPort, String host,
                        int targetPort, SlowLinkSimulator slowLink, boolean mockSmtp) {
        notebook = _notebook;

        if (name == null) {
            name = "SMTP Port " + listenPort;
        }

        this.listenerName = name;

        // set the slow link to the passed down link
        if (slowLink != null) {
            this.slowLink = slowLink;
        } else {
            // or make up a no-op one.
            this.slowLink = new SlowLinkSimulator(0, 0);
        }

        this.setLayout(new BorderLayout());

        // 1st component is just a row of labels and 1-line entry fields
        // ///////////////////////////////////////////////////////////////////
        JPanel topControls = new JPanel();
        topControls.setLayout(new BoxLayout(topControls, BoxLayout.X_AXIS));
        topControls.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        final String start = "Start";
        topControls.add(stopButton = new JButton(start));
        topControls.add(Box.createRigidArea(new Dimension(5, 0)));
        topControls.add(new JLabel("  " + "Listen Port:" + " ", SwingConstants.RIGHT));
        topControls.add(portField = new JTextField("" + listenPort, 4));
        topControls.add(new JLabel("  " + "Host:", SwingConstants.RIGHT));
        topControls.add(hostField = new JTextField(host, 30));
        topControls.add(new JLabel("  " + "Port:" + " ", SwingConstants.RIGHT));
        topControls.add(tPortField = new JTextField("" + targetPort, 4));
        topControls.add(Box.createRigidArea(new Dimension(5, 0)));
        portField.setEditable(false);
        portField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));
        hostField.setEditable(false);
        hostField.setMaximumSize(new Dimension(85, Short.MAX_VALUE));
        tPortField.setEditable(false);
        tPortField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));
        mockSmtpCheckBox = new JCheckBox("Mock SMTP");
        mockSmtpCheckBox.setSelected(mockSmtp);
        mockSmtpCheckBox.setEnabled(false);
        topControls.add(mockSmtpCheckBox);

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if ("Stop".equals(event.getActionCommand())) {
                    stop();
                }
                if (start.equals(event.getActionCommand())) {
                    start();
                }
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        typeLabel = new JLabel("<html>This is a <b>" + (mockSmtp ? "MOCK" : "TUNNEL")
                + "</b> SMTP listener.</html>");
        top.add(typeLabel, BorderLayout.NORTH);
        top.add(topControls, BorderLayout.CENTER);

        this.add(top, BorderLayout.NORTH);

        // 2nd component is a split pane with a table on the top
        // and the request/response text areas on the bottom
        // ///////////////////////////////////////////////////////////////////
        tableModel = new SmtpConnectionTableModel();
        connectionTable = new JTable();
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        connectionTable.getColumnModel().getColumn(0)
                .setCellRenderer(new SmtpConnectionCellRenderer());

        // Reduce the STATE column and increase the REQ column
        TableColumn col;
        col = connectionTable.getColumnModel().getColumn(Main.STATE_COLUMN);
        col.setPreferredWidth(col.getPreferredWidth() / 2);
        col = connectionTable.getColumnModel().getColumn(Main.REQ_COLUMN);
        col.setPreferredWidth(col.getPreferredWidth() * 2);
        ListSelectionModel sel = connectionTable.getSelectionModel();
        sel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    return;
                }
                ListSelectionModel m = (ListSelectionModel) event.getSource();
                int divLoc = outPane.getDividerLocation();
                if (m.isSelectionEmpty()) {
                    setLeft(new JLabel(" " + "Waiting for Connection..."));
                    setRight(new JLabel(""));
                    removeButton.setEnabled(false);
                    removeAllButton.setEnabled(false);
                    saveButton.setEnabled(false);
                } else {
                    int row = m.getLeadSelectionIndex();
                    if (row == 0) {
                        if (tableModel.size() == 0) {
                            setLeft(new JLabel(" Waiting for connection..."));
                            setRight(new JLabel(""));
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(false);
                            saveButton.setEnabled(false);
                        } else {
                            SmtpConnection conn = tableModel.lastConnection();

                            setLeft(conn.inputScroll);
                            setRight(conn.outputScroll);
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(true);
                            saveButton.setEnabled(true);
                        }
                    } else {
                        SmtpConnection conn = tableModel.getConnectionByRow(row);

                        setLeft(conn.inputScroll);
                        setRight(conn.outputScroll);
                        removeButton.setEnabled(true);
                        removeAllButton.setEnabled(true);
                        saveButton.setEnabled(true);
                    }
                }
                outPane.setDividerLocation(divLoc);
            }
        });
        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BorderLayout());
        JScrollPane tableScrollPane = new JScrollPane();
        tableScrollPane.setViewportView(connectionTable);
        tablePane.add(tableScrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        final String removeSelected = "Remove Selected";
        buttons.add(removeButton = new JButton(removeSelected));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String removeAll = "Remove All";
        buttons.add(removeAllButton = new JButton(removeAll));
        tablePane.add(buttons, BorderLayout.SOUTH);
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (removeSelected.equals(event.getActionCommand())) {
                    remove();
                }
            }
        });
        removeAllButton.setEnabled(false);
        removeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (removeAll.equals(event.getActionCommand())) {
                    removeAll();
                }
            }
        });

        // Add Response Section
        // ///////////////////////////////////////////////////////////////////
        JPanel pane2 = new JPanel();
        pane2.setLayout(new BorderLayout());
        leftPanel = new JPanel();
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(new JLabel("  " + "Request"));
        leftPanel.add(new JLabel(" " + "Waiting for connection"));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(new JLabel("  " + "Response"));
        rightPanel.add(new JLabel(""));
        outPane = new JSplitPane(0, leftPanel, rightPanel);
        outPane.setDividerSize(4);
        outPane.setResizeWeight(0.5);
        pane2.add(outPane, BorderLayout.CENTER);
        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String save = "Save";
        bottomButtons.add(saveButton = new JButton(save));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String resend = "Send new / Resend";
        bottomButtons.add(resendButton = new JButton(resend));

        bottomButtons.add(sendMailButton = new JButton("Send Mail"));

        bottomButtons.add(Box.createHorizontalGlue());
        final String switchStr = "Switch Layout";
        bottomButtons.add(switchButton = new JButton(switchStr));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String close = "Close";
        bottomButtons.add(closeButton = new JButton(close));
        pane2.add(bottomButtons, BorderLayout.SOUTH);

        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        resendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                resend();
            }
        });
        sendMailButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int tport = -1;
                try {
                    tport = Integer.valueOf(SmtpListener.this.portField.getText());
                } catch (Exception e2) {
                }
                SendMailDialog sendMailDialog = new SendMailDialog(Main.getFrame(), "127.0.0.1",
                        tport != -1 ? tport : 2500);
                sendMailDialog.setVisible(true);
            }
        });
        switchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (switchStr.equals(event.getActionCommand())) {
                    int v = outPane.getOrientation();
                    if (v == 0) {

                        // top/bottom
                        outPane.setOrientation(1);
                    } else {

                        // left/right
                        outPane.setOrientation(0);
                    }
                    outPane.setDividerLocation(0.5);
                }
            }
        });
        ActionListener closeActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                close();
            }
        };
        closeButton.addActionListener(closeActionListener);
        JSplitPane pane1 = new JSplitPane(0);
        pane1.setDividerSize(4);
        pane1.setTopComponent(tablePane);
        pane1.setBottomComponent(pane2);
        pane1.setDividerLocation(150);
        this.add(pane1, BorderLayout.CENTER);

        // 
        // //////////////////////////////////////////////////////////////////
        sel.setSelectionInterval(0, 0);
        outPane.setResizeWeight(0.5);
        notebook.addTab(name, null, this, listenerName);
        closeTab = new ClosableTabComponent(notebook, name, closeActionListener);
        notebook.setTabComponentAt(notebook.getTabCount() - 1, closeTab);
        start();
    }

    public void setLeft(Component left) {
        leftPanel.removeAll();
        if (left != null) {
            leftPanel.add(left);
        }
    }

    public void setRight(Component right) {
        rightPanel.removeAll();
        if (right != null) {
            rightPanel.add(right);
        }
    }

    /**
     * Method start
     */
    public void start() {
        int port = Integer.parseInt(portField.getText());
        typeLabel.setText("<html>This is a <b>"
                + (mockSmtpCheckBox.isSelected() ? "MOCK" : "TUNNEL")
                + "</b> SMTP listener.</html>");
        portField.setText("" + port);
        int i = notebook.indexOfComponent(this);
        notebook.setTitleAt(i, "SMTP Port " + port);
        notebook.setToolTipTextAt(i, listenerName);
        int tmp = Integer.parseInt(tPortField.getText());
        tPortField.setText("" + tmp);
        sw = new ConnectionAcceptor(this, port);
        stopButton.setText("Stop");
        portField.setEditable(false);
        hostField.setEditable(false);
        tPortField.setEditable(false);
        mockSmtpCheckBox.setEnabled(false);
        closeTab.setActive(true);
    }

    public void close() {
        stop();
        notebook.remove(this);
    }

    public void stop() {
        try {
            for (int i = 0; i < tableModel.size(); i++) {
                tableModel.getConnection(i).halt();
            }
            sw.halt();
            stopButton.setText("Start");
            portField.setEditable(true);
            hostField.setEditable(true);
            tPortField.setEditable(true);
            mockSmtpCheckBox.setEnabled(true);
            closeTab.setActive(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method remove
     */
    public void remove() {
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        int bot = lsm.getMinSelectionIndex();
        int[] rows = connectionTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            if (rows[i] != 0) {
                tableModel.removeConnectionByRow(rows[i]);
            }
        }
        if (bot > tableModel.size()) {
            bot = tableModel.size();
        }
        lsm.setSelectionInterval(bot, bot);
    }

    /**
     * Method removeAll
     */
    public void removeAll() {
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        lsm.clearSelection();
        tableModel.removeAllConnection();
        lsm.setSelectionInterval(0, 0);
    }

    /**
     * Method save
     */
    public void save() {
        JFileChooser dialog = new JFileChooser(".");
        int rc = dialog.showSaveDialog(this);
        if (rc == JFileChooser.APPROVE_OPTION) {
            try {
                File file = dialog.getSelectedFile();
                FileOutputStream out = new FileOutputStream(file);

                int[] rows = connectionTable.getSelectedRows();
                for (int i = 0; i < rows.length; i++) {
                    int row = rows[i];
                    if (row > 0) {
                        SmtpConnection conn = tableModel.getConnectionByRow(row);
                        out.write(conn.inputText.getText().getBytes());
                        out.write(conn.outputText.getText().getBytes());
                    }
                }

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Vector createProfileListData() {
        Vector profileVector = new Vector();
        profileVector.add("<New Profile>");
        profileVector.addAll(ProfileManager.getProfileList());
        return profileVector;
    }

    /**
     * Method resend
     */
    public void resend() {
        try {
            int row = connectionTable.getSelectedRow();
            String inputString = null;
            if (row > 0) {
                SmtpConnection conn = tableModel.getConnectionByRow(row);
                inputString = conn.inputText.getText();
            }

            new EncoderDecoderTextEditor(Main.getFrame(), null, inputString,
                    EncoderDecoderTextEditor.CodecMode.TRIVAL, false, 600, 600,
                    new EditingCallback() {
                        @Override
                        public void done(String text) {
                            doResend(text);
                        }

                        @Override
                        public void cancel() {
                        }
                    }).setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doResend(String emailMessage) {
        SmtpSender sender = new SmtpSender(emailMessage);
        new SmtpConnection(this, sender.getInputStream(), sender.getOutputStream());
    }

    class SmtpConnectionCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component result = super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            if (column == 0 && row > 0 && tableModel.getConnectionByRow(row).smtpd != null) {
                if (isSelected) {
                    setForeground(Color.WHITE);
                    setBackground(Color.BLUE);
                } else {
                    setForeground(Color.BLACK);
                    setBackground(Color.GREEN);
                }
            } else {
                if (isSelected) {
                    setForeground(Color.WHITE);
                    setBackground(Color.BLACK);
                } else {
                    setForeground(Color.BLACK);
                    setBackground(Color.WHITE);
                }
            }
            return result;
        }

    }

    public void newConnection(Socket inSocket) {
        new SmtpConnection(this, inSocket);
    }

    public static class SmtpConnectionTableModel extends ConnectionTableModel<SmtpConnection> {

        private static final long     serialVersionUID = 1L;

        private static final String[] COLUMN_NAME      = { "State", "Time", "Request Host", "From",
                                                               "Title", "Elapsed Time" };
        private static final String[] RECENT_ROW       = { "---", "Most Recent", "---", "---",
                                                               "---", "---" };

        public SmtpConnectionTableModel() {
            super(COLUMN_NAME, RECENT_ROW);
        }

        @Override
        protected Object doGetValueAt(int rowIndex, int columnIndex) {
            SmtpConnection conn = data.get(rowIndex - 1);
            switch (columnIndex) {
                case 0:
                    return conn.state;
                case 1:
                    return conn.time;
                case 2:
                    return conn.fromHost;
                case 3:
                    return conn.emailFrom;
                case 4:
                    return conn.emailTitle;
                case 5:
                    return conn.elapsedTime;
                default:
                    return null;
            }
        }
    }
}

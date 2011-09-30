package org.jmallory.http;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.jmallory.Main;
import org.jmallory.http.ReqDataEditor.Action;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.io.ConnectionAcceptor;
import org.jmallory.model.ConnectionListener;
import org.jmallory.swing.ClosableTabComponent;
import org.jmallory.util.Utils;

public class HttpListener extends JPanel implements ConnectionListener {

    private static final long       serialVersionUID      = 1L;

    public Socket                   inputSocket           = null;
    public Socket                   outputSocket          = null;
    public ServerSocket             sSocket               = null;
    public ConnectionAcceptor             sw                    = null;

    public JTextField               portField             = null;
    public JTextField               hostField             = null;
    public JTextField               tPortField            = null;

    public JCheckBox                useProfilesCheckBox   = null;
    public JCheckBox                httpsTarget           = null;

    public JCheckBox                cloneHostHeader       = null;
    public JCheckBox                useProxy              = null;
    public JButton                  stopButton            = null;

    public JButton                  removeButton          = null;

    public JButton                  removeAllButton       = null;
    public JCheckBox                jsonFormatBox         = null;
    public JButton                  saveButton            = null;
    public JButton                  saveToClipboardButton = null;
    public JButton                  addToProfileButton    = null;
    public JButton                  resendButton          = null;
    public JButton                  switchButton          = null;
    public JButton                  closeButton           = null;

    public JTable                   connectionTable       = null;
    public HttpConnectionTableModel tableModel            = null;

    public JSplitPane               outPane               = null;

    public JPanel                   leftPanel             = null;
    public JPanel                   rightPanel            = null;

    public ClosableTabComponent     closeTab              = null;

    public JTabbedPane              notebook              = null;

    public String                   HTTPProxyHost         = null;
    public int                      HTTPProxyPort         = 80;
    public int                      delayBytes            = 0;

    public int                      delayTime             = 0;

    public SlowLinkSimulator        slowLink;

    public JComboBox                profileComboBox;

    public String                   profileNames;

    public Profile[]                mockDataProfiles;            // methodAndUrl+requestBody -> response

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
    public HttpListener(JTabbedPane _notebook, String name, int listenPort, String host,
                        int targetPort, String proxyHost, int proxyPort,
                        SlowLinkSimulator slowLink, Profile[] profiles, boolean httpsTargetProtool) {
        notebook = _notebook;
        this.mockDataProfiles = profiles;
        if (profiles != null && profiles.length > 0) {

            StringBuilder sb = new StringBuilder();
            sb.append(" [").append(profiles[0]);
            for (int i = 1; i < profiles.length; i++) {
                sb.append(",").append(profiles[i]);
            }
            sb.append("]");
            profileNames = sb.toString();
        } else {
            profileNames = "";
        }

        if (name == null) {
            name = "HTTP Port " + listenPort;
        }

        // proxy
        HTTPProxyHost = proxyHost;
        HTTPProxyPort = proxyPort;

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
        portField.setMinimumSize(new Dimension(50, 10));
        topControls.add(new JLabel("  " + "Host:", SwingConstants.RIGHT));
        topControls.add(hostField = new JTextField(host, 30));
        hostField.setMinimumSize(new Dimension(110, 10));
        topControls.add(new JLabel("  " + "Port:" + " ", SwingConstants.RIGHT));
        topControls.add(tPortField = new JTextField("" + targetPort, 4));
        tPortField.setMinimumSize(new Dimension(50, 10));

        useProfilesCheckBox = new JCheckBox("Use Profiles");
        useProfilesCheckBox.setSelected(profiles != null && profiles.length != 0);
        useProfilesCheckBox.setEnabled(false);
        topControls.add(useProfilesCheckBox);

        httpsTarget = new JCheckBox("HTTPS-Target");
        httpsTarget.setSelected(httpsTargetProtool);
        httpsTarget.setEnabled(false);
        topControls.add(httpsTarget);

        cloneHostHeader = new JCheckBox("Clone Host Header");
        topControls.add(cloneHostHeader);

        if (HTTPProxyHost != null) {
            useProxy = new JCheckBox("Use Proxy: " + HTTPProxyHost + ":" + proxyPort);
            useProxy.setSelected(HTTPProxyHost != null);
            topControls.add(useProxy);
        }

        portField.setEditable(false);
        portField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));
        hostField.setEditable(false);
        hostField.setMaximumSize(new Dimension(85, Short.MAX_VALUE));
        tPortField.setEditable(false);
        tPortField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));
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
        top.add(new JLabel("<html><b>Profiles: " + profileNames + "</b></html>"),
                BorderLayout.NORTH);
        top.add(topControls, BorderLayout.CENTER);

        this.add(top, BorderLayout.NORTH);

        // 2nd component is a split pane with a table on the top
        // and the request/response text areas on the bottom
        // ///////////////////////////////////////////////////////////////////
        tableModel = new HttpConnectionTableModel();
        connectionTable = new JTable();
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        connectionTable.getColumnModel().getColumn(0).setCellRenderer(new ConnectionCellRenderer());

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
                    setLeft(new JLabel(" Waiting for Connection..."));
                    setRight(null);
                    removeButton.setEnabled(false);
                    removeAllButton.setEnabled(false);
                    saveButton.setEnabled(false);
                    saveToClipboardButton.setEnabled(false);
                    addToProfileButton.setEnabled(false);
                    profileComboBox.setEnabled(false);
                } else {
                    int row = connectionTable.getSelectedRow();
                    if (row == 0) {
                        if (tableModel.size() == 0) {
                            setLeft(new JLabel(" Waiting for connection..."));
                            setRight(null);
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(false);
                            saveButton.setEnabled(false);
                            saveToClipboardButton.setEnabled(false);
                            profileComboBox.setEnabled(false);
                            addToProfileButton.setEnabled(false);
                        } else {
                            HttpConnection conn = tableModel.lastConnection();

                            formatJsonIfNeeded(conn);
                            setLeft(conn.inputScroll);
                            setRight(conn.outputScroll);
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(true);
                            saveButton.setEnabled(true);
                            saveToClipboardButton.setEnabled(true);
                            profileComboBox.setEnabled(true);
                            addToProfileButton.setEnabled(true);
                        }
                    } else {
                        HttpConnection conn = tableModel.getConnectionByRow(row);

                        formatJsonIfNeeded(conn);
                        setLeft(conn.inputScroll);
                        setRight(conn.outputScroll);
                        removeButton.setEnabled(true);
                        removeAllButton.setEnabled(true);
                        saveButton.setEnabled(true);
                        saveToClipboardButton.setEnabled(true);
                        profileComboBox.setEnabled(true);
                        addToProfileButton.setEnabled(true);
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
        outPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        outPane.setDividerSize(4);
        outPane.setResizeWeight(0.5);
        pane2.add(outPane, BorderLayout.CENTER);
        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomButtons.add(jsonFormatBox = new JCheckBox("JSON Format"));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String save = "Save";
        bottomButtons.add(saveButton = new JButton(save));
        bottomButtons.add(saveToClipboardButton = new JButton("Save to clipboard"));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String addToProfile = "Add to Profile";
        bottomButtons.add(addToProfileButton = new JButton(addToProfile));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        bottomButtons.add(profileComboBox = new JComboBox(createProfileListData()));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String resend = "Resend";
        bottomButtons.add(resendButton = new JButton(resend));
        bottomButtons.add(Box.createHorizontalGlue());
        final String switchStr = "Switch Layout";
        bottomButtons.add(switchButton = new JButton(switchStr));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String close = "Close";
        bottomButtons.add(closeButton = new JButton(close));
        pane2.add(bottomButtons, BorderLayout.SOUTH);

        jsonFormatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ListSelectionModel lsm = connectionTable.getSelectionModel();
                int row = lsm.getLeadSelectionIndex();
                if (row != -1 && tableModel.size() != 0) {
                    HttpConnection conn = null;
                    if (row == 0) {
                        // recent
                        conn = tableModel.lastConnection();
                    } else {
                        conn = tableModel.getConnectionByRow(row);
                    }
                    formatJsonIfNeeded(conn);
                }
            }
        });

        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        saveToClipboardButton.setEnabled(false);
        saveToClipboardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                saveToClipboard();
            }
        });
        addToProfileButton.setEnabled(false);
        addToProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addToProfile();
            }
        });
        resendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                resend();
            }
        });
        switchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int v = outPane.getOrientation();
                if (v == JSplitPane.VERTICAL_SPLIT) {
                    // top/bottom
                    outPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                } else {
                    // left/right
                    outPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                }
                outPane.setDividerLocation(0.5);
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
        pane1.setResizeWeight(0.5);
        this.add(pane1, BorderLayout.CENTER);

        // 
        // //////////////////////////////////////////////////////////////////
        sel.setSelectionInterval(0, 0);
        notebook.addTab(name, null, this, profileNames);

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

    public void start() {
        int port = Integer.parseInt(portField.getText());
        portField.setText("" + port);
        int i = notebook.indexOfComponent(this);
        notebook.setTitleAt(i, "HTTP Port " + port);
        notebook.setToolTipTextAt(i, profileNames);
        int tmp = Integer.parseInt(tPortField.getText());
        tPortField.setText("" + tmp);
        sw = new ConnectionAcceptor(this, port);
        stopButton.setText("Stop");
        portField.setEditable(false);
        hostField.setEditable(false);
        tPortField.setEditable(false);
        useProfilesCheckBox.setEnabled(false);
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
            useProfilesCheckBox.setEnabled(true);
            httpsTarget.setEnabled(true);
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

    public void save() {
        JFileChooser dialog = new JFileChooser(".");
        int rc = dialog.showSaveDialog(this);
        if (rc == JFileChooser.APPROVE_OPTION) {
            try {
                File file = dialog.getSelectedFile();
                Utils.chmod777(file);

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                        file)));
                saveContent(writer);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveToClipboard() {
        if (connectionTable.getSelectedRow() <= 0) {
            JOptionPane.showMessageDialog(this, "No thing selected!", "Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringWriter string = new StringWriter();

        PrintWriter writer = new PrintWriter(string);

        saveContent(writer);
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(
                        new StringSelection(string.toString().replaceAll(Utils.CRLF, Utils.LF)),
                        new ClipboardOwner() {
                            @Override
                            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                                // do nothing
                            }
                        });

        writer.close();

        JOptionPane.showMessageDialog(this, "Saved to clipboard!", "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    protected void saveContent(PrintWriter writer) {
        int[] rows = connectionTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            if (row > 0) {
                HttpConnection conn = tableModel.getConnectionByRow(row);
                try {
                    writer.println("/==================== Request =====================\\");
                    writer.println(conn.request != null ? conn.request
                            .toUrlDecodedString(jsonFormatBox.isSelected()) : "");
                    writer.println("===================== Response =====================");
                    writer.println(conn.response != null ? conn.response
                            .toUrlDecodedString(jsonFormatBox.isSelected()) : "");
                    writer.println("\\==================================================/");
                    writer.println();
                    writer.println();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void addToProfile() {
        Object selected = profileComboBox.getSelectedItem();
        Profile selectedProfile = null;
        if (selected instanceof Profile) {
            selectedProfile = (Profile) selected;
        } else {
            String newProfileName = JOptionPane.showInputDialog(this,
                    "Please input the new profile name:");
            if (newProfileName != null) {
                selectedProfile = ProfileManager.findExistProfile(newProfileName);
                if (selectedProfile == null) {
                    selectedProfile = ProfileManager.getProfile(newProfileName);
                    ((AbstractTableModel) Main.getFrame().getAdminPane().profileTable.getModel())
                            .fireTableDataChanged();
                    profileComboBox.addItem(selectedProfile);
                }
                profileComboBox.setSelectedItem(selectedProfile);
            } else {
                // null filename, return
                return;
            }
        }

        int[] rows = connectionTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            if (row > 0) {
                HttpConnection conn = tableModel.getConnectionByRow(row);
                selectedProfile.addData(new ReqRespData(conn.request, conn.response));
            }
        }

        for (int i = 0, tabCount = notebook.getTabCount(); i < tabCount; i++) {
            Component component = notebook.getComponentAt(i);
            if (component instanceof ProfileEditor) {
                ProfileEditor profileEditor = (ProfileEditor) component;
                if (selectedProfile.equals(profileEditor.getProfile())) {
                    ((AbstractTableModel) profileEditor.profileDataTable.getModel())
                            .fireTableDataChanged();
                    return;
                }
            }
        }

        // editor is not opened, open it
        new ProfileEditor(notebook, selectedProfile);
    }

    @SuppressWarnings("unchecked")
    public Vector createProfileListData() {
        Vector profileVector = new Vector();
        profileVector.add("<New Profile>");
        profileVector.addAll(ProfileManager.getProfileList());
        return profileVector;
    }

    public HttpResponse getMockResponse(HttpRequest request) {
        if (mockDataProfiles != null) {
            for (Profile profile : mockDataProfiles) {
                HttpResponse result = profile.getDataMap().get(request);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Method resend
     */
    public void resend() {
        int row = connectionTable.getSelectedRow();
        HttpRequest request = null;
        if (row > 0) {
            HttpConnection conn = tableModel.getConnectionByRow(row);
            if (conn != null) {
                request = conn.request;
            }
        }
        new ReqDataEditor(Main.getFrame(), this, ReqDataEditor.Action.RESEND,
                "Edit the request before resend", request).setVisible(true);
    }

    public void doResend(HttpRequest httpRequest) {
        InputStream in = new ByteArrayInputStream(httpRequest.toByteArray());
        new HttpConnection(this, in);
    }

    protected void formatJsonIfNeeded(HttpConnection conn) {
        if (conn.request != null) {
            if (jsonFormatBox.isSelected()) {
                conn.inputText.setText(conn.request.toUrlDecodedString(true));
            } else {
                conn.inputText.setText(conn.request.toUrlDecodedString(false));
            }

            conn.inputText.setCaretPosition(0);
        }

        if (conn.response != null) {
            if (jsonFormatBox.isSelected()) {
                conn.outputText.setText(conn.response.toString(true));
            } else {
                conn.outputText.setText(conn.response.toString(false));
            }
            conn.outputText.setCaretPosition(0);
        }
    }

    class ConnectionCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component result = super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            if (column == 0 && row > 0) {
                HttpConnection conn = tableModel.getConnectionByRow(row);
                if (conn.error) {
                    // error
                    if (isSelected) {
                        setForeground(Color.BLACK);
                        setBackground(Color.CYAN);
                    } else {
                        setForeground(Color.BLACK);
                        setBackground(Color.RED);
                    }
                } else if (conn.mockResponse) {
                    // profile mock
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

    public void requestEditDone(Action action, HttpRequest newData) {
        if (ReqDataEditor.Action.RESEND.equals(action)) {
            doResend(newData);
        } else {
            // we should never reach here
            JOptionPane.showMessageDialog(this.outPane, "Invalid action from request editor: "
                    + action);
        }
    }

    public void newConnection(Socket inSocket) {
        new HttpConnection(this, inSocket);
    }

    public static class HttpConnectionTableModel extends ConnectionTableModel<HttpConnection> {

        private static final long     serialVersionUID = 1L;

        private static final String[] COLUMN_NAME      = { "State", "Time", "Request Host",
                                                               "Target Host", "Request...",
                                                               "Elapsed Time" };
        private static final String[] RECENT_ROW       = { "---", "Most Recent", "---", "---",
                                                               "---", "---" };

        public HttpConnectionTableModel() {
            super(COLUMN_NAME, RECENT_ROW);
        }

        @Override
        protected Object doGetValueAt(int rowIndex, int columnIndex) {
            HttpConnection conn = data.get(rowIndex - 1);
            switch (columnIndex) {
                case 0:
                    return conn.state;
                case 1:
                    return conn.time;
                case 2:
                    return conn.fromHost;
                case 3:
                    return conn.targetHost;
                case 4:
                    return conn.requestSummary;
                case 5:
                    return conn.elapsedTime;
                default:
                    return null;
            }
        }
    }
}

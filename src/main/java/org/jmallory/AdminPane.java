package org.jmallory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.jmallory.http.HttpListener;
import org.jmallory.http.Profile;
import org.jmallory.http.ProfileEditor;
import org.jmallory.http.ProfileManager;
import org.jmallory.http.ProfileOrderAdjustEditor;
import org.jmallory.http.ProfileTableModel;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.smtp.SendMailDialog;
import org.jmallory.smtp.SmtpListener;
import org.jmallory.swing.HostnameField;
import org.jmallory.swing.Scrapbook;
import org.jmallory.swing.NumberField;
import org.jmallory.tcp.TcpListener;
import org.jmallory.util.Utils;

public class AdminPane extends JPanel {

    public static enum ListenerType {
        HTTP,
        HTTPS,
        SMTP,
        TCP;
    }

    private static final long serialVersionUID = 1L;

    public NumberField        localPort;
    public HostnameField      targetHost;
    public NumberField        targetPort;
    public JComboBox          listenerType;
    public JTextField         comment;
    public JTable             listenerConfigTable;

    /**
     * Field noteb
     */
    public JTabbedPane        noteb;

    /**
     * Field HTTPProxyBox
     */
    public JCheckBox          HTTPProxyBox;

    /**
     * Field HTTPProxyHost
     */
    public HostnameField      HTTPProxyHost;

    /**
     * Field HTTPProxyPort
     */
    public NumberField        HTTPProxyPort;

    /**
     * Field HTTPProxyHostLabel, HTTPProxyPortLabel
     */
    public JLabel             HTTPProxyHostLabel, HTTPProxyPortLabel;

    /**
     * Field delayTimeLabel, delayBytesLabel
     */
    public JLabel             delayTimeLabel, delayBytesLabel;

    /**
     * Field delayTime, delayBytes
     */
    public NumberField        delayTime, delayBytes;

    /**
     * Field delayBox
     */
    public JCheckBox          delayBox;

    /**
     * The data profile table
     */
    public JTable             profileTable;

    /**
     * Constructor AdminPage
     * 
     * @param notebook
     * @param name
     */
    public AdminPane(JTabbedPane notebook, String name, int lPort, String tHost, int tPort) {
        JPanel mainPane = null;

        this.setLayout(new BorderLayout());
        noteb = notebook;
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainPane = new JPanel(layout);

        JPanel listenerInfoPane = createListenerInfoPanel(lPort, tHost, tPort);

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(listenerInfoPane, c);

        JPanel advancedPane = createAdvancedPanel();
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        mainPane.add(advancedPane, c);

        JScrollPane mainScrollPane = new JScrollPane();
        mainScrollPane.setViewportView(mainPane);
        this.add(mainScrollPane, BorderLayout.CENTER);

        notebook.addTab(name, this);

        notebook.addTab("Scrapbook", new Scrapbook(Main.getFrame(), notebook));

        notebook.setSelectedIndex(0);
    }

    protected JPanel createListenerInfoPanel(int lPort, String tHost, int tPort) {
        JPanel listenerInfoPane = new JPanel(new BorderLayout());

        JPanel listenerEditPane = new JPanel();
        listenerEditPane.setLayout(new BoxLayout(listenerEditPane, BoxLayout.Y_AXIS));
        JPanel listenerEditPaneLine2 = new JPanel();
        listenerEditPaneLine2.setLayout(new BoxLayout(listenerEditPaneLine2, BoxLayout.X_AXIS));

        JPanel listenerEditPaneLine1 = new JPanel();
        listenerEditPaneLine1.setLayout(new BoxLayout(listenerEditPaneLine1, BoxLayout.X_AXIS));
        listenerEditPaneLine1.add(new JLabel("From Port:"));
        listenerEditPaneLine1.add(localPort = new NumberField(4));
        if (lPort != 0) {
            localPort.setText(String.valueOf(lPort));
        } else {
            localPort.setValue(Main.DEFAULT_LISTEN_PORT);
        }

        listenerEditPaneLine1.add(new JLabel(" Target Host:"));
        targetHost = new HostnameField(15);
        listenerEditPaneLine1.add(targetHost);
        if (tHost != null) {
            targetHost.setText(tHost);
        } else {
            targetHost.setText(Main.DEFAULT_HOST);
        }
        listenerEditPaneLine1.add(new JLabel(" Port:"));

        targetPort = new NumberField(4);
        listenerEditPaneLine1.add(targetPort);
        if (tPort != 0) {
            targetPort.setText(String.valueOf(tPort));
        } else {
            targetPort.setValue(Main.DEFAULT_PORT);
        }

        listenerType = new JComboBox(new String[] { "HTTP", "TCP", "SMTP" });
        listenerEditPaneLine2.add(new JLabel("Type:"));
        listenerEditPaneLine2.add(listenerType);

        listenerEditPaneLine2.add(new JLabel("Comment:"));
        listenerEditPaneLine2.add(comment = new JTextField());

        listenerEditPane.add(listenerEditPaneLine1);
        listenerEditPane.add(listenerEditPaneLine2);

        listenerInfoPane.add(listenerEditPane, BorderLayout.NORTH);

        final ListenerConfigTableModel listenerConfigTableModel = new ListenerConfigTableModel();
        listenerConfigTable = new JTable(listenerConfigTableModel);
        int preferredWidth = listenerConfigTable.getColumnModel().getColumn(0).getPreferredWidth();
        listenerConfigTable.getColumnModel().getColumn(0).setPreferredWidth(preferredWidth / 2);
        listenerConfigTable.getColumnModel().getColumn(1)
                .setPreferredWidth((int) (preferredWidth * 1.5));
        listenerConfigTable.getColumnModel().getColumn(2).setPreferredWidth(preferredWidth / 2);
        listenerConfigTable.getColumnModel().getColumn(3).setPreferredWidth(preferredWidth / 2);
        listenerConfigTable.getColumnModel().getColumn(4).setPreferredWidth(preferredWidth * 3);

        final JScrollPane hostTableScroll = new JScrollPane();
        hostTableScroll.setViewportView(listenerConfigTable);
        hostTableScroll.setPreferredSize(new Dimension(580, 155));

        listenerInfoPane.add(hostTableScroll, BorderLayout.CENTER);
        final JButton newButton = new JButton("++");
        newButton.setToolTipText("Save as New");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isListenerConfigValid()) {
                    int rowToSelect = listenerConfigTableModel.addConfig(
                            new ListenerConfig(localPort.getText(), targetHost.getText(),
                                    targetPort.getText(), String.valueOf(listenerType
                                            .getSelectedItem()), comment.getText()),
                            listenerConfigTable.getSelectedRow(), false);
                    listenerConfigTable.getSelectionModel().setSelectionInterval(rowToSelect,
                            rowToSelect);
                }
            }
        });

        final JButton saveButton = new JButton("==");
        saveButton.setEnabled(false);
        saveButton.setToolTipText("Save Edit");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isListenerConfigValid()) {
                    int rowToSelect = listenerConfigTableModel.addConfig(
                            new ListenerConfig(localPort.getText(), targetHost.getText(),
                                    targetPort.getText(), String.valueOf(listenerType
                                            .getSelectedItem()), comment.getText()),
                            listenerConfigTable.getSelectedRow(), true);
                    listenerConfigTable.getSelectionModel().setSelectionInterval(rowToSelect,
                            rowToSelect);
                }
            }
        });
        final JButton deleteButton = new JButton("--");
        deleteButton.setEnabled(false);
        deleteButton.setToolTipText("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listenerConfigTableModel.deleteConfig(listenerConfigTable.getSelectedRows());
            }
        });

        JPanel hostTableEditButtonPane = new JPanel();
        hostTableEditButtonPane.setLayout(new GridLayout(5, 1));
        hostTableEditButtonPane.add(newButton);
        hostTableEditButtonPane.add(saveButton);
        hostTableEditButtonPane.add(deleteButton);

        listenerInfoPane.add(hostTableEditButtonPane, BorderLayout.WEST);

        final JButton topButton = new JButton("Top");
        topButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectRowsAndScrollToVisible(listenerConfigTable,
                        listenerConfigTableModel.moveTop(listenerConfigTable.getSelectedRows()),
                        false);
            }
        });
        final JButton upButton = new JButton("Up");
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectRowsAndScrollToVisible(listenerConfigTable,
                        listenerConfigTableModel.moveUp(listenerConfigTable.getSelectedRows()),
                        false);
            }
        });
        final JButton downButton = new JButton("Down");
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectRowsAndScrollToVisible(listenerConfigTable,
                        listenerConfigTableModel.moveDown(listenerConfigTable.getSelectedRows()),
                        true);
            }
        });
        final JButton bottomButton = new JButton("Bottom");
        bottomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectRowsAndScrollToVisible(listenerConfigTable,
                        listenerConfigTableModel.moveBottom(listenerConfigTable.getSelectedRows()),
                        true);
            }
        });

        topButton.setEnabled(false);
        upButton.setEnabled(false);
        downButton.setEnabled(false);
        bottomButton.setEnabled(false);

        JPanel hostTableMoveButtonPane = new JPanel();
        hostTableMoveButtonPane.setLayout(new GridLayout(4, 1));
        hostTableMoveButtonPane.add(topButton);
        hostTableMoveButtonPane.add(upButton);
        hostTableMoveButtonPane.add(downButton);
        hostTableMoveButtonPane.add(bottomButton);

        listenerInfoPane.add(hostTableMoveButtonPane, BorderLayout.EAST);

        listenerConfigTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        int[] rows = listenerConfigTable.getSelectedRows();

                        if (rows != null && rows.length == 1) {
                            int row = listenerConfigTable.getSelectedRow();
                            if (row != -1) {
                                ListenerConfigTableModel model = (ListenerConfigTableModel) listenerConfigTable
                                        .getModel();
                                ListenerConfig config = model.getConfig(row);
                                localPort.setText(config.lport);
                                targetHost.setText(config.thost);
                                targetPort.setText(config.tport);
                                listenerType.setSelectedItem(config.type);
                                comment.setText(config.comment);
                                saveButton.setEnabled(true);
                            }
                        } else {
                            localPort.setText("");
                            targetHost.setText("");
                            targetPort.setText("");
                            listenerType.setSelectedIndex(0);
                            comment.setText("");
                            saveButton.setEnabled(false);
                        }

                        if (rows != null && rows.length > 0) {
                            deleteButton.setEnabled(true);
                            if (rows[rows.length - 1] - rows[0] == rows.length - 1) {
                                topButton.setEnabled(true);
                                upButton.setEnabled(true);
                                downButton.setEnabled(true);
                                bottomButton.setEnabled(true);
                            } else {
                                topButton.setEnabled(false);
                                upButton.setEnabled(false);
                                downButton.setEnabled(false);
                                bottomButton.setEnabled(false);
                            }
                        } else {
                            deleteButton.setEnabled(false);
                            topButton.setEnabled(false);
                            upButton.setEnabled(false);
                            downButton.setEnabled(false);
                            bottomButton.setEnabled(false);
                        }
                    }
                });

        JButton addSelectListenerButton = new JButton("Add Selected Listeners");
        addSelectListenerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedCount = listenerConfigTable.getSelectedRowCount();
                if (selectedCount == 0) {
                    JOptionPane.showMessageDialog(AdminPane.this,
                            "No listener configuration selected!");
                    return;
                }

                int[] rows = listenerConfigTable.getSelectedRows();

                ListenerConfigTableModel model = (ListenerConfigTableModel) listenerConfigTable
                        .getModel();

                ListenerConfig[] listenerConfigs = new ListenerConfig[rows.length];
                for (int i = 0; i < listenerConfigs.length; i++) {
                    listenerConfigs[i] = model.getConfig(rows[i]);
                }
                for (int j = 0; j < listenerConfigs.length; j++) {
                    ListenerConfig config = listenerConfigs[j];
                    ListenerType type = ListenerType.valueOf(config.type);
                    switch (type) {
                        case HTTP:
                            addHttpListener(config.getListenerPort(), config.getTargetHost(),
                                    config.getTargetPort(), null, -1, null, null, false);
                            break;
                        case SMTP:
                            addSmtpListener(config.getListenerPort(), config.getTargetHost(),
                                    config.getTargetPort(), null, false);
                            break;
                        case TCP:
                            addTcpListener(config.getListenerPort(), config.getTargetHost(),
                                    config.getTargetPort(), null);
                            break;
                    }
                }
            }
        });

        listenerInfoPane.add(addSelectListenerButton, BorderLayout.SOUTH);
        return listenerInfoPane;
    }

    protected JPanel createAdvancedPanel() {
        JButton addHttpButton = null;
        JButton addTcpButton = null;
        JButton addSmtpButton = null;

        JPanel advancedPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // /////////////////////////////////////////////////////////////////
        JPanel opts = new JPanel();
        opts.setBorder(new TitledBorder("Options"));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        advancedPane.add(opts, c);

        // HTTP Proxy Support section
        // /////////////////////////////////////////////////////////////////

        JPanel proxyPane = new JPanel(new GridBagLayout());

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        final String proxySupport = "HTTP Proxy Support";
        proxyPane.add(HTTPProxyBox = new JCheckBox(proxySupport), c);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        proxyPane.add(HTTPProxyHostLabel = new JLabel("Hostname"), c);
        HTTPProxyHostLabel.setForeground(Color.gray);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        proxyPane.add(HTTPProxyHost = new HostnameField(10), c);
        HTTPProxyHost.setEnabled(false);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        proxyPane.add(HTTPProxyPortLabel = new JLabel("Port #" + " "), c);
        HTTPProxyPortLabel.setForeground(Color.gray);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        proxyPane.add(HTTPProxyPort = new NumberField(4), c);
        HTTPProxyPort.setEnabled(false);
        HTTPProxyBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (proxySupport.equals(event.getActionCommand())) {
                    boolean b = HTTPProxyBox.isSelected();
                    Color color = b ? Color.black : Color.gray;
                    HTTPProxyHost.setEnabled(b);
                    HTTPProxyPort.setEnabled(b);
                    HTTPProxyHostLabel.setForeground(color);
                    HTTPProxyPortLabel.setForeground(color);
                }
            }
        });

        // Set default proxy values...
        String tmp = System.getProperty("http.proxyHost");
        if ((tmp != null) && tmp.equals("")) {
            tmp = null;
        }
        HTTPProxyBox.setSelected(tmp != null);
        HTTPProxyHost.setEnabled(tmp != null);
        HTTPProxyPort.setEnabled(tmp != null);
        HTTPProxyHostLabel.setForeground((tmp != null) ? Color.black : Color.gray);
        HTTPProxyPortLabel.setForeground((tmp != null) ? Color.black : Color.gray);
        if (tmp != null) {
            HTTPProxyBox.setSelected(true);
            HTTPProxyHost.setText(tmp);
            tmp = System.getProperty("http.proxyPort");
            if ((tmp != null) && tmp.equals("")) {
                tmp = null;
            }
            if (tmp == null) {
                tmp = "80";
            }
            HTTPProxyPort.setText(tmp);
        }

        JPanel slowConnectionPane = new JPanel(new GridBagLayout());

        // add byte delay fields
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        final String delaySupport = "Simulate Slow Connection";
        slowConnectionPane.add(delayBox = new JCheckBox(delaySupport), c);

        // bytes per pause
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        delayBytesLabel = new JLabel("Bytes per Pause");
        slowConnectionPane.add(delayBytesLabel, c);
        delayBytesLabel.setForeground(Color.gray);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        slowConnectionPane.add(delayBytes = new NumberField(6), c);
        delayBytes.setEnabled(false);

        // delay interval
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        delayTimeLabel = new JLabel("Delay in Milliseconds");
        slowConnectionPane.add(delayTimeLabel, c);
        delayTimeLabel.setForeground(Color.gray);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        slowConnectionPane.add(delayTime = new NumberField(6), c);
        delayTime.setEnabled(false);

        // enabler callback
        delayBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (delaySupport.equals(event.getActionCommand())) {
                    boolean b = delayBox.isSelected();
                    Color color = b ? Color.black : Color.gray;
                    delayBytes.setEnabled(b);
                    delayTime.setEnabled(b);
                    delayBytesLabel.setForeground(color);
                    delayTimeLabel.setForeground(color);
                }
            }
        });

        opts.add(proxyPane);
        opts.add(slowConnectionPane);

        // Spacer
        // ////////////////////////////////////////////////////////////////
        advancedPane.add(Box.createRigidArea(new Dimension(1, 10)), c);

        // Tcp Listner with Profiles
        // /////////////////////////////////////////////////////////////////
        JPanel httpListenerPane = new JPanel();
        httpListenerPane.setLayout(new BoxLayout(httpListenerPane, BoxLayout.Y_AXIS));
        httpListenerPane.setBorder(new TitledBorder("HTTP Listner with Profiles"));

        // Profile Table
        // /////////////////////////////////////////////////////////////////
        JButton editButton = null;
        JButton deleteButton = null;

        JPanel tablePane = new JPanel(new BorderLayout());
        tablePane
                .add(profileTable = new JTable(new ProfileTableModel(ProfileManager
                        .reloadAllProfile())), BorderLayout.CENTER);
        tablePane.add(profileTable.getTableHeader(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(profileTable);
        httpListenerPane.add(scrollPane, BorderLayout.CENTER);
        httpListenerPane.setPreferredSize(new Dimension(400, 185));

        final String editProfile = "Edit Profile";
        editButton = new JButton(editProfile);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = profileTable.getSelectedRow();
                if (selectedRow == -1) {
                    // nothing selected, do nothing
                    return;
                }

                String profileName = (String) profileTable.getModel().getValueAt(selectedRow, 0);

                for (int i = 0, tabCount = noteb.getTabCount(); i < tabCount; i++) {
                    Component component = noteb.getComponentAt(i);
                    if (component instanceof ProfileEditor) {
                        ProfileEditor profileEditor = (ProfileEditor) component;
                        if (profileName.equals(profileEditor.getProfile().getName())) {
                            // editor is opened, switch to it
                            noteb.setSelectedIndex(i);
                            return;
                        }
                    }
                }

                // editor is not opened, open it
                new ProfileEditor(noteb, ProfileManager.getProfile(profileName));
                noteb.setSelectedIndex(noteb.getTabCount() - 1);
            }
        });

        final String deleteProfile = "Delete Profile";
        deleteButton = new JButton(deleteProfile);
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int[] selectedRows = profileTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    // nothing selected, do nothing
                    return;
                }

                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    String profileName = (String) profileTable.getModel().getValueAt(
                            selectedRows[i], 0);

                    for (int j = 0, tabCount = noteb.getTabCount(); j < tabCount; j++) {
                        Component component = noteb.getComponentAt(j);
                        if (component instanceof ProfileEditor) {
                            ProfileEditor profileEditor = (ProfileEditor) component;
                            if (profileName.equals(profileEditor.getProfile().getName())) {
                                profileEditor.close();
                                return;
                            }
                        }
                    }

                    ProfileManager.deleteProfile(profileName);
                }
                ((AbstractTableModel) profileTable.getModel()).fireTableDataChanged();
            }
        });

        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel profileTableButtonPane = new JPanel();
        profileTableButtonPane.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 1));
        profileTableButtonPane.setLayout(new BoxLayout(profileTableButtonPane, BoxLayout.X_AXIS));

        addHttpButton = new JButton("Add HTTP ConnectionListener with Profiles");
        final JCheckBox httpsTarget = new JCheckBox("HTTPS-Target");

        profileTableButtonPane.add(addHttpButton);
        profileTableButtonPane.add(httpsTarget);
        profileTableButtonPane.add(Box.createGlue());

        profileTableButtonPane.add(editButton);
        profileTableButtonPane.add(deleteButton);
        httpListenerPane.add(profileTableButtonPane, BorderLayout.SOUTH);

        advancedPane.add(httpListenerPane, c);

        // Spacer
        // ////////////////////////////////////////////////////////////////
        advancedPane.add(Box.createRigidArea(new Dimension(1, 10)), c);

        // ADD Button
        // /////////////////////////////////////////////////////////////////

        JPanel smtpButtonPane = new JPanel();
        smtpButtonPane.setLayout(new BoxLayout(smtpButtonPane, BoxLayout.X_AXIS));
        smtpButtonPane.setBorder(new TitledBorder("SMTP ConnectionListener & Send Mail"));

        smtpButtonPane.add(addSmtpButton = new JButton("Add SMTP ConnectionListener"));

        final JCheckBox mockSmtp = new JCheckBox("Mock SMTP");
        smtpButtonPane.add(mockSmtp);

        smtpButtonPane.add(Box.createHorizontalStrut(15));

        final JButton sendMail = new JButton("Send Mail");
        smtpButtonPane.add(sendMail);

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        advancedPane.add(smtpButtonPane, c);

        JPanel tcpButtonPane = new JPanel();
        tcpButtonPane.setLayout(new BoxLayout(tcpButtonPane, BoxLayout.X_AXIS));
        tcpButtonPane.setBorder(new TitledBorder("TCP ConnectionListener"));
        tcpButtonPane.add(addTcpButton = new JButton("Add TCP ConnectionListener"));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        advancedPane.add(tcpButtonPane, c);

        JPanel systemButtonPane = new JPanel();
        systemButtonPane.setLayout(new BoxLayout(systemButtonPane, BoxLayout.X_AXIS));
        systemButtonPane.setBorder(new TitledBorder("System"));

        final JButton gcButton = new JButton("Garbage Collection");
        systemButtonPane.add(gcButton);

        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        advancedPane.add(systemButtonPane, c);

        final Runnable updateGcInfo = new Runnable() {
            @Override
            public void run() {
                long total = Runtime.getRuntime().totalMemory();
                long free = Runtime.getRuntime().freeMemory();
                long used = total - free;
                gcButton.setText("GC " + (used / 1024 / 1024) + "/" + (total / 1024 / 1024) + "MB");
            }
        };
        gcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.gc();
                updateGcInfo.run();
            }
        });

        Thread gcInfoThread = new Thread() {
            public void run() {
                while (!Thread.interrupted()) {
                    SwingUtilities.invokeLater(updateGcInfo);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        gcInfoThread.setDaemon(true);
        gcInfoThread.start();

        addHttpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isListenerConfigValid()) {
                    int[] selectedRows = profileTable.getSelectedRows();
                    if (selectedRows.length == 0) {
                        addListenerWithProfiles(null, httpsTarget.isSelected());
                    } else if (selectedRows.length == 1) {
                        Profile[] profiles = new Profile[selectedRows.length];
                        for (int i = 0; i < selectedRows.length; i++) {
                            profiles[i] = ProfileManager.findExistProfile((String) profileTable
                                    .getModel().getValueAt(selectedRows[i], 0));
                        }
                        addListenerWithProfiles(profiles, httpsTarget.isSelected());
                    } else {
                        Profile[] profiles = new Profile[selectedRows.length];
                        for (int i = 0; i < selectedRows.length; i++) {
                            profiles[i] = ProfileManager.findExistProfile((String) profileTable
                                    .getModel().getValueAt(selectedRows[i], 0));
                        }
                        Arrays.sort(profiles); // sort the profile
                        new ProfileOrderAdjustEditor(Main.getFrame(), AdminPane.this, profiles,
                                httpsTarget.isSelected()).setVisible(true);
                    }
                }
            }
        });
        addTcpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isListenerConfigValid()) {
                    int lPort = localPort.getValue(0);
                    String tHost = targetHost.getText();
                    int tPort = 0;
                    tPort = targetPort.getValue(0);
                    SlowLinkSimulator slowLink = null;
                    if (delayBox.isSelected()) {
                        int bytes = delayBytes.getValue(0);
                        int time = delayTime.getValue(0);
                        slowLink = new SlowLinkSimulator(bytes, time);
                    }

                    addTcpListener(lPort, tHost, tPort, slowLink);

                    // reset the port
                    localPort.setText(null);
                }
            }
        });
        addSmtpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isListenerConfigValid()) {
                    int lPort = localPort.getValue(0);
                    String tHost = targetHost.getText();
                    int tPort = 0;
                    tPort = targetPort.getValue(0);
                    SlowLinkSimulator slowLink = null;
                    if (delayBox.isSelected()) {
                        int bytes = delayBytes.getValue(0);
                        int time = delayTime.getValue(0);
                        slowLink = new SlowLinkSimulator(bytes, time);
                    }

                    addSmtpListener(lPort, tHost, tPort, slowLink, mockSmtp.isSelected());

                    // reset the port
                    localPort.setText(null);
                }
            }
        });

        sendMail.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                int tport = -1;
                try {
                    tport = Integer.valueOf(AdminPane.this.targetPort.getText());
                } catch (Exception e2) {
                }

                SendMailDialog sendMailDialog = new SendMailDialog(Main.getFrame(),
                        AdminPane.this.targetHost.getText(), tport != -1 ? tport : 2500);
                sendMailDialog.setVisible(true);
            }
        });

        return advancedPane;
    }

    protected boolean isListenerConfigValid() {
        int lPort = localPort.getValue(0);
        if (lPort == 0) {
            JOptionPane.showMessageDialog(AdminPane.this, "Please specify Local Port!");
            return false;
        }
        String tHost = targetHost.getText();
        if (tHost == null || tHost.trim().length() == 0) {
            JOptionPane.showMessageDialog(AdminPane.this, "Please specify Target Host!");
            return false;
        }
        int tPort = targetPort.getValue(0);
        if (tPort == 0) {
            JOptionPane.showMessageDialog(AdminPane.this, "Please specify Target Port!");
            return false;
        }

        return true;
    }

    protected void selectRowsAndScrollToVisible(JTable table, int[] rows, boolean ScrollToLastRow) {
        if (rows != null && rows.length != 0) {
            table.getSelectionModel().setSelectionInterval(rows[0], rows[rows.length - 1]);
            if (ScrollToLastRow) {
                table.scrollRectToVisible(table.getCellRect(rows[rows.length - 1], 0, true));
            } else {
                table.scrollRectToVisible(table.getCellRect(rows[0], 0, true));
            }
        }
    }

    public void addListenerWithProfiles(Profile[] profiles, boolean useHttpsTarget) {
        int lPort;
        lPort = localPort.getValue(0);
        if (lPort == 0) {
            // no port, button does nothing
            JOptionPane.showMessageDialog(AdminPane.this, "Please specify Listen Port");
            return;
        }
        String tHost = targetHost.getText();
        int tPort = 0;
        tPort = targetPort.getValue(0);

        // Pick-up the HTTP Proxy settings
        // /////////////////////////////////////////////////
        String proxyHost = null;
        int proxyPort = -1;
        if (HTTPProxyBox.isSelected()) {
            String text = HTTPProxyHost.getText();
            if ("".equals(text)) {
                text = null;
            }
            proxyHost = text;
            text = HTTPProxyPort.getText();
            proxyPort = HTTPProxyPort.getValue(-1);
            if (proxyPort != -1) {
                proxyPort = Integer.parseInt(text);
            }
        }

        // Pick-up the slow connection
        SlowLinkSimulator slowLink = null;
        if (delayBox.isSelected()) {
            int bytes = delayBytes.getValue(0);
            int time = delayTime.getValue(0);
            slowLink = new SlowLinkSimulator(bytes, time);
        }

        addHttpListener(lPort, tHost, tPort, proxyHost, proxyPort, slowLink, profiles,
                useHttpsTarget);

        // reset the port
        localPort.setText(null);
        // we don't switch to the last opened tab
        // noteb.setSelectedIndex(noteb.getTabCount() - 1);
    }

    protected void addHttpListener(int lPort, String tHost, int tPort, String proxyHost,
                                   int proxyPort, SlowLinkSimulator slowLink, Profile[] profiles,
                                   boolean useHttpsTarget) {
        new HttpListener(noteb, null, lPort, tHost, tPort, proxyHost, proxyPort, slowLink,
                profiles, useHttpsTarget);
        addListenerSuccessfullyCallback(ListenerType.HTTP.name(), lPort, tHost, tPort);
    }

    protected void addTcpListener(int lPort, String tHost, int tPort, SlowLinkSimulator slowLink) {
        new TcpListener(noteb, null, lPort, tHost, tPort, slowLink);
        addListenerSuccessfullyCallback(ListenerType.TCP.name(), lPort, tHost, tPort);
    }

    protected void addSmtpListener(int lPort, String tHost, int tPort, SlowLinkSimulator slowLink,
                                   boolean mockSmtp) {
        new SmtpListener(noteb, null, lPort, tHost, tPort, slowLink, mockSmtp);
        addListenerSuccessfullyCallback(ListenerType.SMTP.name(), lPort, tHost, tPort);
    }

    protected void addListenerSuccessfullyCallback(String type, int lPort, String tHost, int tPort) {
        ((ListenerConfigTableModel) listenerConfigTable.getModel()).addConfig(new ListenerConfig(
                lPort, tHost, tPort, type, comment.getText()), 0, false);
    }

    public static class ListenerConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        public String             lport;
        public String             thost;
        public String             tport;
        public String             type;
        public String             comment;

        public ListenerConfig(int lport, String thost, int tport, String type, String comment) {
            this.lport = String.valueOf(lport);
            this.thost = thost;
            this.tport = String.valueOf(tport);
            this.type = type;
            this.comment = comment;
        }

        public ListenerConfig(String lport, String thost, String tport, String type, String comment) {
            this.lport = lport;
            this.thost = thost;
            this.tport = tport;
            this.type = type;
            this.comment = comment;
        }

        public int getListenerPort() {
            return lport != null ? Integer.parseInt(lport) : -1;
        }

        public String getTargetHost() {
            return thost;
        }

        public int getTargetPort() {
            return tport != null ? Integer.parseInt(tport) : -1;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListenerConfig)) {
                return false;
            }

            ListenerConfig another = (ListenerConfig) o;
            if (lport == null || thost == null || tport == null || type == null
                    || another.lport == null || another.thost == null || another.tport == null
                    || another.type == null) {
                return false;
            }

            return lport.equals(another.lport) && thost.equals(another.thost)
                    && tport.equals(another.tport) && type.equals(another.type);
        }

        public String toString() {
            return lport + ":" + thost + ":" + tport + ":" + type + ":"
                    + (comment != null ? comment : "");
        }
    }

    public static class ListenerConfigTableModel extends AbstractTableModel {
        private static final long     serialVersionUID = 1L;

        private static final String[] COLUMN_NAMES     = new String[] { "LPort", "THost", "TPort",
                                                               "Type", "Comment" };

        private List<ListenerConfig>  data             = new ArrayList<ListenerConfig>();

        public ListenerConfigTableModel() {
            load(new File("listener_config.txt"));
        }

        private void load(File file) {
            if (file == null || !file.exists()) {
                return;
            }

            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        String[] parts = line.split(":");
                        data.add(new ListenerConfig(parts[0], parts[1], parts[2],
                                parts.length > 3 ? parts[3] : ListenerType.HTTP.name(),// default to HTTP
                                parts.length > 4 ? parts[4] : ""));
                    }
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void save() {
            File file = new File("listener_config.txt");
            PrintWriter writer;
            try {
                writer = new PrintWriter(file);
                for (int i = 0; i < data.size(); i++) {
                    writer.println(data.get(i));
                }
                writer.close();
                Utils.chmod777(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        public ListenerConfig getConfig(int rowIndex) {
            if (rowIndex < 0 || rowIndex > data.size() - 1) {
                throw new ArrayIndexOutOfBoundsException("rowIndex is out of bound");
            }
            return data.get(rowIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex > data.size() - 1) {
                throw new ArrayIndexOutOfBoundsException("rowIndex is out of bound");
            }

            ListenerConfig config = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return config.lport;
                case 1:
                    return config.thost;
                case 2:
                    return config.tport;
                case 3:
                    return config.type;
                case 4:
                    return config.comment;
            }

            return null;
        }

        /**
         * @param config
         * @param index index of the new row
         * @param asEdit
         * @return
         */
        public int addConfig(ListenerConfig config, int index, boolean asEdit) {

            if (asEdit) {
                data.set(index, config);
                fireTableRowsUpdated(index, index);
                save();
                return index;
            } else {
                // save as new, but do we have the same config already? if so, only update comment
                for (int i = 0; i < data.size(); i++) {
                    ListenerConfig temp = data.get(i);
                    if (temp.equals(config)) {
                        temp.comment = config.comment;
                        fireTableRowsUpdated(i, i);
                        save();
                        return i;
                    }
                }

                // no same config, brand new, save it to the top
                data.add(0, config);
                fireTableDataChanged();
                save();
                return 0;
            }
        }

        public void deleteConfig(int[] rows) {
            if (rows == null || rows.length == 0) {
                return;
            }

            for (int i = rows.length - 1; i >= 0; i--) {
                data.remove(rows[i]);
            }
            fireTableDataChanged();
            save();
        }

        public int[] moveTop(int[] rows) {
            if (rows == null || rows.length == 0) {
                return rows;
            }

            for (int i = 0; i < rows.length; i++) {
                int rowIndex = rows[i];
                for (int j = rowIndex; j > i; j--) {
                    swap(j, j - 1);
                }
                rows[i] = i;
            }
            fireTableDataChanged();
            save();
            return rows;
        }

        public int[] moveUp(int[] rows) {
            if (rows == null || rows.length == 0 || rows[0] == 0) {
                return rows;
            }

            for (int i = 0; i < rows.length; i++) {
                int rowIndex = rows[i];
                swap(rowIndex, rowIndex - 1);
                rows[i] = rowIndex - 1;
            }
            fireTableDataChanged();
            save();
            return rows;
        }

        public int[] moveDown(int[] rows) {
            if (rows == null || rows.length == 0 || rows[rows.length - 1] == data.size() - 1) {
                return rows;
            }

            for (int i = rows.length - 1; i >= 0; i--) {
                int rowIndex = rows[i];
                swap(rowIndex, rowIndex + 1);
                rows[i] = rowIndex + 1;
            }
            fireTableDataChanged();
            save();
            return rows;
        }

        public int[] moveBottom(int[] rows) {
            if (rows == null || rows.length == 0) {
                return rows;
            }

            for (int i = rows.length - 1; i >= 0; i--) {
                int rowIndex = rows[i];
                int end = data.size() - (rows.length - i);
                for (int j = rowIndex; j < end; j++) {
                    swap(j, j + 1);
                }
                rows[i] = end;
            }
            fireTableDataChanged();
            save();
            return rows;
        }

        private void swap(int i, int j) {
            ListenerConfig tempConfig = data.get(i);
            data.set(i, data.get(j));
            data.set(j, tempConfig);
        }
    }
}

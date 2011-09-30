package org.jmallory.tcp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import javax.swing.table.TableColumn;

import org.jmallory.Main;
import org.jmallory.io.SlowLinkSimulator;
import org.jmallory.io.ConnectionAcceptor;
import org.jmallory.model.ConnectionListener;
import org.jmallory.model.ConnectionListener.ConnectionTableModel;
import org.jmallory.swing.ClosableTabComponent;

public class TcpListener extends JPanel implements ConnectionListener {

    private static final long      serialVersionUID = 1L;

    public JTextField              portField        = null;
    public JTextField              hostField        = null;
    public JTextField              tPortField       = null;

    public JButton                 stopButton       = null;
    public JButton                 removeButton     = null;
    public JButton                 removeAllButton  = null;
    public JButton                 disconnectButton = null;

    public JButton                 switchButton     = null;
    public JButton                 closeButton      = null;

    public JTable                  connectionTable  = null;
    public TcpConnectionTableModel tableModel       = null;

    public JSplitPane              outPane          = null;

    public ConnectionAcceptor      sw               = null;

    public JPanel                  leftPanel        = null;
    public JPanel                  rightPanel       = null;

    public ClosableTabComponent    closeTab         = null;
    public JTabbedPane             notebook         = null;

    public int                     delayBytes       = 0;
    public int                     delayTime        = 0;

    public SlowLinkSimulator       slowLink;

    private String                 listenerName;

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
    public TcpListener(JTabbedPane _notebook, String name, int listenPort, String host,
                       int targetPort, SlowLinkSimulator slowLink) {
        notebook = _notebook;

        if (name == null) {
            name = "TCP Port " + listenPort;
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
        top.add(new JLabel("This is a trival TCP tunnel listener."), BorderLayout.NORTH);
        top.add(topControls, BorderLayout.CENTER);

        this.add(top, BorderLayout.NORTH);

        // 2nd component is a split pane with a table on the top
        // and the request/response text areas on the bottom
        // ///////////////////////////////////////////////////////////////////

        tableModel = new TcpConnectionTableModel();
        connectionTable = new JTable(1, 2);
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
                    disconnectButton.setEnabled(false);
                } else {
                    int row = connectionTable.getSelectedRow();
                    if (row == 0) {
                        if (tableModel.size() == 0) {
                            setLeft(new JLabel(" Waiting for connection..."));
                            setRight(null);
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(false);
                            disconnectButton.setEnabled(false);
                        } else {
                            TcpConnection conn = tableModel.lastConnection();

                            setLeft(conn.inputScroll);
                            setRight(conn.outputScroll);
                            removeButton.setEnabled(false);
                            removeAllButton.setEnabled(true);
                            disconnectButton.setEnabled(false);
                        }
                    } else {
                        TcpConnection conn = tableModel.getConnectionByRow(row);

                        setLeft(conn.inputScroll);
                        setRight(conn.outputScroll);
                        removeButton.setEnabled(true);
                        removeAllButton.setEnabled(true);
                        disconnectButton.setEnabled(true);
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
        buttons.add(removeButton = new JButton("Remove Selected"));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(removeAllButton = new JButton("Remove All"));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(disconnectButton = new JButton("Disconnect Connection(s)"));
        tablePane.add(buttons, BorderLayout.SOUTH);
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                remove();
            }
        });
        removeAllButton.setEnabled(false);
        removeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                removeAll();
            }
        });
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                disconnect();
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
        bottomButtons.add(Box.createHorizontalGlue());
        final String switchStr = "Switch Layout";
        bottomButtons.add(switchButton = new JButton(switchStr));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String close = "Close";
        bottomButtons.add(closeButton = new JButton(close));
        pane2.add(bottomButtons, BorderLayout.SOUTH);

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
        pane1.setResizeWeight(0.5);
        this.add(pane1, BorderLayout.CENTER);

        // 
        // //////////////////////////////////////////////////////////////////
        sel.setSelectionInterval(0, 0);
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

    public void start() {
        int port = Integer.parseInt(portField.getText());
        portField.setText("" + port);
        int i = notebook.indexOfComponent(this);
        notebook.setTitleAt(i, "TCP Port " + port);
        notebook.setToolTipTextAt(i, listenerName);
        int tmp = Integer.parseInt(tPortField.getText());
        tPortField.setText("" + tmp);
        sw = new ConnectionAcceptor(this, port);
        stopButton.setText("Stop");
        portField.setEditable(false);
        hostField.setEditable(false);
        tPortField.setEditable(false);
        closeTab.setActive(true);
    }

    /**
     * Method close
     */
    public void close() {
        stop();
        notebook.remove(this);
    }

    /**
     * Method stop
     */
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

    public void removeAll() {
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        lsm.clearSelection();
        tableModel.removeAllConnection();
        lsm.setSelectionInterval(0, 0);
    }

    public void disconnect() {
        int[] rows = connectionTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            if (rows[i] != 0) {
                tableModel.haltConnectionByRow(rows[i]);
            }
        }
    }

    public void newConnection(Socket inSocket) {
        new TcpConnection(this, inSocket);
    }

    public static class TcpConnectionTableModel extends ConnectionTableModel<TcpConnection> {

        private static final long     serialVersionUID = 1L;

        private static final String[] COLUMN_NAME      = { "State", "Time", "Request Host:Port",
                                                               "Income Bytes", "Outcome Bytes",
                                                               "Elapsed Time" };
        private static final String[] RECENT_ROW       = { "---", "Most Recent", "---", "---",
                                                               "---", "---", "---" };

        public TcpConnectionTableModel() {
            super(COLUMN_NAME, RECENT_ROW);
        }

        @Override
        protected Object doGetValueAt(int rowIndex, int columnIndex) {
            TcpConnection conn = data.get(rowIndex - 1);
            switch (columnIndex) {
                case 0:
                    return conn.state;
                case 1:
                    return conn.time;
                case 2:
                    return conn.fromHost;
                case 3:
                    return conn.incomeBytes;
                case 4:
                    return conn.outcomeBytes;
                case 5:
                    return conn.elapsedTime;
                default:
                    return null;
            }
        }
    }
}

package org.jmallory.http;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.jmallory.Main;
import org.jmallory.swing.ClosableTabComponent;
import org.jmallory.swing.JTextAreaX;

public class ProfileEditor extends JPanel {

    private static final long    serialVersionUID = 1L;

    public Profile               profile;

    public JButton               newButton        = null;
    public JButton               editButton       = null;
    public JButton               copyButton       = null;

    /**
     * Field removeButton
     */
    public JButton               removeButton     = null;

    /**
     * Field removeAllButton
     */
    public JButton               removeAllButton  = null;

    /**
     * Field xmlFormatBox
     */
    public JCheckBox             jsonFormatBox    = null;

    /**
     * Field saveButton
     */
    public JButton               saveButton       = null;

    /**
     * Field switchButton
     */
    public JButton               switchButton     = null;

    /**
     * Field closeButton
     */
    public JButton               closeButton      = null;

    /**
     * Field connectionTable
     */
    public JTable                profileDataTable = null;

    /**
     * Field tableModel
     */
    public ProfileDataTableModel tableModel       = null;

    /**
     * Field outPane
     */
    public JSplitPane            outPane          = null;
    /**
     * Field inputText
     */
    public JTextArea             inputText        = null;

    /**
     * Field inputScroll
     */
    public JScrollPane           inputScroll      = null;

    /**
     * Field outputText
     */
    public JTextArea             outputText       = null;

    /**
     * Field outputScroll
     */
    public JScrollPane           outputScroll     = null;
    /**
     * Field sSocket
     */
    public ServerSocket          sSocket          = null;

    /**
     * Field leftPanel
     */
    public JPanel                leftPanel        = null;

    /**
     * Field rightPanel
     */
    public JPanel                rightPanel       = null;

    /**
     * Field notebook
     */
    public JTabbedPane           notebook         = null;

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
    public ProfileEditor(JTabbedPane _notebook, final Profile profile) {
        notebook = _notebook;
        this.profile = profile;

        this.setLayout(new BorderLayout());

        // 2nd component is a split pane with a table on the top
        // and the request/response text areas on the bottom
        // ///////////////////////////////////////////////////////////////////
        tableModel = new ProfileDataTableModel(profile.getData());

        profileDataTable = new JTable(1, 2);
        profileDataTable.setModel(tableModel);
        profileDataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        ListSelectionModel sel = profileDataTable.getSelectionModel();
        sel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    return;
                }

                List<ReqRespData> data = ProfileEditor.this.profile.getData();
                ListSelectionModel m = (ListSelectionModel) event.getSource();
                int row = m.getLeadSelectionIndex();
                int divLoc = outPane.getDividerLocation();
                if (m.isSelectionEmpty() || row < 0 || row >= data.size()) {
                    setLeft(new JLabel(" Nothing selected!"));
                    setRight(new JLabel(""));
                    removeButton.setEnabled(false);
                    removeAllButton.setEnabled(false);
                    editButton.setEnabled(false);
                    copyButton.setEnabled(false);
                } else {
                    if (!data.isEmpty()) {
                        ReqRespData reqRespData = data.get(row);
                        inputText.setText(reqRespData.getRequest().toUrlDecodedString(
                                jsonFormatBox.isSelected()));
                        outputText.setText(reqRespData.getResponse().toString(
                                jsonFormatBox.isSelected()));
                        inputText.setCaretPosition(0);
                        outputText.setCaretPosition(0);
                        setLeft(inputScroll);
                        setRight(outputScroll);
                        removeButton.setEnabled(true);
                        removeAllButton.setEnabled(true);
                        editButton.setEnabled(true);
                        copyButton.setEnabled(true);
                        saveButton.setEnabled(true);
                    }
                }
                outPane.setDividerLocation(divLoc);
            }
        });
        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BorderLayout());
        JScrollPane tableScrollPane = new JScrollPane();
        tableScrollPane.setViewportView(profileDataTable);
        tablePane.add(tableScrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final String removeSelected = "Remove Selected";
        buttons.add(removeButton = new JButton(removeSelected));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String removeAll = "Remove All";
        buttons.add(removeAllButton = new JButton(removeAll));
        buttons.add(Box.createRigidArea(new Dimension(15, 0)));

        final String newEntry = "New";
        buttons.add(newButton = new JButton(newEntry));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));

        final String editSelected = "Edit";
        buttons.add(editButton = new JButton(editSelected));
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));

        final String copySelected = "Copy";
        buttons.add(copyButton = new JButton(copySelected));
        buttons.add(Box.createRigidArea(new Dimension(10, 0)));

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
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (newEntry.equals(event.getActionCommand())) {
                    newEntry();
                }
            }
        });
        editButton.setEnabled(false);
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (editSelected.equals(event.getActionCommand())) {
                    editSelected();
                }
            }
        });
        copyButton.setEnabled(false);
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (copySelected.equals(event.getActionCommand())) {
                    copySelected();
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
        leftPanel.add(new JLabel(" No profile data selected!"));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(new JLabel("  " + "Response"));
        rightPanel.add(new JLabel(""));
        outPane = new JSplitPane(0, leftPanel, rightPanel);
        outPane.setDividerSize(4);
        pane2.add(outPane, BorderLayout.CENTER);

        inputText = new JTextAreaX(null, null, 20, 80);
        inputText.setEditable(false);
        inputScroll = new JScrollPane();
        inputScroll.setViewportView(inputText);
        outputText = new JTextAreaX(null, null, 20, 80);
        outputText.setEditable(false);
        outputScroll = new JScrollPane();
        outputScroll.setViewportView(outputText);

        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomButtons.add(jsonFormatBox = new JCheckBox("JSON Format"));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String saveProfile = "Save Profile";
        bottomButtons.add(saveButton = new JButton(saveProfile));
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
                int index = profileDataTable.getSelectionModel().getLeadSelectionIndex();
                if (index >= 0 && index < profile.getData().size()) {
                    ReqRespData reqRespData = profile.getData().get(index);
                    inputText.setText(reqRespData.getRequest().toUrlDecodedString(
                            jsonFormatBox.isSelected()));
                    outputText.setText(reqRespData.getResponse().toString(
                            jsonFormatBox.isSelected()));
                    inputText.setCaretPosition(0);
                    outputText.setCaretPosition(0);
                }
            }
        });

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (saveProfile.equals(event.getActionCommand())) {
                    save();
                }
            }
        });
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                saveButton.setEnabled(true);
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
        String name = "Profile: " + profile.getName();
        sel.setSelectionInterval(0, 0);
        outPane.setDividerLocation(150);
        notebook.addTab(name, this);
        notebook.setTabComponentAt(notebook.getTabCount() - 1, new ClosableTabComponent(notebook,
                name, closeActionListener));
    }

    public Profile getProfile() {
        return profile;
    }

    /**
     * Method setLeft
     * 
     * @param left
     */
    public void setLeft(Component left) {
        leftPanel.removeAll();
        leftPanel.add(left);
    }

    /**
     * Method setRight
     * 
     * @param right
     */
    public void setRight(Component right) {
        rightPanel.removeAll();
        rightPanel.add(right);
    }

    /**
     * Method close
     */
    public void close() {
        notebook.remove(this);
    }

    /**
     * Method remove
     */
    public void remove() {
        ListSelectionModel lsm = profileDataTable.getSelectionModel();
        int bot = lsm.getMinSelectionIndex();
        int top = lsm.getMaxSelectionIndex();

        for (int i = top; i >= bot; i--) {
            profile.removeData(i);
        }
        int size = profile.getData().size();
        if (bot > size) {
            bot = size;
        }
        tableModel.fireTableDataChanged();
        if (bot >= 0) {
            lsm.setSelectionInterval(bot, bot);
        }
    }

    /**
     * Method removeAll
     */
    public void removeAll() {
        ListSelectionModel lsm = profileDataTable.getSelectionModel();
        lsm.clearSelection();
        profile.removeAllData();
        tableModel.fireTableDataChanged();
    }

    protected void copySelected() {
        int row = profileDataTable.getSelectedRow();
        if (row != -1) {
            ReqRespDataEditor editor = new ReqRespDataEditor(Main.getFrame(), this,
                    ReqRespDataEditor.Action.COPY, "Copy Data Entry", profile.getData().get(row));
            editor.setVisible(true);
        }
    }

    protected void editSelected() {
        int row = profileDataTable.getSelectedRow();
        if (row != -1) {
            ReqRespDataEditor editor = new ReqRespDataEditor(Main.getFrame(), this,
                    ReqRespDataEditor.Action.EDIT, "Edit Data Entry", profile.getData().get(row));
            editor.setVisible(true);
        }
    }

    protected void newEntry() {
        ReqRespDataEditor editor = new ReqRespDataEditor(Main.getFrame(), this,
                ReqRespDataEditor.Action.NEW, "New Data Entry", null);
        editor.setVisible(true);
    }

    /**
     * Method save
     */
    public void save() {
        saveButton.setEnabled(false);
        profile.save();
        ((AbstractTableModel) Main.getFrame().getAdminPane().profileTable.getModel())
                .fireTableDataChanged();
    }

    static class ProfileDataTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Request URL";
                case 1:
                    return "Request Body";
            }

            return null;
        }

        private List<ReqRespData> data;

        public ProfileDataTableModel(List<ReqRespData> data) {
            if (data == null) {
                throw new IllegalArgumentException("data is null");
            }
            this.data = data;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ReqRespData p = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return p.getRequest().getMethodRequestUrl(true);
                case 1:
                    return p.getRequest().getDataString();
            }
            return null;
        }
    }

    public void editDone(ReqRespDataEditor editor, ReqRespDataEditor.Action action) {
        int toSelectIndex = -1;
        switch (action) {
            case NEW:
                if (profile.getDataMap().containsKey(editor.newData.getRequest())) {
                    // new data key already exist
                    int result = JOptionPane.showConfirmDialog(this,
                            "The new data's request already exists in this profile.",
                            "Confirm to override?", JOptionPane.OK_CANCEL_OPTION);

                    if (result == JOptionPane.OK_OPTION) {
                        toSelectIndex = profile.addData(editor.newData);
                        editor.setEnabled(false);
                    }
                } else {
                    toSelectIndex = profile.addData(editor.newData);
                    editor.setVisible(false);
                }
                break;
            case EDIT:
                if (!editor.originalData.getRequest().equals(editor.newData.getRequest())) {
                    // request changed, remove the old one
                    profile.removeData(editor.originalData);
                }
                toSelectIndex = profile.addData(editor.newData);
                editor.setVisible(false);
                break;
            case COPY:
                if (profile.getDataMap().containsKey(editor.newData.getRequest())) {
                    // new data key already exist
                    int result = JOptionPane
                            .showConfirmDialog(
                                    this,
                                    "The copied data's request doesn't changed, override the original one?",
                                    "Confirm to replace the original one?",
                                    JOptionPane.OK_CANCEL_OPTION);

                    if (result == JOptionPane.OK_OPTION) {
                        toSelectIndex = profile.addData(editor.newData);
                        editor.setVisible(false);
                    }
                } else {
                    toSelectIndex = profile.addData(editor.newData);
                    editor.setVisible(false);
                }
                break;
        }

        tableModel.fireTableDataChanged();
        if (toSelectIndex != -1) {
            profileDataTable.getSelectionModel().setSelectionInterval(toSelectIndex, toSelectIndex);
        }
    }
}

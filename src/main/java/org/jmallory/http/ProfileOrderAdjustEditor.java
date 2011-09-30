package org.jmallory.http;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.jmallory.AdminPane;

public class ProfileOrderAdjustEditor extends JDialog {
    private static final long serialVersionUID = 1L;

    JButton                   doneButton;
    JButton                   cancelButton;

    AdminPane                 adminPane;
    List<Profile>             profilesList;
    boolean                   httpsTargetProtool;

    public ProfileOrderAdjustEditor(Frame parent, AdminPane adminPane, Profile[] profiles,
                                    boolean httpsTargetProtool) {
        super(parent, "Change profile priorty for mock data", true);
        this.adminPane = adminPane;
        this.profilesList = Arrays.asList(profiles);
        this.httpsTargetProtool = httpsTargetProtool;

        this.getContentPane().setLayout(new BorderLayout());

        final JTable profileTable = new JTable(new ProfileTableModel(this.profilesList));
        profileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(profileTable);
        this.getContentPane().add(scrollPane, BorderLayout.CENTER);

        JPanel orderButtons = new JPanel(new GridLayout(2, 1));
        //        orderButtons.setLayout(new BoxLayout(orderButtons, BoxLayout.Y_AXIS));
        JButton upButton = new JButton("Up");
        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = profileTable.getSelectedRow();
                if (index == -1 || index == 0) {
                    // no selection or already the top one
                    return;
                }
                int toIndex = index - 1;
                swap(index, toIndex);
                ((AbstractTableModel) profileTable.getModel()).fireTableDataChanged();
                profileTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
            }
        });
        JButton downButton = new JButton("Down");
        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = profileTable.getSelectedRow();
                if (index == -1 || index == profilesList.size() - 1) {
                    // no selection or already the top one
                    return;
                }
                int toIndex = index + 1;
                swap(index, toIndex);
                ((AbstractTableModel) profileTable.getModel()).fireTableDataChanged();
                profileTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
            }
        });
        orderButtons.add(upButton);
        orderButtons.add(downButton);
        this.getContentPane().add(orderButtons, BorderLayout.EAST);

        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final String done = "Done";
        bottomButtons.add(doneButton = new JButton(done));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String cancel = "Cancel";
        bottomButtons.add(cancelButton = new JButton(cancel));
        this.getContentPane().add(bottomButtons, BorderLayout.SOUTH);

        doneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (done.equals(event.getActionCommand())) {
                    done();
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (cancel.equals(event.getActionCommand())) {
                    cancel();
                }
            }
        });

        this.pack();
        this.setSize(400, 300);

        Rectangle rec = parent.getBounds();
        int x = rec.x + rec.width / 2 - this.getWidth() / 2;
        int y = rec.y + rec.height / 2 - this.getHeight() / 2;
        this.setBounds(x, y, this.getWidth(), this.getHeight());
    }

    protected void done() {
        setVisible(false);
        adminPane.addListenerWithProfiles(profilesList.toArray(new Profile[profilesList.size()]),
                httpsTargetProtool);
    }

    protected void cancel() {
        this.setVisible(false);
    }

    protected void swap(int a, int b) {
        if (a < 0 || a >= profilesList.size() || b < 0 || b >= profilesList.size()) {
            return;
        }
        Profile current = profilesList.get(a);
        profilesList.set(a, profilesList.get(b));
        profilesList.set(b, current);
    }

    public static void main(String[] args) {
        //        new ReqRespDataEditor(null, null, "test", "test", null).setVisible(true);
    }
}

package org.jmallory.swing;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class Scrapbook extends JPanel {
    private static final long serialVersionUID = 1L;

    JTabbedPane               notebook         = null;
    JTextAreaX                content          = new JTextAreaX();
    String                    filename         = "";
    private File              file             = null;
    private JFileChooser      filechooser      = new JFileChooser();

    public Scrapbook(Window parent, JTabbedPane _notebook) {
        notebook = _notebook;
        this.setLayout(new BorderLayout());

        JPanel quickMenuBar = new JPanel();
        quickMenuBar.setLayout(new BoxLayout(quickMenuBar, BoxLayout.X_AXIS));
        JButton open = new JButton("Open");
        open.addActionListener(new openMenuItem_actionAdapter(this));
        quickMenuBar.add(open);

        JButton save = new JButton("Save");
        save.addActionListener(new saveMenuItem_actionAdapter(this));
        quickMenuBar.add(save);

        JButton saveAs = new JButton("Save As");
        saveAs.addActionListener(new saveAsMenuItem_actionAdapter(this));
        quickMenuBar.add(saveAs);

        this.add(quickMenuBar, BorderLayout.NORTH);

        content.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(parent, content,
                null, true));

        JScrollPane textPanel = new JScrollPane(content);

        this.add(textPanel, BorderLayout.CENTER);
    }

    public void openMenuItemActionPerformed(ActionEvent evt) {
        try {
            file = null;
            int returnVal = filechooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file = filechooser.getSelectedFile();
                filename = file.getName();
                FileReader fr = new FileReader(file);
                int len = (int) file.length();
                char[] buffer = new char[len];
                fr.read(buffer, 0, len);
                fr.close();
                content.setText(new String(buffer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAsMenuItemActionPerformed(ActionEvent evt) {
        filechooser.setDialogTitle("另存为...");
        int returnVal = filechooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = filechooser.getSelectedFile();
            filename = file.getName();
            try {
                FileWriter fw = new FileWriter(file);
                fw.write(content.getText());
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    class openMenuItem_actionAdapter implements ActionListener {
        Scrapbook adaptee;

        openMenuItem_actionAdapter(Scrapbook adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent evt) {
            adaptee.openMenuItemActionPerformed(evt);
        }
    }

    class saveAsMenuItem_actionAdapter implements ActionListener {
        Scrapbook adaptee;

        saveAsMenuItem_actionAdapter(Scrapbook adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent evt) {
            adaptee.saveAsMenuItemActionPerformed(evt);
        }
    }

    class saveMenuItem_actionAdapter implements ActionListener {
        Scrapbook adaptee;

        saveMenuItem_actionAdapter(Scrapbook adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent evt) {
            adaptee.saveMenuItemActionPerformed(evt);
        }
    }

    public void saveMenuItemActionPerformed(ActionEvent evt) {
        if (filename.isEmpty()) {
            int returnVal = filechooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file = filechooser.getSelectedFile();
                filename = file.getName();
                try {
                    FileWriter fw = new FileWriter(file);
                    fw.write(content.getText());
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            file = filechooser.getSelectedFile();
            filename = file.getName();
            try {
                FileWriter fw = new FileWriter(file);
                fw.write(content.getText());
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

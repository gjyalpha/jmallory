package org.jmallory.http;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jmallory.io.LineInputStream;
import org.jmallory.swing.EncoderDecoderTextEditor;
import org.jmallory.swing.JTextAreaX;
import org.jmallory.util.Utils;


public class ReqDataEditor extends JDialog {

    private static final long serialVersionUID = -7822608508681510475L;

    public static enum Action {
        RESEND;
    }

    JPanel       leftPanel;
    JTextArea    inputText;
    JScrollPane  inputScroll;

    JButton      doneButton;
    JButton      cancelButton;

    HttpRequest  originalData;
    HttpRequest  newData;

    HttpListener listener;
    Action       action;

    public ReqDataEditor(Frame parent, HttpListener listener, Action action, String title,
                             HttpRequest httpRequest) {
        super(parent, title + " (" + EncoderDecoderTextEditor.DEFAULT_TITLE + ")", true);
        this.listener = listener;

        this.action = action;
        this.originalData = httpRequest;

        this.getContentPane().setLayout(new BorderLayout());

        JPanel pane2 = new JPanel();
        pane2.setLayout(new BorderLayout());
        leftPanel = new JPanel();
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(new JLabel("  Request"));
        pane2.add(leftPanel, BorderLayout.CENTER);

        inputText = new JTextAreaX(null, null, 20, 80);
        inputText.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(this, inputText));
        inputText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!(e.getSource() instanceof JTextArea)) {
                    return;
                }
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    onCancel();
                }
                if (e.isControlDown() && keyCode == KeyEvent.VK_ENTER) {
                    onDone();
                }
            }
        });
        inputScroll = new JScrollPane();
        inputScroll.setViewportView(inputText);
        leftPanel.add(inputScroll);

        if (originalData != null) {
            inputText.setText(originalData.toString());
            inputText.setCaretPosition(0);
        }

        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final String done = "Done";
        bottomButtons.add(doneButton = new JButton(done));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String cancel = "Cancel";
        bottomButtons.add(cancelButton = new JButton(cancel));
        pane2.add(bottomButtons, BorderLayout.SOUTH);

        doneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onDone();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onCancel();
            }
        });

        this.getContentPane().add(pane2, BorderLayout.CENTER);
        this.pack();
        this.setSize(600, 400);

        Rectangle rec = parent.getBounds();
        int x = rec.x + rec.width / 2 - this.getWidth() / 2;
        int y = rec.y + rec.height / 2 - this.getHeight() / 2;
        this.setBounds(x, y, this.getWidth(), this.getHeight());
    }

    public void onDone() {
        // parse and formalize
        HttpRequest request = null;
        try {
            LineInputStream requestReader = new LineInputStream(new ByteArrayInputStream(inputText
                    .getText().getBytes(Utils.CHARSET)), false);
            request = Utils.readRequest(requestReader);
            request.normalizeContentLength();

            newData = request;

            listener.requestEditDone(action, newData);
            setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to parse request or response, error: "
                    + e.getMessage());
            return;
        }
    }

    public void onCancel() {
        this.setVisible(false);
    }
}

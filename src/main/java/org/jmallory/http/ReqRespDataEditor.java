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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.jmallory.io.LineInputStream;
import org.jmallory.swing.EncoderDecoderTextEditor;
import org.jmallory.swing.JTextAreaX;
import org.jmallory.util.Utils;


public class ReqRespDataEditor extends JDialog {

    private static final long serialVersionUID = 5351567399760196642L;

    public static enum Action {
        NEW,
        EDIT,
        COPY;
    }

    JSplitPane    outPane;
    JPanel        leftPanel;
    JPanel        rightPanel;
    JTextArea     inputText;
    JScrollPane   inputScroll;
    JTextArea     outputText;
    JScrollPane   outputScroll;

    JButton       doneButton;
    JButton       switchButton;
    JButton       cancelButton;

    ReqRespData   originalData;
    ReqRespData   newData;

    Action        action = null;
    boolean       isDone = false;

    ProfileEditor profileEditor;

    public ReqRespDataEditor(Frame parent, ProfileEditor profileEditor, Action action,
                             String title, ReqRespData originalData) {
        super(parent, title + " (" + EncoderDecoderTextEditor.DEFAULT_TITLE + ")", true);
        this.profileEditor = profileEditor;
        this.action = action;
        this.originalData = originalData;

        this.getContentPane().setLayout(new BorderLayout());

        JPanel pane2 = new JPanel();
        pane2.setLayout(new BorderLayout());
        leftPanel = new JPanel();
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(new JLabel("  " + "Request"));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(new JLabel("  " + "Response"));
        outPane = new JSplitPane(0, leftPanel, rightPanel);
        outPane.setDividerSize(4);
        outPane.setDividerLocation(0.5);
        pane2.add(outPane, BorderLayout.CENTER);

        KeyAdapter doneCancelKey = new KeyAdapter() {
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
        };

        inputText = new JTextAreaX(null, null, 20, 80);
        inputText.addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(this, inputText));
        inputText.addKeyListener(doneCancelKey);
        inputScroll = new JScrollPane();
        inputScroll.setViewportView(inputText);
        leftPanel.add(inputScroll);

        outputText = new JTextAreaX(null, null, 20, 80);
        outputText
                .addKeyListener(new EncoderDecoderTextEditor.ShortcutKeysAdapter(this, outputText));
        outputText.addKeyListener(doneCancelKey);
        outputScroll = new JScrollPane();
        outputScroll.setViewportView(outputText);
        rightPanel.add(outputScroll);

        if (originalData != null) {
            inputText.setText(originalData.getRequest().toString());
            inputText.setCaretPosition(0);
            outputText.setText(originalData.getResponse().toString());
            outputText.setCaretPosition(0);
        }

        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final String done = "Done";
        bottomButtons.add(doneButton = new JButton(done));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String cancel = "Cancel";
        bottomButtons.add(cancelButton = new JButton(cancel));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String switchStr = "Switch Layout";
        bottomButtons.add(switchButton = new JButton(switchStr));
        bottomButtons.add(Box.createHorizontalGlue());
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

        this.getContentPane().add(pane2, BorderLayout.CENTER);
        outPane.setDividerLocation(250);
        this.pack();
        this.setSize(800, 600);

        Rectangle rec = parent.getBounds();
        int x = rec.x + rec.width / 2 - this.getWidth() / 2;
        int y = rec.y + rec.height / 2 - this.getHeight() / 2;
        this.setBounds(x, y, this.getWidth(), this.getHeight());
    }

    public void onDone() {
        HttpRequest request = null;
        HttpResponse response = null;
        try {
            LineInputStream requestReader = new LineInputStream(new ByteArrayInputStream(inputText
                    .getText().getBytes(Utils.CHARSET)), false);
            request = Utils.readRequest(requestReader);
            request.normalizeContentLength();

            LineInputStream responseReader = new LineInputStream(new ByteArrayInputStream(
                    outputText.getText().getBytes(Utils.CHARSET)), false);
            response = Utils.readResponse(responseReader);
            response.normalizeContentLength();

            newData = new ReqRespData(request, response);

            profileEditor.editDone(this, action);
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

    public static void main(String[] args) {
        //        new ReqRespDataEditor(null, null, "test", "test", null).setVisible(true);
    }
}

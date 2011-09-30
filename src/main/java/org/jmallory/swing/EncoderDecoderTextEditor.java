package org.jmallory.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.lang.StringEscapeUtils;
import org.jmallory.util.TextFormatter;
import org.jmallory.util.Utils;

/**
 * Editor which support to edit UrlEncoder, literal string and/or JSON string in
 * a formated way.
 * 
 * @author gerry
 */
public class EncoderDecoderTextEditor extends JDialog {

    private static final long  serialVersionUID = -6929177293390355966L;

    public static final String DEFAULT_TITLE    = "Smart Editor (select text and right click!)";
    public static final String CRLF             = "\r\n";
    public static final String LF               = "\n";

    //    public static final Font DEFAULT_FONT = new Font("Monospace", Font.PLAIN, 10); 

    public static enum CodecMode {
        URL_ENCODE_PARAMETERS {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    StringBuilder sb = new StringBuilder();
                    String[] parameters = str.split("&");
                    for (String parameter : parameters) {
                        String[] parts = parameter.split("=");
                        for (String part : parts) {
                            sb.append(URLDecoder.decode(part, Utils.CHARSET)).append("=");
                        }
                        if (parts.length > 1) {
                            // if we have appended =, remove the last one 
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        sb.append(LF);
                    }
                    return sb.toString();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                str = str.trim();
                try {
                    StringBuilder sb = new StringBuilder();
                    String[] parameters = str.split("(\r)?\n");
                    boolean first = true;
                    for (String parameter : parameters) {
                        parameter = parameter.trim();
                        if (!parameter.isEmpty()) {
                            if (first) {
                                first = false;
                            } else {
                                sb.append("&");
                            }
                            String[] parts = parameter.split("=", 2);
                            sb.append(URLEncoder.encode(parts[0].trim(), Utils.CHARSET)
                                    .replaceAll("\\+", "%20"));
                            if (parts.length == 2) {
                                sb.append("=").append(
                                        URLEncoder.encode(parts[1].trim(), Utils.CHARSET));
                            }
                        }
                    }
                    return sb.toString();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }
        },
        URL_ENCODE_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return URLDecoder.decode(str, Utils.CHARSET);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return URLEncoder.encode(str, Utils.CHARSET).replaceAll("\\+", "%20");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }
        },

        JSON_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.formatText(str);
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                net.sf.json.JSONObject.fromObject(str);
                // check if it is really a json
                return TextFormatter.unifyText(str);
            }
        },
        URL_ENCODE_JSON_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return TextFormatter.formatText(URLDecoder.decode(str, Utils.CHARSET));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                JSONObject.fromObject(str); // check if it is really a json
                return URLEncoder.encode(TextFormatter.unifyText(str), Utils.CHARSET);
            }
        },
        STRING_LITERAL_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.deliteralizeString(str);
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.literalizeString(str);
            }
        },
        GROOVY_STRING_LITERAL_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.deliteralizeGroovy(str);
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.literalizeGroovy(str);
            }
        },
        STRING_LITERAL_JSON_VALUE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return TextFormatter.formatText(TextFormatter.deliteralizeJson(str));
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                JSONObject.fromObject(str); // check if it is really a json
                return TextFormatter.literalizeJson(TextFormatter.unifyText(str));
            }
        },
        BASE64 {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return new String(Base64.decodeBase64(str.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return new String(Base64.encodeBase64(str.getBytes()));
            }
        },
        QUOTED_PRINTABLE {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return new String(QuotedPrintableCodec.decodeQuotedPrintable(str.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return new String(QuotedPrintableCodec.encodeQuotedPrintable(null, str.getBytes()));
            }
        },
        HTML_ENTITIES {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                try {
                    return StringEscapeUtils.unescapeHtml(str);
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return StringEscapeUtils.escapeHtml(str);
            }
        },
        TRIVAL {
            public String decode(String str) {
                if (str == null || str.isEmpty()) {
                    return "";
                } else {
                    return str;
                }
            }

            public String encode(String str) throws Exception {
                if (str == null || str.isEmpty()) {
                    return "";
                }
                return str;
            }
        };

        public String decode(String str) {
            return str;
        }

        public String encode(String str) throws Exception {
            return str;
        }
    }

    public static class ShortcutKeysAdapter extends KeyAdapter {
        protected Window          parent;
        protected JTextArea       textArea;
        protected EditingCallback callback;

        protected Action          selectLineAction;
        protected boolean         viewonly;

        public ShortcutKeysAdapter(Window parent, JTextArea textArea) {
            this(parent, textArea, null);
        }

        public ShortcutKeysAdapter(Window parent, JTextArea textArea, EditingCallback callback) {
            this(parent, textArea, callback, false);
        }

        public ShortcutKeysAdapter(Window parent, JTextArea textArea, EditingCallback callback,
                                   boolean viewonly) {
            this.parent = parent;
            this.textArea = textArea;
            this.callback = callback;

            this.selectLineAction = getAction(DefaultEditorKit.selectLineAction);
            this.viewonly = viewonly;

            addPopupMenu();
        }

        private void addPopupMenu() {

            JPopupMenu menu = new JPopupMenu();

            JMenuItem item = new JMenuItem(new DefaultEditorKit.CutAction());
            item.setText("Cut");
            item.setAccelerator(KeyStroke.getKeyStroke("ctrl X"));
            menu.add(item);
            item = new JMenuItem(new DefaultEditorKit.CopyAction());
            item.setText("Copy");
            item.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
            menu.add(item);
            item = new JMenuItem(new DefaultEditorKit.PasteAction());
            item.setText("Paste");
            item.setAccelerator(KeyStroke.getKeyStroke("ctrl V"));
            menu.add(item);

            item = new JMenuItem(new TextCountAction("Count Selected",
                    "Count the characters in the selected text"));
            item.setAccelerator(KeyStroke.getKeyStroke("ctrl U"));
            menu.add(item);

            menu.addSeparator();

            menu.add(createJMenuItem("String", "ctrl T", CodecMode.TRIVAL));
            menu.add(createJMenuItem("String literal", "ctrl L", CodecMode.STRING_LITERAL_VALUE));
            menu.add(createJMenuItem("BASE64", "ctrl B", CodecMode.BASE64));
            menu.add(createJMenuItem("Quoted Printable", "ctrl Q", CodecMode.QUOTED_PRINTABLE));
            menu.add(createJMenuItem("HTML Entities", "ctrl H", CodecMode.HTML_ENTITIES));
            menu.add(createJMenuItem("Groovy string literal", "ctrl G",
                    CodecMode.GROOVY_STRING_LITERAL_VALUE));
            menu.add(createJMenuItem("JSON", "ctrl S", CodecMode.JSON_VALUE));
            menu.add(createJMenuItem("String literal JSON", "ctrl alt S",
                    CodecMode.STRING_LITERAL_JSON_VALUE));
            menu.add(createJMenuItem("x-www-form-urlencoded", "ctrl shift E",
                    CodecMode.URL_ENCODE_PARAMETERS));
            menu.add(createJMenuItem("x-www-form-urlencoded value", "ctrl E",
                    CodecMode.URL_ENCODE_VALUE));
            menu.add(createJMenuItem("x-www-form-urlencoded JSON value", "ctrl shift S",
                    CodecMode.URL_ENCODE_JSON_VALUE));

            textArea.add(menu);

            textArea.addMouseListener(new PopupTriggerMouseListener(menu, textArea));
        }

        private JMenuItem createJMenuItem(String name, String shortcut, CodecMode mode) {
            JMenuItem item = new JMenuItem(new TextAreaAction(name, shortcut, parent, mode));
            item.setAccelerator(KeyStroke.getKeyStroke(shortcut));
            return item;
        }

        public class PopupTriggerMouseListener extends MouseAdapter {
            private JPopupMenu popup;
            private JComponent component;

            public PopupTriggerMouseListener(JPopupMenu popup, JComponent component) {
                this.popup = popup;
                this.component = component;
            }

            //some systems trigger popup on mouse press, others on mouse release, we want to cater for both
            private void showMenuIfPopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(component, e.getX() + 3, e.getY() + 3);
                }
            }

            public void mousePressed(MouseEvent e) {
                showMenuIfPopupTrigger(e);
            }

            public void mouseReleased(MouseEvent e) {
                showMenuIfPopupTrigger(e);
            }

        }

        public class TextAreaAction extends AbstractAction {
            private static final long serialVersionUID = 1L;

            CodecMode                 mode;

            public TextAreaAction(String name, String description, Window parent, CodecMode mode) {
                super(name);
                this.putValue(SHORT_DESCRIPTION, description);
                this.mode = mode;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (ShortcutKeysAdapter.this.parent != null) {
                    Dimension d = ShortcutKeysAdapter.this.parent.getSize();
                    new EncoderDecoderTextEditor(parent, textArea, null, mode, viewonly,
                            (int) d.getWidth(), (int) d.getHeight(), callback).setVisible(true);
                } else {
                    new EncoderDecoderTextEditor(parent, textArea, mode, viewonly, callback)
                            .setVisible(true);
                }
            }
        }

        public class TextCountAction extends AbstractAction {
            private static final long serialVersionUID = 1L;

            public TextCountAction(String name, String description) {
                super(name);
                this.putValue(SHORT_DESCRIPTION, description);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(textArea, "<html>There are <font color='red'><b>"
                        + Math.abs(textArea.getSelectionEnd() - textArea.getSelectionStart())
                        + "</b></font> characters in selected text.</html>");
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (!(e.getSource() instanceof JTextArea)) {
                return;
            }
            int keyCode = e.getKeyCode();
            if (e.isControlDown()) {
                CodecMode newMode = null;
                switch (keyCode) {
                    case KeyEvent.VK_O:
                        if (selectLineAction != null) {
                            selectLineAction.actionPerformed(null);
                        }
                        break;
                    case KeyEvent.VK_D: {
                        JTextArea textArea = (JTextArea) e.getSource();
                        try {
                            int line = textArea.getLineOfOffset(textArea.getCaretPosition());
                            int start = textArea.getLineStartOffset(line);
                            int end = textArea.getLineEndOffset(line);
                            if (line + 1 == textArea.getLineCount()) {
                                // last line
                                if (start != 0) {
                                    if ("\n".equals(textArea.getDocument().getText(start - 1, 1))) {
                                        start--;
                                    }
                                    if ("\r".equals(textArea.getDocument().getText(start - 2, 1))) {
                                        start--;
                                    }
                                }
                            } else {
                                // we have a following line
                                end = textArea.getLineStartOffset(line + 1);
                            }
                            textArea.getDocument().remove(start, end - start);
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                        break;
                }
                if (newMode != null) {
                    // call url encode editor
                    new EncoderDecoderTextEditor(parent, (JTextArea) e.getSource(), newMode,
                            viewonly, callback).setVisible(true);
                }
            }
        }

        protected Action getAction(String name) {
            Action action = null;
            Action[] actions = textArea.getActions();

            for (int i = 0; i < actions.length; i++) {
                if (name.equals(actions[i].getValue(Action.NAME).toString())) {
                    action = actions[i];
                    break;
                }
            }

            return action;
        }

    };

    protected JTextArea       text;
    protected JScrollPane     scroll;

    protected JButton         doneButton;
    protected JButton         cancelButton;

    protected JTextArea       inputText;
    protected String          inputString;

    protected CodecMode       mode;
    protected boolean         viewonly;
    protected EditingCallback callback;

    public EncoderDecoderTextEditor(Window parent, String inputString, CodecMode mode,
                                    boolean viewonly, EditingCallback callback) {
        this(parent, null, inputString, mode, viewonly, 500, 400, callback);
    }

    public EncoderDecoderTextEditor(Window parent, JTextArea inputText, CodecMode mode,
                                    boolean viewonly, EditingCallback callback) {
        this(parent, inputText, null, mode, viewonly, 500, 400, callback);
    }

    public EncoderDecoderTextEditor(Window parent, JTextArea inText, String inString,
                                    CodecMode mode, boolean viewonly, int width, int height,
                                    EditingCallback callback) {
        super(parent, DEFAULT_TITLE, ModalityType.APPLICATION_MODAL);
        this.inputText = inText;
        this.inputString = inString;
        this.getContentPane().setLayout(new BorderLayout());
        this.mode = mode;
        this.viewonly = viewonly;
        this.callback = callback;

        text = new JTextAreaX();
        text.setTabSize(4);
        text.registerKeyboardAction(new AutoIndentAction(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        text.addKeyListener(new ShortcutKeysAdapter(this, text, null, viewonly));
        text.addKeyListener(new KeyAdapter() {
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

        scroll = new JScrollPane();
        scroll.setViewportView(text);

        this.getContentPane().add(scroll, BorderLayout.CENTER);

        text.setText(decode(inputText != null ? inputText.getSelectedText()
                : (inputString != null ? inputString : "")));
        text.setCaretPosition(0);

        JPanel bottomButtons = new JPanel();
        bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
        bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final String done = "Done";
        bottomButtons.add(doneButton = new JButton(done));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        final String cancel = "Cancel";
        bottomButtons.add(cancelButton = new JButton(cancel));
        bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));

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

        this.getContentPane().add(bottomButtons, BorderLayout.SOUTH);
        this.pack();
        this.setSize(width, height);

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

    protected String decode(String str) {
        if (str != null) {
            // since JTextArea can't handle CR correclty, we replayce all CRLF with LF
            str = str.replaceAll(CRLF, LF);
        }
        return mode.decode(str);
    }

    protected String encode(String str) throws Exception {
        return mode.encode(str);
    }

    public void onDone() {
        try {
            String encodedText = encode(text.getText());

            if (!viewonly && inputText != null) {
                inputText.replaceSelection(encodedText);
            }
            this.setVisible(false);
            if (callback != null) {
                callback.done(encodedText);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to encode this string into " + mode
                    + ", due to " + e.getMessage());
        }
    }

    public void onCancel() {
        this.setVisible(false);
        if (callback != null) {
            callback.cancel();
        }
    }

    public static class AutoIndentAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent ae) {
            JTextArea comp = (JTextArea) ae.getSource();
            Document doc = comp.getDocument();

            if (!comp.isEditable())
                return;
            try {

                int position = comp.getCaretPosition();
                int line = comp.getLineOfOffset(position);

                int start = comp.getLineStartOffset(line);

                if (start == position) {
                    doc.insertString(position, "\n", null);
                } else {

                    int end = comp.getLineEndOffset(line);
                    String str = doc.getText(start, end - start - 1);
                    String whiteSpace = getLeadingWhiteSpace(str);
                    doc.insertString(position, '\n' + whiteSpace, null);
                }
            } catch (BadLocationException ex) {
                try {
                    doc.insertString(comp.getCaretPosition(), "\n", null);
                } catch (BadLocationException ignore) {
                    // ignore 
                }
            }
        }

        /**
         * Returns leading white space characters in the specified string.
         */
        private String getLeadingWhiteSpace(String str) {
            return str.substring(0, getLeadingWhiteSpaceWidth(str));
        }

        /**
         * Returns the number of leading white space characters in the specified
         * string.
         */
        private int getLeadingWhiteSpaceWidth(String str) {
            int whitespace = 0;
            while (whitespace < str.length()) {
                char ch = str.charAt(whitespace);
                if (ch == ' ' || ch == '\t')
                    whitespace++;
                else
                    break;
            }
            return whitespace;
        }
    }

    public static void main(String[] args) {
        EncoderDecoderTextEditor editor = new EncoderDecoderTextEditor(null, null, null,
                CodecMode.TRIVAL, false, 800, 600, null) {
            private static final long serialVersionUID = 1L;

            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (!visible) {
                    System.exit(0);
                }
            }
        };

        editor.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        editor.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
        editor.setVisible(true);
    }
}

package org.jmallory.swing;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class JTextAreaX extends JTextArea {

    private static final long serialVersionUID = 1L;

    protected UndoManager     undo;

    protected int             limitCharacters  = -1;

    public JTextAreaX(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
        setupUndoManager();
    }

    public JTextAreaX(Document doc) {
        super(doc);
        setupUndoManager();
    }

    public JTextAreaX(int rows, int columns) {
        super(rows, columns);
        setupUndoManager();
    }

    public JTextAreaX(String text, int rows, int columns) {
        super(text, rows, columns);
        setupUndoManager();
    }

    public JTextAreaX(String text) {
        super(text);
        setupUndoManager();
    }

    public JTextAreaX() {
        super();
        setupUndoManager();
    }

    private void setupUndoManager() {

        undo = new UndoManager();
        this.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undo.addEdit(e.getEdit());
            }
        });

        this.getActionMap().put("Undo", new AbstractAction("Undo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }
        });
        // Bind the undo action to ctl-Z
        this.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

        // Create a redo action and add it to the text component
        this.getActionMap().put("Redo", new AbstractAction("Redo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });
        // Bind the redo action to ctl-Y 
        this.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
    }

    public int getLimitCharacters() {
        return limitCharacters;
    }

    public void setLimitCharacters(int limitCharacters) {
        this.limitCharacters = limitCharacters;
    }

    @Override
    public void append(String str) {
        if (str == null || str.isEmpty()) {
            return;
        }
        Document doc = getDocument();
        if (doc != null) {
            try {
                if (limitCharacters > 0) {
                    // we need to limit/rotate the content
                    int total = doc.getLength() + str.length();
                    int delta = total - limitCharacters;
                    if (delta > 0) {
                        doc.remove(0, delta);
                    }
                }
                doc.insertString(doc.getLength(), str, null);
            } catch (BadLocationException e) {
            }
        }
    }
}

package org.jmallory.swing;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * a text field with a restricted set of characters
 */
public class RestrictedTextField extends JTextField {
    private static final long serialVersionUID = 1L;
    /**
     * Field validText
     */
    protected String          validText;

    /**
     * Constructor RestrictedTextField
     * 
     * @param validText
     */
    public RestrictedTextField(String validText) {
        setValidText(validText);
    }

    /**
     * Constructor RestrictedTextField
     * 
     * @param columns
     * @param validText
     */
    public RestrictedTextField(int columns, String validText) {
        super(columns);
        setValidText(validText);
    }

    /**
     * Constructor RestrictedTextField
     * 
     * @param text
     * @param validText
     */
    public RestrictedTextField(String text, String validText) {
        super(text);
        setValidText(validText);
    }

    /**
     * Constructor RestrictedTextField
     * 
     * @param text
     * @param columns
     * @param validText
     */
    public RestrictedTextField(String text, int columns, String validText) {
        super(text, columns);
        setValidText(validText);
    }

    /**
     * Method setValidText
     * 
     * @param validText
     */
    private void setValidText(String validText) {
        this.validText = validText;
    }

    /**
     * fascinatingly, this method is called in the super() constructor,
     * meaning before we are fully initialized. C++ doesnt actually permit
     * such a situation, but java clearly does...
     * 
     * @return a new document
     */
    public Document createDefaultModel() {
        return new RestrictedDocument();
    }

    /**
     * this class strips out invaid chars
     */
    class RestrictedDocument extends PlainDocument {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a plain text document. A default model using
         * <code>GapContent</code> is constructed and set.
         */
        public RestrictedDocument() {
        }

        /**
         * add a string; only those chars in the valid text list are allowed
         * 
         * @param offset
         * @param string
         * @param attributes
         * @throws BadLocationException
         */
        public void insertString(int offset, String string, AttributeSet attributes)
                throws BadLocationException {
            if (string == null) {
                return;
            }
            int len = string.length();
            StringBuffer buffer = new StringBuffer(string.length());
            for (int i = 0; i < len; i++) {
                char ch = string.charAt(i);
                if (validText.indexOf(ch) >= 0) {
                    buffer.append(ch);
                }
            }
            super.insertString(offset, new String(buffer), attributes);
        }
    } // end class NumericDocument
}
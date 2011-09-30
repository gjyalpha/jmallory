package org.jmallory.swing;

public class NumberField extends RestrictedTextField {
    private static final long   serialVersionUID = 1L;
    /**
     * Field VALID_TEXT
     */
    private static final String VALID_TEXT       = "0123456789";

    /**
     * Constructs a new <code>TextField</code>. A default model is created, the
     * initial string is <code>null</code>, and the number of columns is set to
     * 0.
     */
    public NumberField() {
        super(VALID_TEXT);
    }

    /**
     * Constructs a new empty <code>TextField</code> with the specified number
     * of columns. A default model is created and the initial string is set to
     * <code>null</code>.
     * 
     * @param columns the number of columns to use to calculate the preferred
     *            width; if columns is set to zero, the preferred width will be
     *            whatever naturally results from the component implementation
     */
    public NumberField(int columns) {
        super(columns, VALID_TEXT);
    }

    /**
     * get the int value of a field, any invalid (non int) field returns the
     * default
     * 
     * @param def default value
     * @return the field contents
     */
    public int getValue(int def) {
        int result = def;
        String text = getText();
        if ((text != null) && (text.length() != 0)) {
            try {
                result = Integer.parseInt(text);
            } catch (NumberFormatException e) {
            }
        }
        return result;
    }

    /**
     * set the text to a numeric value
     * 
     * @param value number to assign
     */
    public void setValue(int value) {
        setText(Integer.toString(value));
    }
}

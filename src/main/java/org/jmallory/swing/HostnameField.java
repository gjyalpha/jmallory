package org.jmallory.swing;

public class HostnameField extends RestrictedTextField {
    private static final long   serialVersionUID = 1L;
    /**
     * Field VALID_TEXT
     */
    private static final String VALID_TEXT       = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZYZ-.";

    /**
     * Constructor HostnameField
     * 
     * @param columns
     */
    public HostnameField(int columns) {
        super(columns, VALID_TEXT);
    }

    /**
     * Constructor HostnameField
     */
    public HostnameField() {
        super(VALID_TEXT);
    }
}

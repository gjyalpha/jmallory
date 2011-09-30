package org.jmallory.swing;

/**
 * When using EncoderDecoderTextEditor, if you want to do more thing with the
 * final encoded text besides replacing the original selection text, then you
 * should implement this interface and pass it to editor. NOTE: even with this
 * callback, the selected text in original JTextArea will still be replaced on
 * editing done.
 * 
 * @author gerry
 */
public interface EditingCallback {

    /**
     * The callback for editing done.
     * 
     * @param text the edited text
     */
    public void done(String text);

    /**
     * The callback for editing cancelled.
     */
    public void cancel();
}

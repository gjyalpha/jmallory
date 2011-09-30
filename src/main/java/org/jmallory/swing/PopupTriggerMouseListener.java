package org.jmallory.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

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

    //according to the javadocs on isPopupTrigger, checking for popup trigger on mousePressed and mouseReleased
    //should be all  that is required
    //public void mouseClicked(MouseEvent e) 
    //{
    //    showMenuIfPopupTrigger(e);
    //}

    public void mousePressed(MouseEvent e) {
        showMenuIfPopupTrigger(e);
    }

    public void mouseReleased(MouseEvent e) {
        showMenuIfPopupTrigger(e);
    }

}

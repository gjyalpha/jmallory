package org.jmallory.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

public class ClosableTabComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JTabbedPane pane;
    private final JLabel      label;

    public ClosableTabComponent(final JTabbedPane pane, String title,
                                ActionListener closeActionListener) {
        //unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }

        this.pane = pane;
        setOpaque(false);

        //make JLabel read titles from JTabbedPane
        label = new JLabel(title);

        add(label);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        //tab button
        JButton button = new TabButton();
        button.addActionListener(closeActionListener);
        add(button);
        //add more space to the top of the component
    }

    public void setActive(boolean active) {
        if (active) {
            label.setForeground(Color.BLACK);
        } else {
            label.setForeground(Color.GRAY);
        }
    }

    private class TabButton extends JButton {
        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
        }

        //we don't want to update UI for this button
        public void updateUI() {
        }

        //paint the cross
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
                                                               public void mouseEntered(MouseEvent e) {
                                                                   Component component = e
                                                                           .getComponent();
                                                                   if (component instanceof AbstractButton) {
                                                                       AbstractButton button = (AbstractButton) component;
                                                                       button
                                                                               .setBorderPainted(true);
                                                                   }
                                                               }

                                                               public void mouseExited(MouseEvent e) {
                                                                   Component component = e
                                                                           .getComponent();
                                                                   if (component instanceof AbstractButton) {
                                                                       AbstractButton button = (AbstractButton) component;
                                                                       button
                                                                               .setBorderPainted(false);
                                                                   }
                                                               }
                                                           };
}

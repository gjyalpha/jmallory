package org.jmallory;

import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Proxy that sniffs and shows traffics.
 */

public class Main extends JFrame {

    private static final long  serialVersionUID    = 1L;

    private static Main        frame;

    public static final Font   MONOSPACED          = new Font("Monospaced", Font.PLAIN, 12);

    private JTabbedPane        notebook            = null;

    protected AdminPane        adminPane           = null;

    /**
     * Field STATE_COLUMN
     */
    public static final int    STATE_COLUMN        = 0;

    /**
     * Field OUTHOST_COLUMN
     */
    public static final int    OUTHOST_COLUMN      = 3;

    public static final int    FROM_COLUMN         = 3;

    public static final int    INCOME_COLUMN       = 3;

    /**
     * Field REQ_COLUMN
     */
    public static final int    REQ_COLUMN          = 4;

    public static final int    TITLE_COLUMN        = 4;

    public static final int    OUTCOME_COLUMN      = 4;

    /**
     * Field ELAPSED_COLUMN
     */
    public static final int    ELAPSED_COLUMN      = 5;

    /**
     * Field DEFAULT_LISTEN_PORT
     */
    public static final int    DEFAULT_LISTEN_PORT = 8890;

    /**
     * Field DEFAULT_HOST
     */
    public static final String DEFAULT_HOST        = "127.0.0.1";

    /**
     * Field DEFAULT_PORT
     */
    public static final int    DEFAULT_PORT        = 8888;

    public static Main getFrame() {
        return frame;
    }

    /**
     * Constructor
     * 
     * @param listenPort
     * @param targetHost
     * @param targetPort
     * @param embedded
     */
    public Main(int listenPort, String targetHost, int targetPort, boolean embedded) {
        super("Utils HTTP Monitor");

        UIManager.put("TextArea.font", MONOSPACED);

        notebook = new JTabbedPane();
        this.getContentPane().add(notebook);
        adminPane = new AdminPane(notebook, "Admin", listenPort, targetHost, targetPort);

        if (!embedded) {
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main.this.pack();
                Main.this.setSize(1000, 800);
                Main.this.setVisible(true);
            }
        });
    }

    /**
     * Constructor
     * 
     * @param listenPort
     * @param targetHost
     * @param targetPort
     */
    public Main(int listenPort, String targetHost, int targetPort) {
        this(listenPort, targetHost, targetPort, false);
    }

    /**
     * set up the L&F
     * 
     * @param nativeLookAndFeel
     * @throws Exception
     */
    private static void setupLookAndFeel(boolean nativeLookAndFeel) throws Exception {
        String classname = UIManager.getCrossPlatformLookAndFeelClassName();
        if (nativeLookAndFeel) {
            classname = UIManager.getSystemLookAndFeelClassName();
        }
        String lafProperty = System.getProperty("jmallory.laf", "");
        if (lafProperty.length() > 0) {
            classname = lafProperty;
        }
        try {
            UIManager.setLookAndFeel(classname);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    /**
     * this is our main method
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            // switch between swing L&F here
            setupLookAndFeel(true);
            if (args.length == 3) {
                int p1 = Integer.parseInt(args[0]);
                int p2 = Integer.parseInt(args[2]);
                frame = new Main(p1, args[1], p2);
            } else {
                frame = new Main(0, null, 0);
            }
        } catch (Throwable exp) {
            exp.printStackTrace();
        }
    }

    public AdminPane getAdminPane() {
        return adminPane;
    }

    public void setAdminPane(AdminPane adminPane) {
        this.adminPane = adminPane;
    }
}

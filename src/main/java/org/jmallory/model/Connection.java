package org.jmallory.model;

public interface Connection {
    /**
     * This method must be synchronized in implementation.
     */
    void wakeUp();
    
    /**
     * Halts this connection.
     */
    void halt();
    
    /**
     * Is this connection still active?
     * @return
     */
    boolean isActive();
}

package org.dsa.iot.dslink.node.value;


import org.dsa.iot.dslink.node.NodeListener;

/**
 * Used for handling updates that involve a previous value.
 *
 * @author Samuel Grenier
 * @see NodeListener#setValueHandler
 */
public class ValuePair {

    private final Value previous;
    private Value current;
    private boolean reject;

    /**
     * @param previous Previous value.
     * @param current The new current value.
     */
    public ValuePair(Value previous, Value current) {
        this.previous = previous;
        this.current = current;
    }

    /**
     * @return The previous, or old, value.
     */
    public Value getPrevious() {
        return previous;
    }

    /**
     * @return The current, or new, value.
     */
    public Value getCurrent() {
        return current;
    }

    /**
     * This allows for dynamic value control when being set from an external
     * source but the value is still valid and does not need to be rejected.
     *
     * @param value New value to set.
     */
    public void setCurrent(Value value) {

    }

    /**
     * @param reject Whether to reject the new value or not.
     */
    @SuppressWarnings("unused")
    public void setReject(boolean reject) {
        this.reject = reject;
    }

    /**
     * @return Whether to reject the new value or not.
     */
    public boolean isRejected() {
        return reject;
    }
}

package org.dsa.iot.commons;

/**
 * Provides an easy way to set variables from an inner class to an
 * outer class.
 *
 * @author Samuel Grenier
 */
public class Container<T> {

    private T value;

    /**
     * Initializes the {@link Container} with no value.
     */
    public Container() {
        this(null);
    }

    /**
     * Initializes the {@link Container} with a value.
     *
     * @param value Value to initialize.
     */
    public Container(T value) {
        this.value = value;
    }

    /**
     * @param value Value to set.
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * @return Value of the {@link Container}.
     */
    public T getValue() {
        return value;
    }
}

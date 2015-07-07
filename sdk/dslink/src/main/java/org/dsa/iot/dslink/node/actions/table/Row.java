package org.dsa.iot.dslink.node.actions.table;

import org.dsa.iot.dslink.node.value.Value;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class Row {

    private List<Value> values = new LinkedList<>();

    /**
     * Adds a value to the row.
     *
     * @param value Value to add.
     */
    public void addValue(Value value) {
        values.add(value);
    }

    /**
     * @return A collection of values in the row.
     */
    public List<Value> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Convenience method for quickly creating rows.
     *
     * @param values Array of values.
     * @return A created row.
     */
    public static Row make(Value... values) {
        Row row = new Row();
        for (Value v : values) {
            row.addValue(v);
        }
        return row;
    }
}

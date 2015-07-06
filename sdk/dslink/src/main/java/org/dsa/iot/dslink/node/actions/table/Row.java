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

    public void addValue(Value value) {
        values.add(value);
    }

    public List<Value> getValues() {
        return Collections.unmodifiableList(values);
    }

    public static Row make(Value... values) {
        Row row = new Row();
        for (Value v : values) {
            row.addValue(v);
        }
        return row;
    }
}

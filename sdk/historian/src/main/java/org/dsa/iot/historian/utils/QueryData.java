package org.dsa.iot.historian.utils;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class QueryData {

    private Value value;
    private long ts;

    public QueryData() {
        this(null, -1);
    }

    public QueryData(Value value, long ts) {
        this.value = value;
        this.ts = ts;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setTimestamp(long ts) {
        this.ts = ts;
    }

    public long getTimestamp() {
        return ts;
    }
}

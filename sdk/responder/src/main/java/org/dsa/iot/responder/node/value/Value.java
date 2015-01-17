package org.dsa.iot.responder.node.value;

/**
 * @author Samuel Grenier
 */
public class Value {

    private ValueType type;

    private Integer i;
    private Boolean b;
    private String s;

    public Value(Integer i) {
        set(i);
    }

    public Value(Boolean b) {
        set(b);
    }

    public Value(String s) {
        set(s);
    }

    public void set(Integer i) {
        set(ValueType.NUMBER, i, null, null);
    }

    public void set(Boolean b) {
        set(ValueType.BOOL, null, b, null);
    }

    public void set(String s) {
        set(ValueType.STRING, null, null, s);
    }

    private void set(ValueType type, Integer i, Boolean b, String s) {
        this.type = type;
        this.i = i;
        this.b = b;
        this.s = s;
    }

    public ValueType getType() {
        return type;
    }

    public Integer getInteger() {
        return i;
    }

    public Boolean getBoolean() {
        return b;
    }

    public String getString() {
        return s;
    }
}

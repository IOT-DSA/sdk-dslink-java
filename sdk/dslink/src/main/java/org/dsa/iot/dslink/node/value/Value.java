package org.dsa.iot.dslink.node.value;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Samuel Grenier
 */
@Getter
@EqualsAndHashCode(exclude = "immutable")
public class Value {

    private boolean immutable;
    private ValueType type;

    private Integer integer;
    private Boolean bool;
    private String string;

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
        checkImmutable();
        this.type = type;
        this.integer = i;
        this.bool = b;
        this.string = s;
    }

    public void setImmutable() {
        immutable = true;
    }

    private void checkImmutable() {
        if (isImmutable()) {
            throw new IllegalStateException("Attempting to modify immutable value");
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case NUMBER:
                return integer.toString();
            case BOOL:
                return bool.toString();
            case STRING:
                return string;
            default:
                throw new RuntimeException("Unknown type");
        }
    }
}

package org.dsa.iot.responder;

import org.dsa.iot.responder.node.value.Value;
import org.dsa.iot.responder.node.value.ValueType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class ValueTest {

    @Test
    public void initialValueSetter() {
        Value val = new Value(1);

        Assert.assertEquals(ValueType.NUMBER, val.getType());

        Assert.assertNotNull(val.getInteger());
        Assert.assertEquals(1, val.getInteger().intValue());

        Assert.assertNull(val.getBool());
        Assert.assertNull(val.getString());
    }

    @Test
    public void modifyingValues() {
        Value val = new Value(1);
        val.set(true);

        Assert.assertEquals(ValueType.BOOL, val.getType());
        Assert.assertNotNull(val.getBool());
        Assert.assertEquals(true, val.getBool());

        Assert.assertNull(val.getInteger());
        Assert.assertNull(val.getString());
    }

    @Test
    public void equals() {
        Value a = new Value(1);
        Value b = new Value(true);
        Value c = new Value(false);
        Value d = new Value("string");

        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(a.equals(c));
        Assert.assertFalse(a.equals(d));

        Assert.assertFalse(b.equals(a));
        Assert.assertFalse(b.equals(c));
        Assert.assertFalse(b.equals(d));

        Assert.assertFalse(c.equals(a));
        Assert.assertFalse(c.equals(b));
        Assert.assertFalse(c.equals(d));

        Assert.assertFalse(d.equals(a));
        Assert.assertFalse(d.equals(b));
        Assert.assertFalse(d.equals(c));
    }
}

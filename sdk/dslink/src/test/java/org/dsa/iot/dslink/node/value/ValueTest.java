package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the value API.
 *
 * @author Samuel Grenier
 */
public class ValueTest {

    @Test
    public void initialValueSetter() {
        Value val = new Value(1);
        Assert.assertEquals(ValueType.NUMBER, val.getType());

        Assert.assertNotNull(val.getNumber());
        Assert.assertEquals(1, val.getNumber().intValue());

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

        Assert.assertNull(val.getNumber());
        Assert.assertNull(val.getString());
    }

    /**
     * Ensures the following values are equal
     */
    @Test
    public void equal() {
        Value a = new Value(1);
        Value b = new Value(1);
        Assert.assertEquals(a, b);

        b.setImmutable();
        Assert.assertEquals(a, b);

        a = new Value(true);
        b = new Value(false);
        b.set(true);
        Assert.assertEquals(a, b);

        a = new Value("test");
        b = new Value("test");
        Assert.assertEquals(a, b);

        JsonArray array = new JsonArray();
        a.set(array);
        b.set(array);
        Assert.assertEquals(a, b);
    }

    /**
     * Ensures the following values are not equal
     */
    @Test
    public void notEqual() {
        Value a = new Value(1);
        Value b = new Value(true);
        Value c = new Value(false);
        Value d = new Value("string");
        Value e = new Value((String) null);
        d.setImmutable();

        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(a.equals(c));
        Assert.assertFalse(a.equals(d));
        Assert.assertFalse(a.equals(e));

        Assert.assertFalse(b.equals(a));
        Assert.assertFalse(b.equals(c));
        Assert.assertFalse(b.equals(d));
        Assert.assertFalse(b.equals(e));

        Assert.assertFalse(c.equals(a));
        Assert.assertFalse(c.equals(b));
        Assert.assertFalse(c.equals(d));
        Assert.assertFalse(c.equals(e));

        Assert.assertFalse(d.equals(a));
        Assert.assertFalse(d.equals(b));
        Assert.assertFalse(d.equals(c));
        Assert.assertFalse(d.equals(e));
    }

    @Test
    public void testNullValues() {
        Value a = new Value((Number) null);
        Value b = new Value((Boolean) null);
        Value c = new Value((String) null);
        Value d = new Value((byte[]) null);
        Value e = new Value((JsonObject) null);
        Value f = new Value((JsonArray) null);
        
        Assert.assertEquals("null", a.toString());
        Assert.assertEquals("null", b.toString());
        Assert.assertEquals("null", c.toString());
        Assert.assertEquals("null", d.toString());
        Assert.assertEquals("null", e.toString());
        Assert.assertEquals("null", f.toString());
    }
}

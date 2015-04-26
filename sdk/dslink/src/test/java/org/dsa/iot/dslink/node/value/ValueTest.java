package org.dsa.iot.dslink.node.value;

import org.junit.Assert;
import org.junit.Test;
import org.vertx.java.core.json.JsonArray;

/**
 * Tests the value API.
 *
 * @author Samuel Grenier
 */
public class ValueTest {

    @Test
    public void initialValueSetter() {
        Value val = new Value(1);
        Assert.assertEquals(ValueType.NUMBER, val.getVisibleType());
        Assert.assertEquals(ValueType.NUMBER, val.getInternalType());

        Assert.assertNotNull(val.getNumber());
        Assert.assertEquals(1, val.getNumber().intValue());

        Assert.assertNull(val.getBool());
        Assert.assertNull(val.getString());
    }

    @Test
    public void modifyingValues() {
        Value val = new Value(1, true);
        val.set(true);

        Assert.assertEquals(ValueType.BOOL, val.getInternalType());
        Assert.assertEquals(ValueType.DYNAMIC, val.getVisibleType());
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
        Value a = new Value(1, true);
        Value b = new Value(1, true);
        Assert.assertEquals(a, b);

        b.setImmutable();
        Assert.assertEquals(a, b);

        a = new Value(true, true);
        b = new Value(false, true);
        b.set(true);
        Assert.assertEquals(a, b);

        a = new Value("test", true);
        b = new Value("test", true);
        Assert.assertEquals(a, b);

        a.set(new JsonArray());
        b.set(new JsonArray());
        Assert.assertEquals(a, b);

        JsonArray arrayA = new JsonArray();
        arrayA.addString("Hello world");
        JsonArray arrayB = new JsonArray();
        arrayB.addString("Hello world");
        a.set(arrayA);
        b.set(arrayB);
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

}

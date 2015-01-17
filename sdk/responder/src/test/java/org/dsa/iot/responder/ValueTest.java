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

        Assert.assertNull(val.getBoolean());
        Assert.assertNull(val.getString());
    }

    @Test
    public void modifyingValues() {
        Value val = new Value(1);
        val.set(true);

        Assert.assertEquals(ValueType.BOOL, val.getType());
        Assert.assertNotNull(val.getBoolean());
        Assert.assertEquals(true, val.getBoolean());

        Assert.assertNull(val.getInteger());
        Assert.assertNull(val.getString());
    }
}

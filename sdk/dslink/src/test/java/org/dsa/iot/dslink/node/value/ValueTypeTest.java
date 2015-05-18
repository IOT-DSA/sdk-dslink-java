package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.node.Node;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class ValueTypeTest {

    @Test
    public void typeComparison() {
        ValueType dyn = ValueType.DYNAMIC;
        Assert.assertTrue(dyn.compare(ValueType.DYNAMIC));

        ValueType enums = ValueType.makeEnum("a", "b");
        Assert.assertTrue(enums.compare(ValueType.ENUM));
    }

    @Test
    public void validEnum() {
        Node node = new Node("root", null, null);
        node.setValueType(ValueType.makeEnum("a", "b", "c"));
        node.setValue(new Value("c"));
    }

    @Test(expected = RuntimeException.class)
    public void invalidEnum() {
        Node node = new Node("root", null, null);
        node.setValueType(ValueType.makeEnum("a", "b", "c"));
        node.setValue(new Value("d"));
    }
}

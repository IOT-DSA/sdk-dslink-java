package org.dsa.iot.dslink.serializer;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Deserializes a JSON file into a node manager
 *
 * @author Samuel Grenier
 */
public class Deserializer {

    private final NodeManager manager;

    public Deserializer(NodeManager manager) {
        this.manager = manager;
    }

    /**
     * Deserializes the object into the manager.
     *
     * @param object Object to deserialize.
     */
    @SuppressWarnings("unchecked")
    public void deserialize(JsonObject object) {
        Map<String, Object> map = object.toMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Node node = manager.getNode(name, true).getNode();
            Object value = entry.getValue();
            Map<String, Object> data = (Map<String, Object>) value;
            deserializeNode(node, data);
        }
    }

    @SuppressWarnings("unchecked")
    private void deserializeNode(Node node, Map<String, Object> map) {
        final String type = (String) map.get("$type");
        if (type != null) {
            ValueType t = ValueType.toValueType(type);
            node.setValueType(t);
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if ("$type".equals(name)) {
                continue;
            }
            if ("$is".equals(name)) {
                node.setProfile((String) value);
            } else if ("$interface".equals(name)) {
                node.setInterfaces((String) value);
            } else if ("$mixin".equals(name)) {
                node.setMixins((String) value);
            } else if ("$name".equals(name)) {
                node.setDisplayName((String) value);
            } else if ("$writable".equals(name)) {
                node.setWritable(Writable.toEnum((String) value));
            } else if ("$$password".equals(name)) {
                node.setPassword(((String) value).toCharArray());
            } else if ("?value".equals(name)) {
                node.setValue(ValueUtils.toValue(value));
            } else if (name.startsWith("$$")) {
                node.setRoConfig(name.substring(2), ValueUtils.toValue(value));
            } else if (name.startsWith("$")) {
                node.setConfig(name.substring(1), ValueUtils.toValue(value));
            } else if (name.startsWith("@")) {
                node.setAttribute(name.substring(1), ValueUtils.toValue(value));
            } else {
                Node child = node.createChild(name).build();
                Map<String, Object> children = (Map<String, Object>) value;
                deserializeNode(child, children);
            }
        }
    }
}

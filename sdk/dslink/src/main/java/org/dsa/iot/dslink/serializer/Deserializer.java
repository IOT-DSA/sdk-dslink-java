package org.dsa.iot.dslink.serializer;

import java.util.*;
import org.dsa.iot.dslink.node.*;
import org.dsa.iot.dslink.node.value.*;
import org.dsa.iot.dslink.util.json.*;

/**
 * Deserializes a JSON file into a node manager
 *
 * @author Samuel Grenier
 */
public class Deserializer {

    private final SerializationManager serializationManager;
    private final NodeManager nodeManager;

    public Deserializer(NodeManager nodeManager) {
        this(nodeManager.getSuperRoot().getLink().getSerialManager(), nodeManager);
    }

    public Deserializer(SerializationManager serializationManager,
                        NodeManager nodeManager) {
        this.serializationManager = serializationManager;
        this.nodeManager = nodeManager;
    }

    /**
     * Deserializes the object into the node manager.
     *
     * @param object Object to deserialize.
     */
    @SuppressWarnings("unchecked")
    public void deserialize(JsonObject object) {
        for (Map.Entry<String, Object> entry : object) {
            String name = entry.getKey();
            Node node = nodeManager.getNode(name, true).getNode();
            Object value = entry.getValue();
            JsonObject data = (JsonObject) value;
            deserializeNode(node, data);
        }
    }

    @SuppressWarnings("unchecked")
    private void deserializeNode(Node node, JsonObject map) {
        final String type = map.get("$type");
        if (type != null) {
            ValueType t = ValueType.toValueType(type);
            node.setValueType(t);
        }
        for (Map.Entry<String, Object> entry : map) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value == null || "$type".equals(name)) {
                continue;
            }
            if ("$is".equals(name)) {
                node.setProfile((String) value);
            } else if ("$interface".equals(name)) {
                node.setInterfaces((String) value);
            } else if ("$name".equals(name)) {
                node.setDisplayName((String) value);
            } else if ("$writable".equals(name)) {
                node.setWritable(Writable.toEnum((String) value));
            } else if ("$hidden".equals(name)) {
                node.setHidden((Boolean) value);
            } else if ("$$password".equals(name)) {
                String pass = decrypt((String) value);
                node.setPassword(pass.toCharArray());
            } else if ("?value".equals(name)) {
                ValueType t = node.getValueType();
                Value val = ValueUtils.toValue(value);
                if (t != null && val != null
                        && val.getType().compare(ValueType.STRING)
                        && t.compare(ValueType.NUMBER)
                        && "NaN".equals(val.getString())) {
                    node.setValue(new Value(Float.NaN));
                } else {
                    node.setValue(val);
                }
            } else if (name.startsWith("$$")) {
                if (name.endsWith(SerializationManager.PASSWORD_TOKEN)) {
                    value = decrypt((String) value);
                }
                node.setRoConfig(name.substring(2), ValueUtils.toValue(value));
            } else if (name.startsWith("$")) {
                node.setConfig(name.substring(1), ValueUtils.toValue(value));
            } else if (name.startsWith("@")) {
                node.setAttribute(name.substring(1), ValueUtils.toValue(value));
            } else {
                Node child = node.createChild(name, false).build();
                JsonObject children = (JsonObject) value;
                deserializeNode(child, children);
            }
        }
    }

    private String decrypt(String pass) {
        return serializationManager.decrypt(nodeManager.getSuperRoot(), pass);
    }

}

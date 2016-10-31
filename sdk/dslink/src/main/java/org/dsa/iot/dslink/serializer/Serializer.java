package org.dsa.iot.dslink.serializer;

import java.util.*;
import org.dsa.iot.dslink.node.*;
import org.dsa.iot.dslink.node.value.*;
import org.dsa.iot.dslink.util.*;
import org.dsa.iot.dslink.util.json.*;

/**
 * @author Samuel Grenier
 */
public class Serializer {

    private final NodeManager nodeManager;
    private final SerializationManager serializationManager;

    public Serializer(NodeManager nodeManager) {
        this(nodeManager.getSuperRoot().getLink().getSerialManager(), nodeManager);
    }

    public Serializer(SerializationManager serializationManager,
                      NodeManager nodeManager) {
        this.serializationManager = serializationManager;
        this.nodeManager = nodeManager;
    }

    public JsonObject serialize() {
        JsonObject top = new JsonObject();

        Map<String, Node> rootChildren = nodeManager.getChildren("/");
        if (rootChildren != null) {
            for (Node child : rootChildren.values()) {
                if (child.isSerializable()) {
                    JsonObject childOut = new JsonObject();
                    serializeChildren(childOut, child);
                    top.put(child.getName(), childOut);
                }
            }
        }
        return top;
    }

    private void serializeChildren(JsonObject out, Node parent) {
        String data = parent.getDisplayName();
        if (data != null) {
            out.put("$name", data);
        }

        Set<String> set = parent.getInterfaces();
        if (set != null && set.size() > 0) {
            out.put("$interface", StringUtils.join(set, "|"));
        }

        String profile = parent.getProfile();
        if (profile != null) {
            out.put("$is", profile);
        }

        ValueType type = parent.getValueType();
        if (type != null) {
            out.put("$type", type.toJsonString());
            Value value = parent.getValue();
            if (value != null && value.isSerializable()) {
                out.put("?value", value);
            }
        }

        char[] password = parent.getPassword();
        if (password != null) {
            out.put("$$password", encrypt(new String(password)));
        }

        Writable writable = parent.getWritable();
        if (!(writable == null || writable == Writable.NEVER)) {
            out.put("$writable", writable.toJsonName());
        }

        if (parent.isHidden()) {
            out.put("$hidden", true);
        }

        addValues("$$", out, parent.getRoConfigurations());
        addValues("$", out, parent.getConfigurations());
        addValues("@", out, parent.getAttributes());

        Map<String, Node> children = parent.getChildren();
        if (children != null && children.size() > 0) {
            for (Node child : children.values()) {
                if (child.isSerializable()) {
                    JsonObject childOut = new JsonObject();
                    serializeChildren(childOut, child);
                    out.put(child.getName(), childOut);
                }
            }
        }
    }

    private void addValues(String prefix, JsonObject out, Map<String, Value> vals) {
        if (vals == null || vals.size() == 0) {
            return;
        }
        boolean testPassword = prefix.equals("$$");
        for (Map.Entry<String, Value> entry : vals.entrySet()) {
            Value value = entry.getValue();
            if (value.isSerializable()) {
                String name = prefix + entry.getKey();
                if (testPassword && (value.getType() == ValueType.STRING)) {
                    if (name.endsWith(SerializationManager.PASSWORD_TOKEN)) {
                        value = new Value(encrypt(value.getString()));
                    }
                }
                out.put(name, value);
            }
        }
    }

    private String encrypt(String pass) {
        return serializationManager.encrypt(nodeManager.getSuperRoot(), pass);
    }

}

package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Request used to set a value
 *
 * @author Samuel Grenier
 */
public class SetRequest extends Request {

    private final String path;
    private final Value value;

    public SetRequest(String path, Value value) {
        if (path == null) {
            throw new NullPointerException("path");
        } else if (value == null) {
            throw new NullPointerException("value");
        }
        this.path = path;
        this.value = value;
    }

    @Override
    public String getName() {
        return "set";
    }

    public String getPath() {
        return path;
    }

    @Override
    public void addJsonValues(JsonObject out) {
        out.put("path", path);
        out.put("value", value);
    }
}

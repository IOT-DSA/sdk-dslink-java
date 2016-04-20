package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListRequest extends Request {
    private final String path;

    public ListRequest(String path) {
        if (path == null)
            throw new NullPointerException("path");
        this.path = path;
    }

    @Override
    public String getName() {
        return "list";
    }

    /**
     * @return Path used in the request
     */
    public String getPath() {
        return path;
    }

    @Override
    public void addJsonValues(JsonObject out) {
        out.put("path", path);
    }
}

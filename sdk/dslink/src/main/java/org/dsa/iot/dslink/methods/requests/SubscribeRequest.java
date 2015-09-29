package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.util.SubData;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class SubscribeRequest implements Request {

    private Map<SubData, Integer> subSids;
    private final Set<SubData> paths;

    /**
     * @param paths Paths to subscribe to.
     */
    public SubscribeRequest(Set<SubData> paths) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        this.paths = paths;
    }

    public Set<SubData> getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    public void setSubSids(Map<SubData, Integer> sids) {
        this.subSids = sids;
    }

    @Override
    public String getName() {
        return "subscribe";
    }

    @Override
    public void addJsonValues(JsonObject out) {
        JsonArray array = new JsonArray();
        for (Map.Entry<SubData, Integer> sub : subSids.entrySet()) {
            SubData data = sub.getKey();
            String path = data.getPath();
            Integer qos = data.getQos();

            JsonObject obj = new JsonObject();
            obj.put("path", path);
            obj.put("sid", sub.getValue());
            if (qos != null) {
                obj.put("qos", qos);
            }
            array.add(obj);
        }
        out.put("paths", array);
    }
}

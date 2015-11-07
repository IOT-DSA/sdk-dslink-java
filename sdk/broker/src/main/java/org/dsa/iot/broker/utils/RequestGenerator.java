package org.dsa.iot.broker.utils;

import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class RequestGenerator {

    public static JsonObject list(ParsedPath path, int rid) {
        JsonObject req = new JsonObject();
        req.put("method", "list");
        req.put("rid", rid);
        req.put("path", path.base());

        JsonArray reqs = new JsonArray();
        reqs.add(req);
        JsonObject top = new JsonObject();
        top.put("requests", reqs);
        return top;
    }

    public static JsonObject subscribe(ParsedPath path, int sid, int rid) {
        JsonObject req = new JsonObject();
        req.put("rid", rid);
        req.put("method", "subscribe");
        {
            JsonArray paths = new JsonArray();
            {
                JsonObject obj = new JsonObject();
                obj.put("path", path.base());
                obj.put("sid", sid);
                paths.add(obj);
            }
            req.put("paths", paths);
        }

        JsonArray reqs = new JsonArray();
        reqs.add(req);
        JsonObject top = new JsonObject();
        top.put("requests", reqs);
        return top;
    }

    public static JsonObject unsubscribe(int rid, int sid) {
        JsonObject req = new JsonObject();
        req.put("rid", rid);
        req.put("method", "unsubscribe");
        JsonArray sids = new JsonArray();
        sids.add(sid);
        req.put("sids", sids);

        JsonArray reqs = new JsonArray();
        reqs.add(req);

        JsonObject top = new JsonObject();
        top.put("requests", reqs);
        return top;
    }
}

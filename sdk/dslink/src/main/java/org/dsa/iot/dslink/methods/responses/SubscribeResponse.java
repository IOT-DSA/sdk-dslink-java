package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class SubscribeResponse implements Response {

    private final int rid;
    private final DSLink link;
    private final SubscriptionManager manager;

    public SubscribeResponse(int rid, DSLink link) {
        this.rid = rid;
        this.link = link;
        this.manager = link.getSubscriptionManager();
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        JsonArray paths = in.getArray("paths");
        if (paths != null && paths.size() > 0) {
            for (Object obj : paths) {
                JsonObject subData = (JsonObject) obj;
                String path = subData.getString("path");
                int sid = subData.getInteger("sid");

                NodeManager nm = link.getNodeManager();
                NodePair pair = nm.getNode(path, false, false);
                if (pair != null) {
                    Node node = pair.getNode();
                    if (node != null) {
                        manager.addValueSub(node, sid);
                    }
                }
            }
        }

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }
}

package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;

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
            StringBuilder builder = null;
            for (Object obj : paths) {
                try {
                    JsonObject subData = (JsonObject) obj;
                    String path = subData.getString("path");
                    int sid = subData.getInteger("sid");

                    NodeManager nm = link.getNodeManager();
                    NodePair pair = nm.getNode(path, false, false);
                    Node node = pair.getNode();
                    if (node == null) {
                        DSLinkHandler h = link.getLinkHandler();
                        h.onSubscriptionFail(path);
                    }
                    manager.addValueSub(path, sid);
                } catch (Exception e) {
                    if (builder == null) {
                        builder = new StringBuilder();
                    }

                    StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    builder.append(writer.toString());
                    builder.append("\n");
                }
            }
            if (builder != null) {
                throw new RuntimeException(builder.toString());
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

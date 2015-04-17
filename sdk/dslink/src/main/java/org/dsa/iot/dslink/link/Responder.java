package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.methods.responses.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming requests and outgoing responses.
 *
 * @author Samuel Grenier
 */
public class Responder extends Linkable {

    private final Map<Integer, Response> resps = new ConcurrentHashMap<>();

    public Responder(DSLinkHandler handler) {
        super(handler);
    }

    /**
     * Handles incoming requests
     *
     * @param in Incoming request
     * @return Outgoing response
     */
    public JsonObject parse(JsonObject in) {
        final Integer rid = in.getInteger("rid");
        final String method = in.getString("method");
        if (rid == null) {
            throw new NullPointerException("rid");
        } else if (method == null) {
            throw new NullPointerException("method");
        }

        DSLink link = getDSLink();
        NodeManager nodeManager = link.getNodeManager();
        Response response;
        switch (method) {
            case "list":
                String path = in.getString("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                Node node = nodeManager.getNode(path).getNode();
                node.getListener().postListUpdate(node);
                SubscriptionManager subs = link.getSubscriptionManager();
                response = new ListResponse(link, subs, rid, node);
                break;
            case "set":
                path = in.getString("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                NodePair pair = nodeManager.getNode(path);
                response = new SetResponse(rid, pair);
                break;
            case "subscribe":
                response = new SubscribeResponse(rid, link);
                break;
            case "unsubscribe":
                response = new UnsubscribeResponse(rid, link);
                break;
            case "invoke":
                path = in.getString("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                node = nodeManager.getNode(path).getNode();
                response = new InvokeResponse(link, rid, node);
                break;
            case "close":
                Response resp = resps.remove(rid);
                response = new CloseResponse(rid, resp);
                break;
            default:
                throw new RuntimeException("Unknown method: " + method);
        }

        JsonObject resp = response.getJsonResponse(in);
        if (!StreamState.CLOSED.getJsonName().equals(resp.getString("stream"))) {
            resps.put(rid, response);
        }
        return resp;
    }
}

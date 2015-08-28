package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.methods.responses.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
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
     * Forcibly removes a response from the cache.
     *
     * @param rid ID to remove.
     */
    public void removeResponse(int rid) {
        resps.remove(rid);
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
                Node node = nodeManager.getNode(path, false, false).getNode();
                if (node != null) {
                    node.getListener().postListUpdate();
                }
                SubscriptionManager subs = link.getSubscriptionManager();
                response = new ListResponse(link, subs, rid, node);
                break;
            case "set":
                path = in.getString("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                response = new SetResponse(rid, link, path);
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
                response = new InvokeResponse(link, rid, path);
                break;
            case "close":
                Response resp = resps.remove(rid);
                response = new CloseResponse(rid, resp);
                break;
            default:
                throw new RuntimeException("Unknown method: " + method);
        }

        JsonObject resp = response.getJsonResponse(in);
        String closedName = StreamState.CLOSED.getJsonName();
        if (resp != null && !closedName.equals(resp.getString("stream"))) {
            resps.put(rid, response);
        }
        return resp;
    }
}

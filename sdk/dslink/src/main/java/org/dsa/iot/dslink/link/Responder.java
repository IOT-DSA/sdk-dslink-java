package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.methods.responses.*;
import org.dsa.iot.dslink.node.*;
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

                Node node = nodeManager.getSuperRoot();
                if (!"/".equals(path)) {
                    String[] split = NodeManager.splitPath(path);
                    node = nodeManager.getSuperRoot().getChild(split[0]);
                    for (int i = 1; i < split.length; ++i) {
                        Node tmp = node.getChild(split[i]);
                        if (tmp == null) {
                            NodeBuilder b = node.createChild(split[i]);
                            b.setVisible(false);
                            tmp = b.build();
                        }
                        node = tmp;
                    }
                }

                node.getListener().postListUpdate();
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

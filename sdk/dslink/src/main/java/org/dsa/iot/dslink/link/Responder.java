package org.dsa.iot.dslink.link;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.methods.responses.CloseResponse;
import org.dsa.iot.dslink.methods.responses.InvokeResponse;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.methods.responses.RemoveResponse;
import org.dsa.iot.dslink.methods.responses.SetResponse;
import org.dsa.iot.dslink.methods.responses.SubscribeResponse;
import org.dsa.iot.dslink.methods.responses.UnsubscribeResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Handles incoming requests and outgoing responses.
 *
 * @author Samuel Grenier
 */
public class Responder extends Linkable {

    private final Map<Integer, Response> resps = new ConcurrentHashMap<>();
    private int maxStreams = 0;

    public Responder(DSLinkHandler handler) {
        super(handler);
        maxStreams = handler.getConfig().getMaxOpenStreams();
    }

    @Override
    public void batchSet(Map<Node, Value> updates) {
        SubscriptionManager sm = getSubscriptionManager();
        if (sm == null) {
            for (Map.Entry<Node, Value> update : updates.entrySet()) {
                update.getKey().setValue(update.getValue());
            }
            return;
        }
        sm.batchValueUpdate(updates, true);
    }

    /**
     * Handles incoming requests
     *
     * @param in Incoming request
     * @return Outgoing response
     */
    public JsonObject parse(JsonObject in) {
        final Integer rid = in.get("rid");
        final String method = in.get("method");
        if (rid == null) {
            throw new NullPointerException("rid");
        } else if (method == null) {
            throw new NullPointerException("method");
        }

        DSLink link = getDSLink();
        NodeManager nodeManager = link.getNodeManager();
        Response response = null;
        switch (method) {
            case "list": {
                String path = in.get("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                if ((maxStreams > 0) && (resps.size() >= maxStreams)) {
                    throw new IllegalStateException("Too many open streams");
                }
                Node node = nodeManager.getNode(path, false, false).getNode();
                if (node != null) {
                    node.getListener().postListUpdate();
                }
                SubscriptionManager subs = link.getSubscriptionManager();
                response = new ListResponse(link, subs, rid, node, path);
                break;
            }
            case "set": {
                String path = in.get("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                response = new SetResponse(rid, link, path);
                break;
            }
            case "subscribe": {
                response = new SubscribeResponse(rid, link);
                break;
            }
            case "unsubscribe": {
                response = new UnsubscribeResponse(rid, link);
                break;
            }
            case "invoke": {
                String path = in.get("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                if ((maxStreams > 0) && (resps.size() >= maxStreams)) {
                    throw new IllegalStateException("Too many open streams");
                }
                response = new InvokeResponse(link, rid, path);
                break;
            }
            case "close": {
                Response resp = resps.remove(rid);
                response = new CloseResponse(rid, resp);
                break;
            }
            case "remove": {
                String path = in.get("path");
                if (path == null) {
                    throw new NullPointerException("path");
                }
                NodePair pair = nodeManager.getNode(path);
                response = new RemoveResponse(rid, pair);
                break;
            }
            default:
                throw new RuntimeException("Unknown method: " + method);
        }

        JsonObject resp = response.getJsonResponse(in);
        if (!StreamState.CLOSED.getJsonName().equals(resp.get("stream"))) {
            resps.put(rid, response);
        }
        return resp;
    }

    /**
     * Forcibly removes a response from the cache.
     *
     * @param rid ID to remove.
     */
    public void removeResponse(int rid) {
        resps.remove(rid);
    }
}

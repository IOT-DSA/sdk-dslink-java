package org.dsa.iot.dslink.requester;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles incoming responses and outgoing requests.
 *
 * @author Samuel Grenier
 */
public class Requester {

    private final Map<Integer, Request> reqs = new ConcurrentHashMap<>();
    private final DSLinkHandler handler;

    private WeakReference<DSLink> link;

    /**
     * Current request ID to send to the client
     */
    private final AtomicInteger currentReqID = new AtomicInteger();

    /**
     * Constructs a requester
     *
     * @param handler Handler for callbacks and data handling
     */
    public Requester(DSLinkHandler handler) {
        if (handler == null)
            throw new NullPointerException("handler");
        this.handler = handler;
    }

    /**
     * The DSLink object is used for the client and node manager.
     * @param link The link to set.
     */
    public void setDSLink(DSLink link) {
        if (link == null)
            throw new NullPointerException("link");
        this.link = new WeakReference<>(link);
    }

    /**
     * @return A reference to the dslink, can be null
     */
    public DSLink getDSLink() {
        return link.get();
    }

    /**
     * Sends a request to the client.
     *
     * @param request Request to send to the client
     */
    public void sendRequest(Request request) {
        DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        JsonObject obj = new JsonObject();
        request.addJsonValues(obj);
        {
            int rid = currentReqID.incrementAndGet();
            obj.putNumber("rid", rid);
            reqs.put(rid, request);
        }
        obj.putString("method", request.getName());

        JsonObject top = new JsonObject();
        JsonArray requests = new JsonArray();
        requests.addObject(obj);
        top.putArray("requests", requests);
        link.getClient().write(top);
    }

    /**
     * Parses a response that came from an client.
     *
     * @param in Parses an incoming response
     */
    public void parseResponse(JsonObject in) {
        DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        Integer rid = in.getInteger("rid");
        Request request = reqs.get(rid);
        String method = request.getName();
        NodeManager manager = link.getNodeManager();
        switch (method) {
            case "list":
                ListRequest req = (ListRequest) request;
                Node node = manager.getNode(req.getPath(), true);
                ListResponse resp = new ListResponse(rid, node);
                resp.populate(in);
                handler.onListResponse(req, resp);
                break;
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }

        String streamState = in.getString("stream");
        if (StreamState.CLOSED.getJsonName().equals(streamState)) {
            reqs.remove(rid);
        }
    }
}

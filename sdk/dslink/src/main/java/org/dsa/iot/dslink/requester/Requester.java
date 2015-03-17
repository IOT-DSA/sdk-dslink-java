package org.dsa.iot.dslink.requester;

import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Requester {

    private final Map<Integer, Request> reqs = new ConcurrentHashMap<>();
    private final NodeManager nodeManager;
    private final DSLinkHandler handler;

    private RemoteEndpoint endpoint;

    /**
     * Current request ID to send to the endpoint
     */
    private final AtomicInteger currentReqID = new AtomicInteger();

    public Requester(NodeManager manager, DSLinkHandler handler) {
        if (manager == null)
            throw new NullPointerException("manager");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.nodeManager = manager;
        this.handler = handler;
    }

    /**
     * Sets the endpoint that requester sends requests to. Must be set
     * before sending any requests
     * @param endpoint Endpoint to set.
     */
    public void setRemoteEndpoint(RemoteEndpoint endpoint) {
        if (endpoint == null)
            throw new NullPointerException("endpoint");
        this.endpoint = endpoint;
    }

    public void sendRequest(Request request) {
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
        endpoint.write(top);
    }

    public void parseResponse(JsonObject in) {
        Integer rid = in.getInteger("rid");
        Request request = reqs.get(rid);
        String method = request.getName();
        Response response;
        switch (method) {
            case "list":
                ListRequest req = (ListRequest) request;
                Node node = nodeManager.getNode(req.getPath(), true);
                response = new ListResponse(rid, node);
                break;
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }

        String streamState = in.getString("stream");
        if (StreamState.CLOSED.getJsonName().equals(streamState)) {
            reqs.remove(rid);
        }

        JsonArray updates = in.getArray("updates");
        if (updates != null) {
            response.populate(in.getArray("updates"));
        }
        handler.onResponse(request, response);
    }
}

package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.methods.requests.*;
import org.dsa.iot.dslink.methods.responses.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.util.NodePair;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles incoming responses and outgoing requests.
 *
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private final Map<Integer, Request> reqs = new ConcurrentHashMap<>();

    /**
     * Current request ID to send to the client
     */
    private final AtomicInteger currentReqID = new AtomicInteger();

    /**
     * Current subscription ID to send to the client
     */
    private final AtomicInteger currentSubID = new AtomicInteger();

    /**
     * Mapping of path->sid
     */
    private final Map<String, Integer> subs = new ConcurrentHashMap<>();

    /**
     * Constructs a requester
     *
     * @param handler Handler for callbacks and data handling
     */
    public Requester(DSLinkHandler handler) {
        super(handler);
    }

    public void subscribe(Set<String> paths) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        Map<String, Integer> subs = new HashMap<>();
        int min = currentSubID.getAndAdd(paths.size());
        int max = min + paths.size();
        Iterator<String> it = paths.iterator();
        while (min < max) {
            String path = NodeManager.normalizePath(it.next(), true);
            subs.put(path, min++);
        }
        SubscribeRequest req = new SubscribeRequest(subs);
        this.subs.putAll(subs);
        sendRequest(req, currentReqID.incrementAndGet());
    }

    public void unsubscribe(Set<String> paths) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        List<Integer> subs = new ArrayList<>();
        for (String path : paths) {
            path = NodeManager.normalizePath(path, true);
            Integer sid = this.subs.remove(path);
            if (sid != null) {
                subs.add(sid);
            }
        }
        UnsubscribeRequest req = new UnsubscribeRequest(subs);
        sendRequest(req, currentReqID.incrementAndGet());
    }

    /**
     * Sends a request to the responder to close the given stream.
     *
     * @param rid Stream to close.
     */
    public void closeStream(int rid) {
        CloseRequest req = new CloseRequest();
        sendRequest(req, rid);
    }

    /**
     * Sends a request to the client.
     *
     * @param request Request to send to the client
     */
    public void sendRequest(Request request) {
        int rid = currentReqID.incrementAndGet();
        sendRequest(request, rid);
    }

    /**
     * Sends a request to the client with a given request ID.
     *
     * @param request Request to send to the client
     * @param rid Request ID to use
     */
    private void sendRequest(Request request, int rid) {
        DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        JsonObject obj = new JsonObject();
        request.addJsonValues(obj);
        {
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
     * Handles incoming responses.
     *
     * @param in Incoming response.
     */
    public void parse(JsonObject in) {
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
                ListRequest listRequest = (ListRequest) request;
                Node node = manager.getNode(listRequest.getPath(), true).getNode();
                SubscriptionManager subs = link.getSubscriptionManager();
                ListResponse resp = new ListResponse(link, subs, rid, node);
                resp.populate(in);
                getHandler().onListResponse(listRequest, resp);
                break;
            case "set":
                SetRequest setRequest = (SetRequest) request;
                NodePair pair = manager.getNode(setRequest.getPath(), true);
                SetResponse setResponse = new SetResponse(rid, pair);
                setResponse.populate(in);
                getHandler().onSetResponse(setRequest, setResponse);
                break;
            case "remove":
                RemoveRequest removeRequest = (RemoveRequest) request;
                pair = manager.getNode(removeRequest.getPath(), true);
                RemoveResponse removeResponse = new RemoveResponse(rid, pair);
                removeResponse.populate(in);
                getHandler().onRemoveResponse(removeRequest, removeResponse);
                break;
            case "close":
                CloseRequest closeRequest = (CloseRequest) request;
                CloseResponse closeResponse = new CloseResponse(rid, null);
                closeResponse.populate(in);
                getHandler().onCloseResponse(closeRequest, closeResponse);
                break;
            case "subscribe":
                SubscribeRequest subReq = (SubscribeRequest) request;
                SubscribeResponse subResp = new SubscribeResponse(rid, link);
                subResp.populate(in);
                getHandler().onSubscribeResponse(subReq, subResp);
                break;
            case "unsubscribe":
                UnsubscribeRequest unsubReq = (UnsubscribeRequest) request;
                UnsubscribeResponse unsubResp = new UnsubscribeResponse(rid, link);
                unsubResp.populate(in);
                getHandler().onUnsubscribeResponse(unsubReq, unsubResp);
                break;
            case "invoke":
                InvokeRequest inReq = (InvokeRequest) request;
                node = manager.getNode(inReq.getPath(), true).getNode();
                InvokeResponse inResp = new InvokeResponse(rid, node);
                inResp.populate(in);
                getHandler().onInvokeResponse(inReq, inResp);
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

package org.dsa.iot.dslink.link;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.methods.requests.*;
import org.dsa.iot.dslink.methods.responses.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.SubData;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Handles incoming responses and outgoing requests.
 *
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private final Map<Integer, RequestWrapper> reqs;

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
    private final Map<String, Integer> subPaths = new ConcurrentHashMap<>();

    /**
     * Mapping of sid->path
     */
    private final Map<Integer, String> subSids = new ConcurrentHashMap<>();

    /**
     * Mapping of sid->handler
     */
    private final Map<Integer, Handler<SubscriptionValue>> subUpdates = new ConcurrentHashMap<>();

    /**
     * Mapping of rid->response
     */
    private final Map<Integer, InvokeResponse> invokeResponses = new HashMap<>();

    private SubscriptionHelper subscriptionHelper;

    /**
     * Constructs a requester
     *
     * @param handler Handler for callbacks and data handling
     */
    public Requester(DSLinkHandler handler) {
        super(handler);
        reqs = new ConcurrentHashMap<>();
    }

    @Override
    public void batchSet(Map<Node, Value> updates) {
        throw new UnsupportedOperationException();
    }

    /**
     * If you need to have multiple subscriptions to the same path, use the
     * subscriber helper.
     */
    @SuppressWarnings("unused")
    public synchronized SubscriptionHelper getSubscriptionHelper() {
        if (subscriptionHelper == null) {
            subscriptionHelper = new SubscriptionHelper(this);
        }
        return subscriptionHelper;
    }

    @SuppressWarnings("unused")
    public Map<String, Integer> getSubscriptionPaths() {
        return Collections.unmodifiableMap(subPaths);
    }

    public Map<Integer, String> getSubscriptionIDs() {
        return Collections.unmodifiableMap(subSids);
    }

    @SuppressWarnings("unused")
    public boolean isSubscribed(String path) {
        return subPaths.containsKey(path);
    }

    public Map<Integer, Handler<SubscriptionValue>> getSubscriptionHandlers() {
        return Collections.unmodifiableMap(subUpdates);
    }

    public void subscribe(String path,
                          Handler<SubscriptionValue> onUpdate) {
        SubData sub = new SubData(path, null);
        subscribe(sub, onUpdate);
    }

    public void subscribe(SubData path,
                          Handler<SubscriptionValue> onUpdate) {
        subscribe(Collections.singleton(path), onUpdate);
    }

    public void subscribe(Set<SubData> paths,
                          Handler<SubscriptionValue> onUpdate) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        subscribe(new SubscribeRequest(paths), onUpdate);
    }

    public void subscribe(SubscribeRequest req,
                          Handler<SubscriptionValue> onUpdate) {
        if (req == null) {
            throw new NullPointerException("req");
        }
        final Set<SubData> paths = req.getPaths();
        Map<SubData, Integer> subs = new HashMap<>();
        int min = currentSubID.getAndAdd(paths.size());
        int max = min + paths.size();
        Iterator<SubData> it = paths.iterator();
        StringBuilder error = null;
        while (min < max && it.hasNext()) {
            try {
                SubData data = it.next();
                String path = data.getPath();
                subs.put(data, min);
                Integer prev = subPaths.put(path, min);
                if (prev != null) {
                    String err = "Path " + path + " already subscribed";
                    throw new RuntimeException(err);
                }
                subSids.put(min, path);
                if (onUpdate != null) {
                    subUpdates.put(min, onUpdate);
                }
                min++;
            } catch (IllegalArgumentException e) {
                if (error == null) {
                    error = new StringBuilder();
                }
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                error.append(writer.toString());
                error.append("\n\n");
            }
        }

        req.setSubSids(subs);
        RequestWrapper wrapper = new RequestWrapper(req);
        sendRequest(wrapper, currentReqID.incrementAndGet());
        if (error != null) {
            throw new RuntimeException(error.toString());
        }
    }

    public void unsubscribe(String path, Handler<UnsubscribeResponse> onResponse) {
        Set<String> paths = new HashSet<>();
        paths.add(path);
        unsubscribe(paths, onResponse);
    }

    public void unsubscribe(Set<String> paths, Handler<UnsubscribeResponse> onResponse) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        List<Integer> subs = new ArrayList<>();
        for (String path : paths) {
            //just in case...
            if (subscriptionHelper != null) {
                subscriptionHelper.clear(path);
            }
            path = NodeManager.normalizePath(path, true);
            Integer sid = subPaths.remove(path);
            if (sid != null) {
                subs.add(sid);
                subSids.remove(sid);
                subUpdates.remove(sid);
            }
        }
        UnsubscribeRequest req = new UnsubscribeRequest(subs);
        RequestWrapper wrapper = new RequestWrapper(req);
        wrapper.unsubHandler = onResponse;
        sendRequest(wrapper, currentReqID.incrementAndGet());
    }

    /**
     * Sends a request to the responder to close the given stream.
     *
     * @param rid        Stream to close.
     * @param onResponse Response.
     */
    @SuppressWarnings("unused")
    public void closeStream(int rid, Handler<CloseResponse> onResponse) {
        CloseRequest req = new CloseRequest();
        RequestWrapper wrapper = new RequestWrapper(req);
        sendRequest(wrapper, rid);

        reqs.remove(rid);
        if (onResponse != null) {
            onResponse.handle(new CloseResponse(rid, null));
        }
    }

    /**
     * Sends an invocation request.
     *
     * @param request    Invocation request.
     * @param onResponse Response.
     * @return Request ID that can be used to close the stream.
     * @see InvokeResponse#getState To determine if the stream is open. If the
     * stream isn't closed then it can be continuously invoked.
     */
    public int invoke(InvokeRequest request, Handler<InvokeResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.invokeHandler = onResponse;
        return sendRequest(wrapper);
    }

    /**
     * Invokes a previously open invocation stream. The stream must not be
     * closed.
     *
     * @param rid    Previous invocation request ID
     * @param params Parameters of the invocation, can be {@code null}
     * @see #invoke
     */
    public void continuousInvoke(int rid, JsonObject params) {
        Request req = new ContinuousInvokeRequest(params);
        RequestWrapper wrapper = new RequestWrapper(req);
        sendRequest(wrapper, rid, false);
    }

    /**
     * Sends a list request.
     *
     * @param request    List request.
     * @param onResponse Response.
     * @return Request ID that can be used to close the stream.
     */
    public int list(ListRequest request, Handler<ListResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.listHandler = onResponse;
        return sendRequest(wrapper);
    }

    /**
     * Sends a set request.
     *
     * @param request    Set request.
     * @param onResponse Response.
     */
    public void set(SetRequest request, Handler<SetResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.setHandler = onResponse;
        sendRequest(wrapper);
    }

    /**
     * Sends a remove request.
     *
     * @param request    Remove request.
     * @param onResponse Called when a response is received.
     */
    public void remove(RemoveRequest request, Handler<RemoveResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.removeHandler = onResponse;
        sendRequest(wrapper);
    }

    /**
     * Sends a request to the client.
     *
     * @param wrapper Request to send to the client.
     */
    private int sendRequest(RequestWrapper wrapper) {
        int rid = currentReqID.incrementAndGet();
        sendRequest(wrapper, rid);
        return rid;
    }

    /**
     * Sends a request to the client with a given request ID.
     *
     * @param wrapper Request to send to the client.
     * @param rid     Request ID to use.
     */
    private void sendRequest(RequestWrapper wrapper, int rid) {
        sendRequest(wrapper, rid, true);
    }

    /**
     * Sends a request to the client with a given request ID.
     *
     * @param wrapper Request to send to the client.
     * @param rid     Request ID to use.
     * @param merge   Whether the request should merge.
     */
    private void sendRequest(RequestWrapper wrapper,
                             int rid,
                             boolean merge) {
        final DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        Request request = wrapper.request;
        JsonObject obj = new JsonObject();
        request.addJsonValues(obj);
        {
            obj.put("rid", rid);
            if (wrapper.shouldStore()) {
                reqs.put(rid, wrapper);
            }
        }
        {
            String name = request.getName();
            if (name != null) {
                obj.put("method", request.getName());
            }
        }
        link.getWriter().writeRequest(obj, merge);
    }

    /**
     * Handles incoming responses.
     *
     * @param in Incoming response.
     */
    public void parse(final JsonObject in) {
        DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        int rid = in.get("rid");
        if (rid == 0) {
            new SubscriptionUpdate(this).populate(in);
            return;
        }
        RequestWrapper wrapper = reqs.get(rid);
        if (wrapper == null) {
            return;
        }
        Request request = wrapper.request;
        String method = request.getName();

        StreamState stream = StreamState.toEnum((String) in.get("stream"));
        if (stream == null) {
            stream = StreamState.OPEN;
        }

        final ErrorResponse error;
        {
            JsonObject e = in.get("error");
            if (e != null) {
                String msg = e.get("msg");
                String detail = e.get("detail");
                error = new ErrorResponse(msg, detail);
            } else {
                error = null;
            }
        }

        final NodeManager manager = link.getNodeManager();
        boolean closed = StreamState.CLOSED == stream;

        switch (method) {
            case "list":
                ListRequest listRequest = (ListRequest) request;
                Node node = manager.getNode(listRequest.getPath(), true).getNode();
                String path = node.getPath();
                SubscriptionManager subs = link.getSubscriptionManager();
                ListResponse listResp = new ListResponse(link, subs, rid, node, path);
                listResp.setError(error);
                listResp.populate(in);
                if (wrapper.listHandler != null) {
                    wrapper.listHandler.handle(listResp);
                }
                break;
            case "set":
                SetRequest setRequest = (SetRequest) request;
                path = setRequest.getPath();
                manager.getNode(path, true);
                SetResponse setResponse = new SetResponse(rid, link, path);
                setResponse.setError(error);
                setResponse.populate(in);
                if (wrapper.setHandler != null) {
                    wrapper.setHandler.handle(setResponse);
                }
                break;
            case "remove":
                RemoveRequest removeRequest = (RemoveRequest) request;
                NodePair pair = manager.getNode(removeRequest.getPath(), true);
                RemoveResponse removeResponse = new RemoveResponse(rid, pair);
                removeResponse.setError(error);
                removeResponse.populate(in);
                if (wrapper.removeHandler != null) {
                    wrapper.removeHandler.handle(removeResponse);
                }
                break;
            case "close":
                closed = true;
                break;
            case "subscribe":
                SubscribeResponse subResp = new SubscribeResponse(rid, link);
                subResp.setError(error);
                subResp.populate(in);
                break;
            case "unsubscribe":
                UnsubscribeResponse unsubResp = new UnsubscribeResponse(rid, link);
                unsubResp.setError(error);
                unsubResp.populate(in);
                if (wrapper.unsubHandler != null) {
                    wrapper.unsubHandler.handle(unsubResp);
                }
                break;
            case "invoke":
                InvokeRequest inReq = (InvokeRequest) request;
                path = inReq.getPath();
                manager.getNode(path, true);
                InvokeResponse inResp;
                synchronized (invokeResponses) {
                    switch (stream) {
                        case OPEN:
                            inResp = invokeResponses.get(rid);
                            break;
                        case INITIALIZED:
                            inResp = new InvokeResponse(link, rid, path);
                            invokeResponses.put(rid, inResp);
                            break;
                        case CLOSED:
                            inResp = invokeResponses.remove(rid);
                            break;
                        default:
                            inResp = null;
                    }
                    if (inResp == null) {
                        inResp = new InvokeResponse(link, rid, path);
                    }
                }
                inResp.setStreamState(stream);
                inResp.setError(error);
                inResp.populate(in);
                boolean invoke = false;
                if (inReq.waitForStreamClose()) {
                    if (closed) {
                        invoke = true;
                    }
                } else {
                    invoke = true;
                }
                if (invoke && wrapper.invokeHandler != null) {
                    wrapper.invokeHandler.handle(inResp);
                }
                break;
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }

        if (closed) {
            reqs.remove(rid);
        }
    }

    /**
     * Forcibly clears all subscriptions and handlers. This does not call
     * unsubscribe to the server.
     */
    public void clearSubscriptions() {
        subPaths.clear();
        subSids.clear();
        subUpdates.clear();
        invokeResponses.clear();
        if (subscriptionHelper != null) {
            subscriptionHelper.clear();
        }
    }

    private static class RequestWrapper {

        private final Request request;

        private Handler<InvokeResponse> invokeHandler;
        private Handler<ListResponse> listHandler;
        private Handler<RemoveResponse> removeHandler;
        private Handler<SetResponse> setHandler;
        private Handler<UnsubscribeResponse> unsubHandler;

        public RequestWrapper(Request request) {
            this.request = request;
        }

        public boolean shouldStore() {
            return !(invokeHandler == null
                    && listHandler == null
                    && removeHandler == null
                    && setHandler == null
                    && unsubHandler == null);
        }
    }
}

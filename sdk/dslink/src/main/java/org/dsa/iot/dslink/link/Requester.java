package org.dsa.iot.dslink.link;

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
import org.dsa.iot.dslink.util.Objects;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles incoming responses and outgoing requests.
 *
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private final Object subUpdateLock = new Object();
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
     * Constructs a requester
     *
     * @param handler Handler for callbacks and data handling
     */
    public Requester(DSLinkHandler handler) {
        super(handler);
        reqs = new ConcurrentHashMap<>();
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

    public void subscribe(String path, Handler<SubscriptionValue> onUpdate) {
        Set<String> paths = new HashSet<>();
        paths.add(path);
        subscribe(paths, onUpdate);
    }

    public void subscribe(Set<String> paths, Handler<SubscriptionValue> onUpdate) {
        if (paths == null) {
            throw new NullPointerException("paths");
        }
        Map<String, Integer> subs = new HashMap<>();
        int min = currentSubID.getAndAdd(paths.size());
        int max = min + paths.size();
        Iterator<String> it = paths.iterator();
        StringBuilder error = null;
        while (min < max && it.hasNext()) {
            try {
                String path = NodeManager.normalizePath(it.next(), true);
                subs.put(path, min);
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
        SubscribeRequest req = new SubscribeRequest(subs);

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
        wrapper.setUnsubHandler(onResponse);
        sendRequest(wrapper, currentReqID.incrementAndGet());
    }

    /**
     * Sends a request to the responder to close the given stream.
     *
     * @param rid Stream to close.
     * @param onResponse Response.
     */
    public void closeStream(int rid, Handler<CloseResponse> onResponse) {
        CloseRequest req = new CloseRequest();
        RequestWrapper wrapper = new RequestWrapper(req);
        wrapper.setCloseHandler(onResponse);
        sendRequest(wrapper, rid);

        reqs.remove(rid);
        if (onResponse != null) {
            wrapper.getCloseHandler().handle(new CloseResponse(rid, null));
        }
    }

    /**
     * Sends an invocation request.
     *
     * @param request Invocation request.
     * @param onResponse Response.
     */
    public void invoke(InvokeRequest request, Handler<InvokeResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.setInvokeHandler(onResponse);
        sendRequest(wrapper);
    }

    /**
     * Sends a list request.
     *
     * @param request List request.
     * @param onResponse Response.
     */
    public void list(ListRequest request, Handler<ListResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.setListHandler(onResponse);
        sendRequest(wrapper);
    }

    /**
     * Sends a set request.
     *
     * @param request Set request.
     * @param onResponse Response.
     */
    public void set(SetRequest request, Handler<SetResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.setSetHandler(onResponse);
        sendRequest(wrapper);
    }

    /**
     * Sends a remove request.
     *
     * @param request Remove request.
     * @param onResponse Called when a response is received.
     */
    public void remove(RemoveRequest request, Handler<RemoveResponse> onResponse) {
        RequestWrapper wrapper = new RequestWrapper(request);
        wrapper.setRemoveHandler(onResponse);
        sendRequest(wrapper);
    }

    /**
     * Sends a request to the client.
     *
     * @param wrapper Request to send to the client.
     */
    private void sendRequest(RequestWrapper wrapper) {
        int rid = currentReqID.incrementAndGet();
        sendRequest(wrapper, rid);
    }

    /**
     * Sends a request to the client with a given request ID.
     *
     * @param wrapper Request to send to the client
     * @param rid Request ID to use
     */
    private void sendRequest(RequestWrapper wrapper, int rid) {
        final DSLink link = getDSLink();
        if (link == null) {
            return;
        }
        Request request = wrapper.getRequest();
        JsonObject obj = new JsonObject();
        request.addJsonValues(obj);
        {
            obj.putNumber("rid", rid);
            reqs.put(rid, wrapper);
        }
        obj.putString("method", request.getName());
        link.getWriter().writeRequest(obj);
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
        int rid = in.getInteger("rid");
        NodeManager manager = link.getNodeManager();
        if (rid == 0) {
            final SubscriptionUpdate update = new SubscriptionUpdate(this);
            Objects.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (subUpdateLock) {
                        update.populate(in);
                    }
                }
            });
            return;
        }
        RequestWrapper wrapper = reqs.get(rid);
        Request request = wrapper.getRequest();
        String method = request.getName();

        final String stream = in.getString("stream");
        boolean closed = StreamState.CLOSED.getJsonName().equals(stream);

        switch (method) {
            case "list":
                ListRequest listRequest = (ListRequest) request;
                Node node = manager.getNode(listRequest.getPath(), true).getNode();
                SubscriptionManager subs = link.getSubscriptionManager();
                ListResponse resp = new ListResponse(link, subs, rid, node);
                resp.populate(in);
                if (wrapper.getListHandler() != null) {
                    wrapper.getListHandler().handle(resp);
                }
                break;
            case "set":
                SetRequest setRequest = (SetRequest) request;
                String path = setRequest.getPath();
                manager.getNode(path, true);
                SetResponse setResponse = new SetResponse(rid, link, path);
                setResponse.populate(in);
                if (wrapper.getSetHandler() != null) {
                    wrapper.getSetHandler().handle(setResponse);
                }
                break;
            case "remove":
                RemoveRequest removeRequest = (RemoveRequest) request;
                NodePair pair = manager.getNode(removeRequest.getPath(), true);
                RemoveResponse removeResponse = new RemoveResponse(rid, pair);
                removeResponse.populate(in);
                if (wrapper.getRemoveHandler() != null) {
                    wrapper.getRemoveHandler().handle(removeResponse);
                }
                break;
            case "close":
                break;
            case "subscribe":
                SubscribeResponse subResp = new SubscribeResponse(rid, link);
                subResp.populate(in);
                break;
            case "unsubscribe":
                UnsubscribeResponse unsubResp = new UnsubscribeResponse(rid, link);
                unsubResp.populate(in);
                if (wrapper.getUnsubHandler() != null) {
                    wrapper.getUnsubHandler().handle(unsubResp);
                }
                break;
            case "invoke":
                InvokeRequest inReq = (InvokeRequest) request;
                path = inReq.getPath();
                manager.getNode(inReq.getPath(), true);
                InvokeResponse inResp = new InvokeResponse(link, rid, path);
                inResp.populate(in);
                if (wrapper.getInvokeHandler() != null) {
                    wrapper.getInvokeHandler().handle(inResp);
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
    }

    private static class RequestWrapper {

        private final Request request;

        private Handler<CloseResponse> closeHandler;
        private Handler<InvokeResponse> invokeHandler;
        private Handler<ListResponse> listHandler;
        private Handler<RemoveResponse> removeHandler;
        private Handler<SetResponse> setHandler;
        private Handler<UnsubscribeResponse> unsubHandler;

        public RequestWrapper(Request request) {
            this.request = request;
        }

        public Request getRequest() {
            return request;
        }

        public Handler<CloseResponse> getCloseHandler() {
            return closeHandler;
        }

        public void setCloseHandler(Handler<CloseResponse> closeHandler) {
            this.closeHandler = closeHandler;
        }

        public Handler<InvokeResponse> getInvokeHandler() {
            return invokeHandler;
        }

        public void setInvokeHandler(Handler<InvokeResponse> invokeHandler) {
            this.invokeHandler = invokeHandler;
        }

        public Handler<ListResponse> getListHandler() {
            return listHandler;
        }

        public void setListHandler(Handler<ListResponse> listHandler) {
            this.listHandler = listHandler;
        }

        public Handler<RemoveResponse> getRemoveHandler() {
            return removeHandler;
        }

        public void setRemoveHandler(Handler<RemoveResponse> removeHandler) {
            this.removeHandler = removeHandler;
        }

        public Handler<SetResponse> getSetHandler() {
            return setHandler;
        }

        public void setSetHandler(Handler<SetResponse> setHandler) {
            this.setHandler = setHandler;
        }

        public Handler<UnsubscribeResponse> getUnsubHandler() {
            return unsubHandler;
        }

        public void setUnsubHandler(Handler<UnsubscribeResponse> unsubHandler) {
            this.unsubHandler = unsubHandler;
        }
    }
}

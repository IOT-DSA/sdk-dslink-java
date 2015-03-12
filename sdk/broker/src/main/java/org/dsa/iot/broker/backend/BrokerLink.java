package org.dsa.iot.broker.backend;

import lombok.SneakyThrows;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.Pair;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.connector.server.ServerClient;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.ClientConnectedEvent;
import org.dsa.iot.dslink.events.IncomingDataEvent;
import org.dsa.iot.dslink.events.InitializationEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Subscription;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.requester.requests.Request;
import org.dsa.iot.dslink.requester.requests.SubscribeRequest;
import org.dsa.iot.dslink.requester.requests.UnsubscribeRequest;
import org.dsa.iot.dslink.responder.Responder;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class BrokerLink extends DSLink {

    private final Map<String, ServerClient> connMap = new HashMap<>();
    private final Map<Request, Pair<Client, JsonObject>> reqMap = new HashMap<>();

    private final Node connections;

    @SneakyThrows
    BrokerLink(MBassador<Event> bus,
                ClientConnector clientConn,
                ServerConnector serverConn,
                Requester req, Responder resp) {
        super(bus, clientConn, serverConn, req, resp);
        
        val man = getNodeManager();
        connections = man.createRootNode("conns");
        man.createRootNode("def");
        man.createRootNode("quarantine"); // TODO: Zone management
    }

    @Override
    protected void init() {
        getBus().publish(new InitializationEvent(this, null));
    }

    @Handler
    @SneakyThrows
    public void onConnected(ClientConnectedEvent event) {
        val client = event.getClient();
        val dsId = client.getDsId();
        connections.createChild(dsId);
        connMap.put(dsId, client);
        System.out.println("Client connected: " + dsId);
    }

    @Handler
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
    
    @Handler
    public void jsonHandler(IncomingDataEvent event) {
        try {
            val client = event.getClient();
            val data = event.getData();
            handleRequests(client, data.getArray("requests"));
            handleResponses(client, data.getArray("responses"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleRequests(Client client, JsonArray array) {
        if (array == null)
            return;
        System.out.println("Requests: " + client.getDsId() + " <= " + array.encode());
        val it = array.iterator();
        val responses = new JsonArray();
        for (JsonObject obj; it.hasNext();) {
            obj = (JsonObject) it.next();
            if (!handledPathRequest(client, obj)) {
                handleSubscriptionRequest(client, obj);
                val out = new JsonObject();
                getResponder().handleRequest(client, obj, out);
                responses.add(out);
            }
        }

        if (responses.size() > 0) {
            val top = new JsonObject();
            top.putArray("responses", responses);
            client.write(top);
            System.out.println(client.getDsId() + " => " + top.encode());
        }
    }

    @SneakyThrows
    private void handleSubscriptionRequest(Client client, JsonObject obj) {
        val method = obj.getString("method");
        val paths = obj.getArray("paths");
        if (paths == null || method == null)
            return;

        val iter = paths.iterator();
        while (iter.hasNext()) {
            val path = (String) iter.next();
            String[] parts;
            if (path != null
                    && (parts = NodeManager.splitPath(path)).length > 1
                    && ("/" + parts[0]).equals(connections.getPath())) {
                iter.remove();
                val newPath = "/" + StringUtils.join(2, "/", parts);
                val responder = connMap.get(parts[1]);

                if (responder.isResponder()) {
                    val node = getNodeManager().getNode(path).getKey();
                    Request request = null;
                    if ("subscribe".equals(method)) {
                        request = new SubscribeRequest(newPath);
                        node.subscribe(new Subscription(client));
                    } else if ("unsubscribe".equals(method)) {
                        request = new UnsubscribeRequest(newPath);
                        node.unsubscribe(client);
                    }
                    if (request != null) {
                        getRequester().sendRequest(responder, request);
                    }
                }
            }
        }
    }

    @SneakyThrows
    private boolean handledPathRequest(Client client, JsonObject obj) {
        val path = obj.getString("path");
        String[] parts;
        if (path == null || (parts = NodeManager.splitPath(path)).length < 2)
            return false;
        if (("/" + parts[0]).equals(connections.getPath())) {
            val responder = connMap.get(parts[1]);
            if (responder.isResponder()) {
                val newObj = obj.copy();
                newObj.removeField("path");
                newObj.putString("path", "/" + StringUtils.join(2, "/", parts));

                val requester = getRequester();
                val request = requester.getRequest(newObj);
                if (request != null) {
                    int rid = requester.sendRequest(responder, request, false);
                    val track = requester.getRequest(obj);
                    responder.getRequestTracker().track(rid, track);
                    synchronized (this) {
                        reqMap.put(track, new Pair<>(client, obj));
                    }
                }
            }
        }
        return true;
    }

    private void handleResponses(Client client, JsonArray array) {
        if (array == null)
            return;
        System.out.println("Responses: " + client.getDsId() + " <= " + array.encode());
        val it = array.iterator();
        for (JsonObject o; it.hasNext();) {
            o = (JsonObject) it.next();
            
            val rid = o.getNumber("rid");
            if (rid != null && rid.intValue() == 0) {
                val newUpdates = new JsonArray();
                val updates = o.getArray("updates");
                o.removeField("updates");
                val iter = updates.iterator();
                for (JsonElement element; iter.hasNext();) {
                    element = (JsonElement) iter.next();
                    if (element.isArray()) {
                        val update = element.asArray();
                        val newUpdate = new JsonArray();
                        String path = update.get(0);
                        newUpdate.addString(buildPath(client, path));
                        newUpdate.add(update.get(1));
                        newUpdate.add(update.get(2));
                        
                        newUpdates.addArray(newUpdate);
                    } else if (element.isObject()) {
                        val update = element.asObject();
                        val path = update.getString("path");
                        update.removeField("path");
                        update.putString("path", buildPath(client, path));
                        newUpdates.addObject(update);
                    }
                }
                o.putArray("updates", newUpdates);
            }

            val resp = getRequester().handleResponse(client, o);
            val pair = reqMap.remove(resp.getRequest());
            if (pair != null) {
                val out = new JsonObject();

                val reqClient = pair.getKey();
                val orig = pair.getValue();
                getResponder().handleRequest(reqClient, orig, out);

                val responses = new JsonArray();
                responses.addObject(out);

                val top = new JsonObject();
                top.putArray("responses", responses);
                reqClient.write(top);
                System.out.println(reqClient.getDsId() + " => " + top.encode());
            }
            
        }
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private String buildPath(Client client, String path) {
        StringBuilder builder = new StringBuilder(connections.getPath());
        builder.append("/");
        builder.append(client.getDsId());
        builder.append(NodeManager.normalizePath(path, true));
        return builder.toString();
    }

    public static BrokerLink create(MBassador<Event> bus,
                                    ServerConnector server) {
        return create(bus, server, null);
    }

    public static BrokerLink create(MBassador<Event> bus,
                                    ServerConnector server,
                                    ClientConnector client) {
        val requester = new Requester(bus);
        val responder = new Responder(bus);
        return new BrokerLink(bus, client, server, requester, responder);
    }
}

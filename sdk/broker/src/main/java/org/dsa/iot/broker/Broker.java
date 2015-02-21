package org.dsa.iot.broker;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.ServerClient;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.ClientConnectedEvent;
import org.dsa.iot.dslink.events.RequestEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.requester.requests.ListRequest;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Broker {

    @Getter
    private final MBassador<Event> bus;

    @NonNull
    private final DSLink dslink;

    private final Map<String, WeakReference<ServerClient>> connMap = new HashMap<>();
    private final Map<Integer, RequestEvent> reqs = new HashMap<>();
    
    private final Node connections;
    private final Node defs;
    private final Node quarantine;

    public Broker(@NonNull MBassador<Event> master,
                  @NonNull DSLink link) {
        this.bus = master;
        this.dslink = link;
        bus.subscribe(this);

        val manager = dslink.getNodeManager();
        connections = manager.createRootNode("conns");
        defs = manager.createRootNode("def");
        // TODO: Zone management
        quarantine = manager.createRootNode("quarantine");
    }

    public void listen() {
        // TODO: Setup configurable ports
        listen(8080);
    }

    public void listen(int port) {
        System.out.println("Listening on port " + port);
        dslink.listen(port);
    }

    public void stop() {
        dslink.stopListening();
    }

    @Handler
    public synchronized void onRequest(final RequestEvent event) {
        event.setLocked(true);
        if (event.getMethod().equals("list")) {
            val path = event.getRequest().getString("path");
            if (path != null
                    && path.length() > connections.getPath().length() + 1
                    && path.startsWith(connections.getPath())) {
                path = path.substring(connections.getPath().length());
                val data = path.split("/");
                val dsId = data[0];
                
                WeakReference<ServerClient> ref = connMap.get(dsId);
                ServerClient client = ref.get();
                if (client != null && client.isResponder()) {
                    val req = new ListRequest(StringUtils.join(1, data));
                    val gid = dslink.getRequester().sendRequest(client, req);
                    reqs.put(gid, event);
                } else {
                    connMap.remove(dsId);
                }
            }
        }
    }
    
    @Handler
    public void onResponse(ResponseEvent event) {
        val req = reqs.get(event.getGid());
        if (req != null) {
            req.setLocked(false);
            req.call();
        }
    }

    @Handler
    public void onConnected(ClientConnectedEvent event) {
        val client = event.getClient();
        val dsId = client.getDsId();
        connections.createChild(dsId);
        connMap.put(dsId, new WeakReference<>(client));
    }
    
    @Handler
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
}

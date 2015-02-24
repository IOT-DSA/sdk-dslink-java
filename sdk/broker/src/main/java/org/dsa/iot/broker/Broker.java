package org.dsa.iot.broker;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.broker.events.ListResponseEvent;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.ServerClient;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.ClientConnectedEvent;
import org.dsa.iot.dslink.events.RequestEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.requester.requests.ListRequest;

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

    private final Map<String, ServerClient> connMap = new HashMap<>(); // TODO: weak value references
    private final Map<Integer, RequestEvent> reqs = new HashMap<>(); // TODO: merge two maps into 1
    private final Map<Integer, String> prefixes = new HashMap<>();
    
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
    public synchronized void onRequest(RequestEvent event) {
        if (event.getMethod().equals("list")) {
            String path = event.getRequest().getString("path");
            if (path != null
                    && path.length() > connections.getPath().length() + 1
                    && path.startsWith(connections.getPath())) {
                event.setLocked(true);
                path = path.substring(connections.getPath().length() + 1);
                val data = path.split("/");
                val dsId = data[0];
                
                ServerClient client = connMap.get(dsId);
                if (client != null && client.isResponder()) {
                    String clientPath = StringUtils.join(1, data);
                    if (clientPath.isEmpty())
                        clientPath = "/";
                    val req = new ListRequest(clientPath);
                    val gid = dslink.getRequester().sendRequest(client, req);
                    reqs.put(gid, event);
                    prefixes.put(gid, "/conns/" + dsId);
                }
            }
        }
    }
    
    @Handler
    public void onResponse(ListResponseEvent event) {
        val prefix = prefixes.get(event.getGid());
        val req = reqs.remove(event.getGid());
        if (req != null) {
            event.setPrefix(prefix);
            req.setLocked(false);
            req.call();
        }
    }

    @Handler
    public void onConnected(ClientConnectedEvent event) {
        val client = event.getClient();
        val dsId = client.getDsId();
        connections.createChild(dsId);
        connMap.put(dsId, client);
    }
    
    @Handler
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
}

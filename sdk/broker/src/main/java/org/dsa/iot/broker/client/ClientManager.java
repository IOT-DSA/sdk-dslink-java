package org.dsa.iot.broker.client;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class ClientManager {

    private final Object lock = new Object();
    private Map<String, Client> pendingClients = new HashMap<>();
    private Map<String, Client> connectedClients = new HashMap<>();

    public void clientConnecting(Client client) {
        if (client == null) {
            throw new NullPointerException("client");
        }
        synchronized (lock) {
            String dsId = client.getDsId();
            pendingClients.put(dsId, client);
        }
    }

    public void clientConnected() {
        throw new UnsupportedOperationException();
    }

    public Client getPendingClient(String dsId) {
        synchronized (lock) {
            return pendingClients.get(dsId);
        }
    }
}

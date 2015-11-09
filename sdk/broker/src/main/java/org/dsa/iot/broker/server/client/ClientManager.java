package org.dsa.iot.broker.server.client;

import org.dsa.iot.dslink.util.Objects;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class ClientManager {

    private final Object lock = new Object();
    private Map<String, TimedClient> pendingClients = new HashMap<>();
    private Map<String, Client> connectedClients = new HashMap<>();

    public void clientConnecting(Client client) {
        if (client == null) {
            throw new NullPointerException("client");
        }
        synchronized (lock) {
            String key = client.handshake().dsId();
            TimedClient value = new TimedClient(key, client);
            value.start(30, TimeUnit.SECONDS);
            pendingClients.put(key, value);
        }
    }

    public void clientConnected(Client client) {
        synchronized (lock) {
            String dsId = client.handshake().dsId();
            TimedClient c = pendingClients.remove(dsId);
            if (c != null) {
                c.expireEarly();
            }

            {
                Client old = connectedClients.put(dsId, client);
                if (old != null) {
                    old.close();
                }
            }
        }
        client.broker().tree().connected(client);
    }

    public void clientDisconnected(Client client) {
        synchronized (lock) {
            String dsId = client.handshake().dsId();
            Client old = connectedClients.remove(dsId);
            if (old != null) {
                old.close();
            }
        }
        client.broker().tree().disconnected(client);
    }

    public Client getPendingClient(String dsId) {
        synchronized (lock) {
            TimedClient c = pendingClients.get(dsId);
            return c != null ? c.value : null;
        }
    }

    private class TimedClient {

        private String key;
        private Client value;

        private ScheduledFuture<?> fut;

        public TimedClient(String key, Client value) {
            this.key = key;
            this.value = value;

        }

        public void start(long time, TimeUnit unit) {
            fut = Objects.getDaemonThreadPool().schedule(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        pendingClients.remove(key);
                    }
                    expireEarly();
                }
            }, time, unit);
        }

        public void expireEarly() {
            if (fut != null) {
                fut.cancel(true);
            }
            fut = null;
            key = null;
            value = null;
        }
    }
}

package org.dsa.iot.dslink.connection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

/**
 * Used for handling servers.
 * @author Samuel Grenier
 */
@AllArgsConstructor
public abstract class ServerConnector {

    @Getter
    @NonNull
    private final HandshakeClient client;

    public abstract void start(int port, String bindAddr);

    public abstract void stop();
}

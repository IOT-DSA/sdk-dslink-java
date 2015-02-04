package org.dsa.iot.dslink.connection;

import lombok.*;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

/**
 * Used for handling servers.
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public abstract class ServerConnector {

    @Getter
    @NonNull
    private final HandshakeClient client;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean listening = false;

    public abstract void start(int port, String bindAddr);

    public abstract void stop();
}

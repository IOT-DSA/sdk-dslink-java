package org.dsa.iot.dslink.connection;

import com.google.common.eventbus.EventBus;
import lombok.*;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

/**
 * Used for handling servers.
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public abstract class ServerConnector {

    @NonNull
    private final EventBus bus;

    @NonNull
    private final HandshakeClient client;

    @Setter(AccessLevel.PROTECTED)
    private boolean listening = false;

    public abstract void start(int port, String bindAddr);

    public abstract void stop();
}

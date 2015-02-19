package org.dsa.iot.dslink.connection;

import lombok.*;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.core.event.Event;

/**
 * Used for handling servers.
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public abstract class ServerConnector {

    @NonNull
    private final MBassador<Event> bus;

    @NonNull
    private final HandshakeClient client;

    @Setter(AccessLevel.PROTECTED)
    private boolean listening = false;

    public abstract void start(int port, String bindAddr);

    public abstract void stop();
}

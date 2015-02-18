package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.connector.server.ServerClient;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class ClientConnectedEvent extends Event {
    
    @NonNull
    private final ServerClient client;
    
}

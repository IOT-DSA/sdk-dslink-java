package org.dsa.iot.dslink.connection.handshake;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class HandshakePair {

    private final HandshakeClient client;
    private final HandshakeServer server;

}

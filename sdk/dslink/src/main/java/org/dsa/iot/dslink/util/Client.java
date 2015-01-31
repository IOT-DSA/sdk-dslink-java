package org.dsa.iot.dslink.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Client accepted from the server
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class Client {

    private final String dsId;
    private final byte[] decryptedNonce;

    private final String salt;
    private String saltS;

}

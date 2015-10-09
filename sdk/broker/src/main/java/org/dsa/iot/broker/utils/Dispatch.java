package org.dsa.iot.broker.utils;

import org.dsa.iot.broker.client.Client;

/**
 * @author Samuel Grenier
 */
public class Dispatch {

    private final Client client;
    private final int rid;

    public Dispatch(Client client, int rid) {
        this.client = client;
        this.rid = rid;
    }

    public Client getClient() {
        return client;
    }

    public int getRid() {
        return rid;
    }
}

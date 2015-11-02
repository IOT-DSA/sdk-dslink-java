package org.dsa.iot.broker.processor.methods;

import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Response {

    public abstract JsonObject getResponse(Client client, int rid);

}

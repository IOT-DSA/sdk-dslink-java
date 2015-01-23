package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.util.Linkable;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class Requester implements Linkable {

    private Connector connector;

    @Override
    public void parse(JsonArray array) {

    }

    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
    }
}

package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.connection.Connector;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public interface Linkable {

    public void parse(JsonArray array);

    /**
     * A link must hold onto this in order to check for connectivity
     * and write to the server.
     * @param connector Connector to be set.
     */
    public void setConnector(Connector connector);
}

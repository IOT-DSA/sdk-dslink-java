package org.dsa.iot.responder;

import org.dsa.iot.responder.connection.Connector;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Samuel Grenier
 */
public class Responder {

    private Connector connector;
    private boolean connected;

    /**
     * Used to set the connector implementation.
     * @param connector Connector instance to use
     */
    public void setConnector(Connector connector) {
        checkConnected();
        this.connector = connector;
    }

    public void connect() throws IOException {
        connect(true);
    }

    /**
     * Performs the connection
     * @param sslVerify Whether to verify ssl if attempting to connect over
     *                  secure communications.
     * @throws IOException
     */
    public synchronized void connect(boolean sslVerify) throws IOException {
        checkConnected();
        checkConnector();
        connector.connect(new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                parse(event);
            }
        }, new Handler<Void>() {
            @Override
            public void handle(Void event) {
                synchronized (Responder.this) {
                    connected = false;
                }
            }
        }, sslVerify);
        connected = true;
    }

    public synchronized void disconnect() {
        connector.disconnect();
        connected = false;
    }

    /**
     * Parse the object for requests
     * @param object Object to parse
     */
    protected void parse(JsonObject object) {
        JsonArray array = object.getObject("requests").asArray();

        @SuppressWarnings("unchecked")
        Iterator<JsonObject> it = (Iterator) array.iterator();

        for (JsonObject o = null; it.hasNext();) {
            o = it.next();

            // TODO: handle each request
        }
    }

    private synchronized void checkConnected() {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }
    }

    private void checkConnector() {
        if (connector == null) {
             throw new IllegalStateException("Connector not set");
        }
    }
}

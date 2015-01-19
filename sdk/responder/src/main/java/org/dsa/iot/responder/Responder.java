package org.dsa.iot.responder;

import org.dsa.iot.responder.connection.Connector;
import org.dsa.iot.responder.methods.*;
import org.dsa.iot.responder.node.Node;
import org.dsa.iot.responder.node.NodeManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import static org.dsa.iot.responder.methods.Method.StreamState;

/**
 * @author Samuel Grenier
 */
public class Responder {

    private final NodeManager manager = new NodeManager();

    private Connector connector;
    private boolean connected;

    public void createRoot(String name) {
        checkConnected();
        manager.createRootNode(name);
    }

    public void addRoot(Node node) {
        checkConnected();
        manager.addRootNode(node);
    }

    /**
     * Used to set the connector implementation.
     * @param connector Connector instance to use
     */
    public void setConnector(Connector connector) {
        checkConnected();
        this.connector = connector;
    }

    /**
     * Performs the connection to the server. If attempting to connect over
     * secure communications, the SSL certificate of the server will be validated.
     * @throws IOException
     * @see #connect(boolean)
     */
    public void connect() throws IOException {
        connect(true);
    }

    /**
     * Performs the connection to the server.
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
    protected synchronized void parse(JsonObject object) {
        JsonArray array = object.getObject("requests").asArray();

        JsonObject top = new JsonObject();

        @SuppressWarnings("unchecked")
        Iterator<JsonObject> it = (Iterator) array.iterator();
        JsonArray responses = new JsonArray();
        for (JsonObject o; it.hasNext();) {
            o = it.next();

            Number rid = o.getNumber("rid");
            String sMethod = o.getString("method");
            String path = o.getString("path");

            Method method;
            switch (sMethod) {
                case "list":
                    Node node = manager.getNode(path);
                    method = new ListMethod(node);
                    break;
                case "set":
                    method = new SetMethod();
                    break;
                case "remove":
                    method = new RemoveMethod();
                    break;
                case "invoke":
                    method = new InvokeMethod();
                    break;
                case "subscribe":
                    method = new SubscribeMethod();
                    break;
                case "unsubscribe":
                    method = new UnsubscribeMethod();
                    break;
                case "close":
                    method = new CloseMethod();
                    break;
                default:
                    throw new RuntimeException("Unknown method");
            }

            JsonObject resp = new JsonObject();
            try {
                resp.putNumber("rid", rid);

                JsonObject updates = method.invoke();
                StreamState state = method.getState();

                if (state == null) {
                    throw new IllegalStateException("state");
                }
                if (state != StreamState.INITIALIZED) {
                    // This is a first response, default value is initialized if omitted
                    resp.putString("stream", state.jsonName);
                }

                if (updates != null) {
                    resp.putElement("update", updates);
                }
            } catch (Exception e) {
                resp.putString("stream", StreamState.CLOSED.jsonName);

                JsonObject error = new JsonObject();
                error.putString("msg", e.getMessage());

                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                error.putString("detail", writer.toString());

                resp.putElement("error", error);
            } finally {
                responses.addElement(resp);
            }
        }

        top.putElement("responses", responses);
        connector.write(top);
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

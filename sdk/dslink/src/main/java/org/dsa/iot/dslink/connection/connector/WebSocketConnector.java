package org.dsa.iot.dslink.connection.connector;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.util.HttpClientUtils;
import org.dsa.iot.dslink.util.IntervalTaskManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * Handles connecting to web socket servers.
 *
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private IntervalTaskManager<JsonObject> requests;
    private IntervalTaskManager<JsonObject> responses;

    private WebSocket webSocket;
    private Handler<JsonArray> requestHandler;
    private Handler<JsonArray> responseHandler;
    private Handler<NetworkClient> clientHandler;
    private boolean isActive;

    @Override
    public void init() {
        int updateInterval = getRemoteHandshake().getUpdateInterval();
        requests = new IntervalTaskManager<>(updateInterval, new Handler<List<JsonObject>>() {
            @Override
            public void handle(List<JsonObject> event) {
                checkConnected();
                JsonObject top = new JsonObject();
                top.putArray("requests", toArray(event));
                webSocket.writeTextFrame(top.encode());
            }
        });

        responses = new IntervalTaskManager<>(updateInterval, new Handler<List<JsonObject>>() {
            @Override
            public void handle(List<JsonObject> event) {
                checkConnected();
                JsonObject top = new JsonObject();
                top.putArray("responses", toArray(event));
                webSocket.writeTextFrame(top.encode());
            }
        });
    }

    @Override
    public void activate() {
        HttpClient client = HttpClientUtils.configure(getEndpoint());
        client.connectWebsocket(getUri(), new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                webSocket = event;
                isActive = true;

                Handler<NetworkClient> handler = clientHandler;
                if (handler != null) {
                    handler.handle(WebSocketConnector.this);
                }

                event.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        event.printStackTrace();
                    }
                });

                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        Handler<JsonArray> reqHandler = requestHandler;
                        Handler<JsonArray> respHandler = responseHandler;

                        String string = event.toString("UTF-8");
                        JsonObject obj = new JsonObject(string);

                        JsonArray requests = obj.getArray("requests");
                        if (!(reqHandler == null || requests == null)) {
                            reqHandler.handle(requests);
                        }

                        JsonArray responses = obj.getArray("responses");
                        if (!(respHandler == null || responses == null)) {
                            respHandler.handle(responses);
                        }
                    }
                });

                event.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        // TODO: implement HTTP fallback
                        isActive = false;
                    }
                });
            }
        });
    }

    @Override
    public void deactivate() {
        if (webSocket != null) {
            webSocket.close();
        }
    }

    @Override
    public void writeRequest(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        requests.post(object);
    }

    @Override
    public void writeResponse(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        responses.post(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeResponses(List<JsonObject> objects) {
        if (objects == null) {
            throw new NullPointerException("objects");
        }
        responses.post(objects);
    }

    @Override
    public void setClientConnectedHandler(Handler<NetworkClient> handler) {
        this.clientHandler = handler;
    }

    @Override
    public void setRequestDataHandler(Handler<JsonArray> handler) {
        if (!isResponder()) {
            throw new RuntimeException("This client is not a responder");
        }
        this.requestHandler = handler;
    }

    @Override
    public void setResponseDataHandler(Handler<JsonArray> handler) {
        if (!isRequester()) {
            throw new RuntimeException("This client is not a requester");
        }
        this.responseHandler = handler;
    }

    @Override
    public boolean isRequester() {
        return getLocalHandshake().isRequester();
    }

    @Override
    public boolean isResponder() {
        return getLocalHandshake().isResponder();
    }

    private JsonArray toArray(List<JsonObject> objs) {
        JsonArray array = new JsonArray();
        for (JsonObject obj : objs) {
            array.addObject(obj);
        }
        return array;
    }

    private void checkConnected() {
        if (!isActive) {
            throw new RuntimeException("Cannot write to unconnected connection");
        }
    }
}

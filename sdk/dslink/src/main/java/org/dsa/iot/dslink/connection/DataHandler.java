package org.dsa.iot.dslink.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collection;
import java.util.List;

/**
 * Handles all incoming and outgoing data in a network endpoint.
 *
 * @author Samuel Grenier
 */
public class DataHandler {

    private static final Logger LOGGER;

    private final IntervalUpdateManager requests;
    private final IntervalUpdateManager responses;

    private NetworkClient client;
    private Handler<JsonArray> reqHandler;
    private Handler<JsonArray> respHandler;

    public DataHandler(int updateInterval) {
        requests = getIntervalHandler(updateInterval, "requests");
        responses = getIntervalHandler(updateInterval, "responses");
    }

    public void setClient(NetworkClient client) {
        this.client = client;
    }

    public void setReqHandler(Handler<JsonArray> handler) {
        this.reqHandler = handler;
    }

    public void setRespHandler(Handler<JsonArray> handler) {
        this.respHandler = handler;
    }

    /**
     * Processes incoming data from a remote endpoint.
     *
     * @param buffer Buffer to process
     */
    public void processData(Buffer buffer) {
        String string = buffer.toString("UTF-8");
        JsonObject obj = new JsonObject(string);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received data: {}", obj.encode());
        }

        JsonArray requests = obj.getArray("requests");
        if (!(reqHandler == null || requests == null)) {
            reqHandler.handle(requests);
        }

        JsonArray responses = obj.getArray("responses");
        if (!(respHandler == null || responses == null)) {
            respHandler.handle(responses);
        }
    }

    public void writeRequest(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        requests.post(object);
    }

    public void writeResponse(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        responses.post(object);
    }

    public void writeResponses(List<JsonObject> objects) {
        if (objects == null) {
            throw new NullPointerException("objects");
        }
        responses.post(objects);
    }

    private IntervalUpdateManager getIntervalHandler(int updateInterval,
                                                               final String name) {
        return new IntervalUpdateManager(updateInterval,
                new Handler<Collection<JsonObject>>() {
                    @Override
                    public void handle(Collection<JsonObject> event) {
                        JsonObject top = new JsonObject();

                        JsonArray array = new JsonArray();
                        for (JsonObject obj : event) {
                            array.addObject(obj);
                        }

                        top.putArray(name, array);
                        String encoded = top.encode();

                        if (client.isConnected()) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Sent data: {}", encoded);
                            }
                            client.write(encoded);
                        }
                    }
                });
    }

    static {
        LOGGER = LoggerFactory.getLogger(DataHandler.class);
    }
}

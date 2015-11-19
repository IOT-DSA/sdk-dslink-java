package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Handles all incoming and outgoing data in a network endpoint.
 *
 * @author Samuel Grenier
 */
public class DataHandler {

    private static final Logger LOGGER;

    private NetworkClient client;
    private Handler<DataReceived> reqHandler;
    private Handler<DataReceived> respHandler;

    private QueuedWriteManager requestsManager;
    private QueuedWriteManager responsesManager;

    public void setClient(NetworkClient client) {
        this.client = client;
        this.requestsManager = new QueuedWriteManager(client, "requests");
        this.responsesManager = new QueuedWriteManager(client, "responses");
    }

    public void setReqHandler(Handler<DataReceived> handler) {
        this.reqHandler = handler;
    }

    public void setRespHandler(Handler<DataReceived> handler) {
        this.respHandler = handler;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Processes incoming data from a remote endpoint.
     *
     * @param obj JSON object to process.
     */
    public void processData(JsonObject obj) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received data: {}", obj.encode());
        }


        final Integer msgId = obj.get("msg");
        final JsonArray requests = obj.get("requests");
        if (!(reqHandler == null || requests == null)) {
            Objects.getDaemonThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    reqHandler.handle(new DataReceived(msgId, requests));
                }
            });
        }

        final JsonArray responses = obj.get("responses");
        if (!(respHandler == null || responses == null)) {
            Objects.getDaemonThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    respHandler.handle(new DataReceived(msgId, responses));
                }
            });
        }
    }

    public void writeRequest(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        requestsManager.post(object);
    }

    public void writeAck(Integer ack) {
        if (ack == null) {
            return;
        }
        JsonObject obj = new JsonObject();
        obj.put("ack", ack);
        client.write(obj.encode());
    }

    /**
     * Writes a response that is not tied to any message. These responses can
     * be throttled to prevent flooding.
     *
     * @param object Data to write.
     */
    public void writeResponse(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }

        writeRequestResponses(null, Collections.singleton(object));
    }

    /**
     * Writes all the responses back out that the requester requested.
     *
     * @param ackId Acknowledgement ID.
     * @param objects Responses to write.
     */
    public void writeRequestResponses(Integer ackId,
                                      Collection<JsonObject> objects) {
        if (objects == null) {
            throw new NullPointerException("objects");
        }

        responsesManager.post(objects);
        if (ackId != null) {
            JsonObject ack = new JsonObject();
            ack.put("ack", ackId);
            client.write(ack.encode());
        }
    }

    public static class DataReceived {

        private final Integer msgId;
        private final JsonArray data;

        public DataReceived(Integer msgId, JsonArray data) {
            this.msgId = msgId;
            this.data = data;
        }

        public Integer getMsgId() {
            return msgId;
        }

        public JsonArray getData() {
            return data;
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(DataHandler.class);
    }
}

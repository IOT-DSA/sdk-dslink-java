package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.EncodingFormat;
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
public class DataHandler implements MessageTracker {

    private static final Logger LOGGER;

    private final Object msgLock = new Object();
    private int messageId = 0;
    private int lastReceivedAck = 0;

    private EncodingFormat format;
    private NetworkClient client;

    private Handler<DataReceived> reqHandler;
    private Handler<DataReceived> respHandler;

    private QueuedWriteManager reqsManager;
    private QueuedWriteManager respsManager;

    public void setClient(NetworkClient client,
                          EncodingFormat format) {
        this.client = client;
        this.format = format;
        this.reqsManager = new QueuedWriteManager(client, this, format, "requests");
        this.respsManager = new QueuedWriteManager(client, this, format, "responses");
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
            String f = format.toJson();
            LOGGER.debug("Received data ({}): {}", f, obj);
        }

        final Integer msgId = obj.get("msg");
        final JsonArray requests = obj.get("requests");
        if (!(reqHandler == null || requests == null)) {
            LoopProvider.getProvider().schedule(new Runnable() {
                @Override
                public void run() {
                    reqHandler.handle(new DataReceived(msgId, requests));
                }
            });
        }

        final JsonArray responses = obj.get("responses");
        if (!(respHandler == null || responses == null)) {
            respHandler.handle(new DataReceived(msgId, responses));
        }

        final Integer ackId = obj.get("ack");
        if (ackId != null) {
            ackReceived(ackId);
        }
    }

    public void writeRequest(JsonObject object, boolean merge) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        reqsManager.post(object, merge);
    }

    public void writeAck(Integer ack) {
        if (ack == null) {
            return;
        }
        JsonObject obj = new JsonObject();
        obj.put("ack", ack);
        client.write(format, obj);
    }

    /**
     * Writes a response that is not tied to any message. These responses can
     * be throttled to prevent flooding.
     *
     * @param object Data to write.
     */
    public void writeResponse(JsonObject object) {
        writeResponse(object,true);
    }

    /**
     * Writes a response that is not tied to any message. These responses can
     * be throttled to prevent flooding.
     *
     * @param object Data to write.
     * @param merge Whether or not the response can be merged with other messages for the same request.
     */
    public void writeResponse(JsonObject object, boolean merge) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        respsManager.post(object, merge);
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

        for (JsonObject o : objects) {
            respsManager.post(o, true);
        }
        if (ackId != null) {
            JsonObject ack = new JsonObject();
            ack.put("ack", ackId);
            client.write(format, ack);
        }
    }

    @Override
    public void ackReceived(int ack) {
        synchronized (msgLock) {
            lastReceivedAck = Math.max(lastReceivedAck, ack);
        }
    }

    @Override
    public int missingAckCount() {
        synchronized (msgLock) {
            return messageId - lastReceivedAck;
        }
    }

    @Override
    public int incrementMessageId() {
        synchronized (msgLock) {
            return ++messageId;
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

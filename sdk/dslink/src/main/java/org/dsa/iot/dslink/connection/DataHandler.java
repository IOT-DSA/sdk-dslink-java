package org.dsa.iot.dslink.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles all incoming and outgoing data in a network endpoint.
 *
 * @author Samuel Grenier
 */
public class DataHandler {

    private static final Logger LOGGER;

    private final Object idLock = new Object();
    private int msgId = 0;
    private int lastAckId = 0;

    private final Object queueLock = new Object();
    private final Queue<JsonObject> queue = new LinkedBlockingQueue<>();

    private NetworkClient client;
    private Handler<DataReceived> reqHandler;
    private Handler<DataReceived> respHandler;
    private Handler<Void> closeHandler;

    public void setClient(NetworkClient client) {
        this.client = client;
    }

    public void setReqHandler(Handler<DataReceived> handler) {
        this.reqHandler = handler;
    }

    public void setRespHandler(Handler<DataReceived> handler) {
        this.respHandler = handler;
    }

    public void setCloseHandler(Handler<Void> handler) {
        this.closeHandler = handler;
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

        Integer msgId = obj.getInteger("msg");
        JsonArray requests = obj.getArray("requests");
        if (!(reqHandler == null || requests == null)) {
            reqHandler.handle(new DataReceived(msgId, requests));
        }

        JsonArray responses = obj.getArray("responses");
        if (!(respHandler == null || responses == null)) {
            // TODO: handle ack
            respHandler.handle(new DataReceived(msgId, responses));
        }

        Integer ack = obj.getInteger("ack");
        if (ack != null) {
            synchronized (idLock) {
                if (lastAckId < ack) {
                    lastAckId = ack;
                }
            }

            synchronized (queueLock) {
                while (true) {
                    synchronized (idLock) {
                        if (!(msgId - lastAckId < 8)) {
                            break;
                        }
                    }
                    JsonObject data = queue.poll();
                    if (data == null) {
                        break;
                    }
                    client.write(data.encode());
                }
            }
        }
    }

    public void close() {
        Handler<Void> closeHandler = this.closeHandler;
        if (closeHandler != null) {
            closeHandler.handle(null);
        }
    }

    public void writeRequest(JsonObject object) {
        if (object == null) {
            throw new NullPointerException("object");
        }
        JsonArray reqs = new JsonArray();
        reqs.addObject(object);
        JsonObject top = new JsonObject();
        top.putArray("requests", reqs);
        client.write(top.encode());
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

        JsonArray responses = new JsonArray();
        responses.addObject(object);
        JsonObject top = new JsonObject();
        top.putArray("responses", responses);
        boolean queue;
        synchronized (idLock) {
            int id = msgId++;
            top.putNumber("msg", id);
            // Compare whether the handler received enough acks to continue
            // writing
            queue = id - lastAckId < 8;
        }

        if (queue) {
            synchronized (queueLock) {
                this.queue.add(top);
            }
        } else {
            client.write(top.encode());
        }
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

        JsonArray responses = new JsonArray();
        for (JsonObject obj : objects) {
            responses.addObject(obj);
        }
        JsonObject top = new JsonObject();
        top.putArray("responses", responses);
        if (ackId != null) {
            top.putNumber("ack", ackId);
        }
        client.write(top.encode());
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

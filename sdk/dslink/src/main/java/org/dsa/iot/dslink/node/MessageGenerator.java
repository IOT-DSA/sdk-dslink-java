package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Used by subscriptions to achieve proper QOS, but could be used by any responder message that
 * want to delay encoding until actually being written to the output stream.
 */
public interface MessageGenerator {

    /**
     * The source should provide the message to send here, or null to send nothing.  The returned
     * message will not be queued, that is the responsibility of the MessageGenerator.
     *
     * @param lastAckId    The last ack received, can be used for qos.
     * @return The message to send, or null to not send anything.
     */
    public JsonObject getMessage(int lastAckId);

    /**
     * The message couldn't be sent, so it should requeue and try again.
     */
    public void retry();

    /**
     * If a message was returned from getMessage, this will be it's message id.
     */
    public void setMessageId(int messageId);

}

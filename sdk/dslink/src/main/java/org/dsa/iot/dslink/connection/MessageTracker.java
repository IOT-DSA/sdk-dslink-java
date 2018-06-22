package org.dsa.iot.dslink.connection;

/**
 * Keeps track of outgoing message IDs and incoming acks from and to the
 * network.
 *
 * @author Samuel Grenier
 */
public interface MessageTracker {

    /**
     * An ack has been received from the network. This directly affects the
     * missing ack count.
     *
     * @param ack Received ack ID.
     */
    void ackReceived(int ack);

    /**
     * Increments the message ID. This directly affects the missing ack
     * count.
     *
     * @return Next message ID.
     */
    int incrementMessageId();

    /**
     * The last ack received from the network.
     */
    int lastAckReceived();

    /**
     * Retrieves the amount of missing acks. This can be used for network
     * throttling if their is too many missing acks.
     *
     * @return Missing acks from the network.
     */
    int missingAckCount();
}

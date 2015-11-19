package org.dsa.iot.dslink.connection;

/**
 * @author Samuel Grenier
 */
public interface MessageTracker {

    void ackReceived(int ack);

    int missingAckCount();

    int incrementMessageId();
}

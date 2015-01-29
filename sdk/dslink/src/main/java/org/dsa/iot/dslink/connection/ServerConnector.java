package org.dsa.iot.dslink.connection;

/**
 * Used for handling servers.
 * @author Samuel Grenier
 */
public abstract class ServerConnector {

    public abstract void start(int port, String bindAddr);

    public abstract void stop();
}

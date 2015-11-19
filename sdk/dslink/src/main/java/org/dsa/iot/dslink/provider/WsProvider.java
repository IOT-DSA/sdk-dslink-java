package org.dsa.iot.dslink.provider;

import org.dsa.iot.dslink.provider.netty.DefaultWsProvider;
import org.dsa.iot.dslink.util.http.WsClient;

/**
 * @author Samuel Grenier
 */
public abstract class WsProvider {

    private static WsProvider PROVIDER;

    public static WsProvider getProvider() {
        if (PROVIDER == null) {
            setProvider(new DefaultWsProvider());
        }
        return PROVIDER;
    }

    public static void setProvider(WsProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        PROVIDER = provider;
    }

    public abstract void connect(WsClient client);

    /**
     * Handles writing data over the network.
     */
    public interface Writer {

        /**
         * @return Whether the data can be written to the network immediately.
         */
        boolean writable();

        /**
         * @param data Data to write to the network
         */
        void write(String data);

        /**
         * Closes the connection to the remote endpoint.
         */
        void close();
    }
}

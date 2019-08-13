package org.dsa.iot.dslink.provider;

import org.dsa.iot.dslink.provider.netty.DefaultWsProvider;
import org.dsa.iot.dslink.util.http.WsClient;

/**
 * @author Samuel Grenier
 */
public abstract class WsProvider {

    private static WsProvider PROVIDER;
    private boolean useCompression = true;

    public abstract void connect(WsClient client);

    public static WsProvider getProvider() {
        if (PROVIDER == null) {
            setProvider(new DefaultWsProvider());
        }
        return PROVIDER;
    }

    public boolean getUseCompression() {
        return useCompression;
    }

    public static void setProvider(WsProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        PROVIDER = provider;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

}

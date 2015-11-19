package org.dsa.iot.dslink.util.http;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.provider.WsProvider;
import org.dsa.iot.dslink.util.URLInfo;

/**
 * @author Samuel Grenier
 */
public abstract class WsClient {

    private final URLInfo url;

    public WsClient(URLInfo url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        this.url = url;
    }

    public URLInfo getUrl() {
        return url;
    }

    public void connect() {
        WsProvider.getProvider().connect(this);
    }

    public abstract void onData(String data);

    public abstract void onConnected(NetworkClient writer);

    public abstract void onDisconnected();

    public abstract void onThrowable(Throwable throwable);
}

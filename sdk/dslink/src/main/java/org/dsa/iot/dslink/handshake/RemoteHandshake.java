package org.dsa.iot.dslink.handshake;

import org.dsa.iot.dslink.util.HttpClientUtils;
import org.dsa.iot.dslink.util.URLInfo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonObject;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

/**
 * Handshake information retrieved from the server.
 *
 * @author Samuel Grenier
 */
public class RemoteHandshake {

    private final RemoteKey remoteKey;
    private final String dsId;
    private final String publicKey;
    private final String wsUri;
    private final String httpUri;
    private final String salt;
    private String saltS;
    private String saltL;
    private final int updateInterval;

    /**
     * Populates the handshake with data from the server.
     *
     * @param keys Local client keys necessary to create the remote key.
     * @param in   JSON object retrieved from the server.
     */
    public RemoteHandshake(LocalKeys keys, JsonObject in) {
        String tempKey = in.getString("tempKey");
        this.remoteKey = RemoteKey.generate(keys, tempKey);
        this.dsId = in.getString("dsId");
        this.publicKey = in.getString("publicKey");
        this.wsUri = in.getString("wsUri");
        this.httpUri = in.getString("httpUri");
        this.salt = in.getString("salt");
        this.saltS = in.getString("saltS");
        this.saltL = in.getString("saltL");

        Number ui = in.getNumber("updateInterval");
        if (ui != null) {
            updateInterval = ui.intValue();
        } else {
            updateInterval = 0;
        }
    }

    /**
     * @return The remote key.
     */
    public RemoteKey getRemoteKey() {
        return remoteKey;
    }

    /**
     * @return The ID of the remote.
     */
    public String getDsId() {
        return dsId;
    }

    /**
     * @return The public key of the remote
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * @return The web socket data endpoint URI for connecting to the server.
     */
    public String getWsUri() {
        return wsUri;
    }

    /**
     * @return The http data endpoint URI for connecting to the server.
     */
    public String getHttpUri() {
        return httpUri;
    }

    /**
     * @return The salt used in the handshake data endpoint.
     */
    public String getSalt() {
        return salt;
    }

    /**
     * @return The salt used in short polling.
     */
    public String getSaltS() {
        return saltS;
    }

    /**
     * @return The salt used in long polling.
     */
    public String getSaltL() {
        return saltL;
    }

    /**
     * @return The update interval which determines the delay before sending
     * an update in the subscription stream.
     */
    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Generates a remote handshake by connecting to the authentication
     * endpoint. Once the handshake is complete, a populated handshake
     * is returned. This enables the DSLink to connect to the data
     * endpoint of the server.
     *
     * @param lh  Handshake information
     * @param url URL for the authentication endpoint
     * @return Remote handshake information
     */
    public static RemoteHandshake generate(final LocalHandshake lh, URLInfo url) {
        if (url == null)
            throw new NullPointerException("url");
        HttpClient client = HttpClientUtils.configure(url);
        String fullPath = url.path + "?dsId=" + lh.getDsId();
        final DefaultFutureResult<RemoteHandshake> h = new DefaultFutureResult<>();
        final CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest req = client.post(fullPath, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse event) {
                if (event.statusCode() != HttpURLConnection.HTTP_OK) {
                    Throwable t = new Throwable("BAD STATUS: " + event.statusCode());
                    h.setFailure(t);
                    latch.countDown();
                } else {
                    event.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer event) {
                            String s = event.toString();
                            JsonObject o = new JsonObject(s);
                            LocalKeys k = lh.getKeys();
                            RemoteHandshake rh = new RemoteHandshake(k, o);
                            h.setResult(rh);
                            latch.countDown();
                        }
                    });
                }
            }
        });

        req.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                h.setFailure(event);
                latch.countDown();
            }
        });

        String encoded = lh.toJson().encode();
        req.end(encoded);

        try {
            latch.await();
        } catch (InterruptedException e) {
            h.setFailure(e);
        }

        if (h.failed()) {
            throw new RuntimeException(h.cause());
        }

        return h.result();
    }
}

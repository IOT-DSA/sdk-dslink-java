package org.dsa.iot.dslink.connection.handshake;

import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.UrlBase64;
import org.dsa.iot.core.URLInfo;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;

/**
 * Holds handshake information about a server.
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HandshakeServer {

    private final String dsId;
    private final String publicKey;
    private final String wsUri;
    private final String httpUri;
    private final byte[] sharedSecret;
    private final String salt;
    private final String saltS;
    private final Integer updateInterval;

    public byte[] getSharedSecret() {
        return sharedSecret.clone();
    }

    public static void perform(EventBus bus, String url, HandshakeClient hc,
                                      Handler<HandshakeServer> onComplete) {
        perform(bus, URLInfo.parse(url), hc, onComplete);
    }

    public static void perform(EventBus bus, URLInfo url, HandshakeClient hc,
                                      Handler<HandshakeServer> onComplete) {
        perform(bus, url, hc, true, onComplete);
    }

    public static void perform(@NonNull final EventBus bus,
                                @NonNull final URLInfo url,
                                @NonNull final HandshakeClient hc,
                                final boolean verifySsl,
                                @NonNull final Handler<HandshakeServer> onComplete) {
        HttpClient client = Utils.VERTX.createHttpClient();
        client.setHost(url.host).setPort(url.port);
        if (url.secure) {
            client.setSSL(true);
            client.setVerifyHost(verifySsl);
        }

        final String fullPath = url.path + "?dsId=" + hc.getDsId();
        HttpClientRequest req = client.post(fullPath,
                                            new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(final HttpClientResponse event) {
                        event.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer event) {
                                JsonObject obj = new JsonObject(event.toString());

                                String dsId = obj.getString("dsId");
                                String publicKey = obj.getString("publicKey");
                                String wsUri = obj.getString("wsUri");
                                String httpUri = obj.getString("httpUri");
                                byte[] sharedSecret = decryptSharedSecret(hc, obj.getString("tempKey"));
                                String salt = obj.getString("salt");
                                String saltS = obj.getString("saltS");
                                Integer updateInterval = obj.getInteger("updateInterval");

                                onComplete.handle(new HandshakeServer(dsId, publicKey, wsUri, httpUri,
                                        sharedSecret, salt, saltS, updateInterval));
                            }
                        });
                    }
                });
        String encoded = hc.toJson().encode();
        req.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                bus.post(new AsyncExceptionEvent(event));
                onComplete.handle(null);
            }
        });
        req.end(encoded, "UTF-8");
    }

    private static byte[] decryptSharedSecret(HandshakeClient client,
                                                String tempKey) {
        tempKey = Utils.addPadding(tempKey, true);
        byte[] decoded = UrlBase64.decode(tempKey);

        ECParameterSpec params = client.getPrivKeyInfo().getParameters();
        ECPoint point = params.getCurve().decodePoint(decoded);
        ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
        point = spec.getQ().multiply(client.getPrivKeyInfo().getD());
        byte[] sharedSecret = point.normalize().getXCoord().toBigInteger().toByteArray();
        sharedSecret = normalize(sharedSecret);
        return sharedSecret;
    }

    public static byte[] normalize(byte[] sharedSecret) {
        if (sharedSecret.length < 32) {
            byte[] fixed = new byte[32];
            int len = sharedSecret.length;
            System.arraycopy(sharedSecret, 0, fixed, 32 - len, len);
            sharedSecret = fixed;
        } else if (sharedSecret.length > 32) {
            byte[] fixed = new byte[32];
            System.arraycopy(sharedSecret, sharedSecret.length - 32, fixed, 0, fixed.length);
            sharedSecret = fixed;
        }
        return sharedSecret;
    }
}

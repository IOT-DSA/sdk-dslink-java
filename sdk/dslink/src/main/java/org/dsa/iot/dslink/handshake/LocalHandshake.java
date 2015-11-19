package org.dsa.iot.dslink.handshake;

import io.netty.util.CharsetUtil;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.TransportFormat;
import org.dsa.iot.dslink.util.UrlBase64;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Holds the necessary information to perform a handshake to a remote server.
 *
 * @author Samuel Grenier
 */
public class LocalHandshake {

    private final LocalKeys keys;
    private final String publicKey;
    private final String dsId;
    private final boolean isRequester;
    private final boolean isResponder;
    private final JsonObject linkData;
    private final String zone;
    private final String token;

    /**
     * Populates the handshake with the designated configuration.
     *
     * @param config Configuration of the DSLink.
     */
    public LocalHandshake(Configuration config) {
        if (config == null)
            throw new NullPointerException("config");
        config.validate();
        this.keys = config.getKeys();
        this.publicKey = keys.encodedPublicKey();
        this.dsId = config.getDsIdWithHash();
        this.isRequester = config.isRequester();
        this.isResponder = config.isResponder();
        this.linkData = config.getLinkData();
        this.zone = config.getZone();
        String token = config.getToken();
        if (token != null) {
            byte[] dsId = this.dsId.getBytes(CharsetUtil.UTF_8);
            byte[] fullToken = token.getBytes(CharsetUtil.UTF_8);
            byte[] bytes = new byte[dsId.length + fullToken.length];
            System.arraycopy(dsId, 0, bytes, 0, dsId.length);
            System.arraycopy(fullToken, 0, bytes, dsId.length, fullToken.length);

            SHA256.Digest sha = new SHA256.Digest();
            byte[] digested = sha.digest(bytes);
            String hash = UrlBase64.encode(digested);

            token = token.substring(0, 16) + hash;
        }
        this.token = token;
    }

    /**
     * @return The keys this handshake uses.
     */
    public LocalKeys getKeys() {
        return keys;
    }

    /**
     * @return Token used to connect to the broker
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the ID of the DSLink including the encoded hash key. This is
     * necessary in order to post to the server as a query parameter.
     *
     * @return ID of the DSLink.
     */
    public String getDsId() {
        return dsId;
    }

    /**
     * @return Whether the client wants to be a requester or not.
     */
    public boolean isRequester() {
        return isRequester;
    }

    /**
     * @return Whether the client wants to be a responder or not.
     */
    public boolean isResponder() {
        return isResponder;
    }

    /**
     * Encodes the handshake to be sent to the authentication server.
     *
     * @return JSON object ready to send to the server.
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.put("publicKey", publicKey);
        if (zone != null) {
            obj.put("zone", zone);
        }
        obj.put("isRequester", isRequester);
        obj.put("isResponder", isResponder);
        obj.put("linkData", linkData);
        obj.put("version", "1.0.4");
        obj.put("formats", TransportFormat.toJsonArray());
        return obj;
    }
}

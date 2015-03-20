package org.dsa.iot.dslink.handshake;

import org.dsa.iot.dslink.util.Configuration;
import org.vertx.java.core.json.JsonObject;

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
    private final String zone;

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
        this.dsId = config.getDsId() + "-" + keys.encodedHashPublicKey();
        this.isRequester = config.isRequester();
        this.isResponder = config.isResponder();
        this.zone = config.getZone();
    }

    /**
     * @return The keys this handshake uses.
     */
    public LocalKeys getKeys() {
        return keys;
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
        obj.putString("publicKey", publicKey);
        if (zone != null) {
            obj.putString("zone", zone);
        }
        obj.putBoolean("isRequester", isRequester);
        obj.putBoolean("isResponder", isResponder);
        return obj;
    }
}

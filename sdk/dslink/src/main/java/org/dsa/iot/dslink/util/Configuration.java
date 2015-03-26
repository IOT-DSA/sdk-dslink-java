package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;

import java.nio.file.Path;

/**
 * Holds the configuration of a DSLink.
 *
 * @author Samuel Grenier
 */
public class Configuration {

    private URLInfo authEndpoint;
    private String dsId;
    private String zone;
    private boolean isRequester;
    private boolean isResponder;
    private ConnectionType type;
    private LocalKeys keys;
    private Path serializationPath;

    /**
     * Example endpoint: http://localhost:8080/conn
     *
     * @param endpoint Authentication endpoint.
     * @see URLInfo#parse
     * @see #setAuthEndpoint(URLInfo)
     */
    public void setAuthEndpoint(String endpoint) {
        setAuthEndpoint(URLInfo.parse(endpoint));
    }

    /**
     * Sets the authentication endpoint used to make a connection for
     * performing a handshake.
     *
     * @param endpoint Authentication endpoint
     */
    public void setAuthEndpoint(URLInfo endpoint) {
        this.authEndpoint = endpoint;
    }

    /**
     * Gets the authentication endpoint used to make a connection for
     * performing a handshake.
     *
     * @return Authentication endpoint
     */
    public URLInfo getAuthEndpoint() {
        return authEndpoint;
    }

    /**
     * Sets the serialized keys. A deserialization will be performed when
     * setting these keys. If the serialized keys are not set, then one will
     * be automatically generated.
     *
     * @param serializedKeys Serialized keys to set
     * @see LocalKeys#deserialize(String)
     */
    public void setKeys(String serializedKeys) {
        if (serializedKeys == null)
            throw new NullPointerException("serializedKeys");
        setKeys(LocalKeys.deserialize(serializedKeys));
    }

    /**
     * Sets the local keys. These keys are used in the handshake.
     *
     * @param keys Keys to set
     * @see LocalKeys
     */
    public void setKeys(LocalKeys keys) {
        if (keys == null)
            throw new NullPointerException("keys");
        this.keys = keys;
    }

    /**
     * @return Local keys used in the handshake.
     */
    public LocalKeys getKeys() {
        return keys;
    }

    /**
     * Sets the designated connection type. This is used for connecting to
     * a data endpoint after the handshake is complete.
     *
     * @param type Type of connection
     */
    public void setConnectionType(ConnectionType type) {
        if (type == null)
            throw new NullPointerException("type");
        this.type = type;
    }

    /**
     * Designated connection type used when connecting to data endpoint.
     *
     * @return Connection type to be used
     */
    public ConnectionType getConnectionType() {
        return type;
    }

    /**
     * Do not set the ID with the public key hash. It will be automatically
     * appended during the handshake setup.
     *
     * @param dsId ID to set
     */
    public void setDsId(String dsId) {
        if (dsId == null)
            throw new NullPointerException("dsId");
        else if (dsId.isEmpty())
            throw new IllegalArgumentException("dsId is empty");
        this.dsId = dsId;
    }

    /**
     * Gets the raw ID of this DSLink. The public key hash has not been
     * appended to it.
     *
     * @return DsID
     */
    public String getDsId() {
        return dsId;
    }

    /**
     * This variable is only effective when the broker needs to approve of this
     * DSLink. When approved, the broker will take you out of this zone and
     * place the link in a normal designated area.
     *
     * @param zone Zone the broker should place this DSLink in.
     */
    public void setZone(String zone) {
        this.zone = zone;
    }

    /**
     * @return Requested zone
     */
    public String getZone() {
        return zone;
    }

    /**
     * @param requester Whether the DSLink is a requester or not.
     */
    public void setRequester(boolean requester) {
        this.isRequester = requester;
    }

    /**
     * @return Whether the DSLink is a requester or not.
     */
    public boolean isRequester() {
        return isRequester;
    }

    /**
     * @param responder Whether the DSLink is a requester or not.
     */
    public void setResponder(boolean responder) {
        this.isResponder = responder;
    }

    /**
     * @return Whether the DSLink is a requester or not.
     */
    public boolean isResponder() {
        return isResponder;
    }

    /**
     * Sets the serialization path. This location determines where
     * serialization and deserialization will occur.
     *
     * @param path Serialization path, can be null.
     */
    public void setSerializationPath(Path path) {
        this.serializationPath = path;
    }

    /**
     * @return Serialization path, can be null.
     */
    public Path getSerializationPath() {
        return serializationPath;
    }

    /**
     * Validates the configuration for any issues.
     */
    public void validate() {
        if (dsId == null) {
            throw new RuntimeException("dsId not set");
        } else if (dsId.isEmpty()) {
            // Should never happen
            throw new RuntimeException("dsId is empty");
        } else if (type == null) {
            throw new RuntimeException("connection type not set");
        } else if (authEndpoint == null) {
            throw new RuntimeException("authentication endpoint not set");
        } else if (keys == null) {
            throw new RuntimeException("keys not set");
        }
    }
}

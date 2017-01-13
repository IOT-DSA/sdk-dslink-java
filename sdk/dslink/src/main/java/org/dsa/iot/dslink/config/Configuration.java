package org.dsa.iot.dslink.config;

import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.PropertyReference;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.dslink.util.log.LogManager;

import java.io.File;
import java.io.IOException;

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
    private File serializationPath;
    private JsonObject linkData;
    private String token;
    private boolean qosPersistenceEnabled;
    private Boolean valuePersistenceEnabled;

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
     * Sets the token used during the connection handshake.
     *
     * @param token Token to set.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the token used during the connection handshake.
     *
     * @return Token
     */
    public String getToken() {
        return token;
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
     * Gets the full ID of this DSLink.
     *
     * @return DsId with public key hash appended to it.
     */
    public String getDsIdWithHash() {
        return getDsId() + "-" + keys.encodedHashPublicKey();
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
     * Sets the extra link data. This data can be used by any requester to
     * determine how it should handle the DSLink.
     *
     * @param data Link data.
     */
    public void setLinkData(JsonObject data) {
        if (data == null) {
            this.linkData = null;
        } else {
            this.linkData = data;
        }
    }

    /**
     * @return Link data.
     */
    public JsonObject getLinkData() {
        return linkData;
    }

    /**
     * Sets the serialization path. This location determines where
     * serialization and deserialization will occur.
     *
     * @param file Serialization path, can be null.
     */
    public void setSerializationPath(File file) {
        this.serializationPath = file;
    }

    /**
     * @return Serialization path, can be null.
     */
    public File getSerializationPath() {
        return serializationPath;
    }

    /**
     * Validates the configuration for any issues.
     */
    public void validate() {
        if (dsId == null) {
            throw new IllegalStateException("dsId not set");
        } else if (dsId.isEmpty()) {
            // Should never happen
            throw new IllegalStateException("dsId is empty");
        } else if (type == null) {
            throw new IllegalStateException("connection type not set");
        } else if (authEndpoint == null) {
            throw new IllegalStateException("authentication endpoint not set");
        } else if (keys == null) {
            throw new IllegalStateException("keys not set");
        } else if (!(isRequester || isResponder)) {
            throw new IllegalStateException("Neither a requester nor a responder");
        } else if (token != null && token.length() != 48) {
            throw new IllegalStateException("Token is not 48 characters long");
        }
    }

    public static Configuration autoConfigure(String[] origArgs,
                                              boolean requester,
                                              boolean responder,
                                              JsonObject linkData) {
        Configuration defaults = new Configuration();
        defaults.setConnectionType(ConnectionType.WEB_SOCKET);
        defaults.setRequester(requester);
        defaults.setResponder(responder);
        defaults.setLinkData(linkData);

        Arguments pArgs = Arguments.parse(origArgs);
        if (pArgs == null) {
            return null;
        }

        JsonObject json = getAndValidateJson(pArgs.getDslinkJson());
        String name = getFieldValue(json, "name", pArgs.getName());
        String logLevel = getFieldValue(json, "log", pArgs.getLogLevel());
        String brokerHost = pArgs.getBrokerHost();
        String keyPath = getFieldValue(json, "key", pArgs.getKeyPath());
        String nodePath = getFieldValue(json, "nodes", pArgs.getNodesPath());
        String handlerClass = getFieldValue(json, "handler_class", null);

        JsonObject valuePersistenceEnabledJsonNode = json.get("valuePersistenceEnabled");
        boolean valuePersistenceEnabled = true;
        if (valuePersistenceEnabledJsonNode != null) {
            valuePersistenceEnabled = getFieldValue(valuePersistenceEnabledJsonNode, "value", true);
        }
        defaults.setValuePersistenceEnabled(valuePersistenceEnabled);

        JsonObject qosPersistenceEnabledJsonNode = json.get("qosPersistenceEnabled");
        boolean persistQosEnabled = false;
        if (qosPersistenceEnabledJsonNode != null) {
            persistQosEnabled = getFieldValue(qosPersistenceEnabledJsonNode, "value", false);
        }
        defaults.setQosPersistenceEnabled(persistQosEnabled);

        {
            String logPath = pArgs.getLogPath();
            File file = null;
            if (logPath != null) {
                file = new File(logPath);
            }
            LogManager.configure(file);
            LogManager.setLevel(logLevel);
        }

        String prop = System.getProperty(PropertyReference.VALIDATE, "true");
        boolean validate = Boolean.parseBoolean(prop);
        if (validate) {
            prop = PropertyReference.VALIDATE_HANDLER;
            prop = System.getProperty(prop, "true");
            validate = Boolean.parseBoolean(prop);
        }
        if (validate) {
            try {
                // Validate handler class
                ClassLoader loader = Configuration.class.getClassLoader();
                Class<?> clazz = loader.loadClass(handlerClass);
                if (!DSLinkHandler.class.isAssignableFrom(clazz)) {
                    String err = "Class `" + handlerClass + "` does not extend";
                    err += " " + DSLinkHandler.class.getName();
                    throw new RuntimeException(err);
                }
            } catch (ClassNotFoundException e) {
                String err = "Handler class not found: " + handlerClass;
                throw new RuntimeException(err);
            }
        }

        defaults.setAuthEndpoint(brokerHost);
        defaults.setToken(pArgs.getToken());
        defaults.setDsId(name);

        File loc = new File(keyPath);
        defaults.setKeys(LocalKeys.getFromFileSystem(loc));

        loc = new File(nodePath);
        defaults.setSerializationPath(loc);
        return defaults;
    }

    public static JsonObject getConfigs(String jsonPath) {
        File file = new File(jsonPath);
        try {
            byte[] bytes = FileUtils.readAllBytes(file);
            JsonObject json = new JsonObject(new String(bytes, "UTF-8"));
            json = json.get("configs");

            if (json == null) {
                throw new RuntimeException("Missing `configs` field");
            }
            return json;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject getAndValidateJson(String jsonPath) {
        JsonObject json = getConfigs(jsonPath);

        String prop = System.getProperty(PropertyReference.VALIDATE, "true");
        if (!Boolean.parseBoolean(prop)) {
            return json;
        }
        prop = System.getProperty(PropertyReference.VALIDATE_JSON, "true");
        if (!Boolean.parseBoolean(prop)) {
            return json;
        }

        checkField(json, "broker");
        checkParam(json, "name");
        checkParam(json, "log");
        checkParam(json, "key");
        checkParam(json, "nodes");
        checkParam(json, "handler_class");
        return json;
    }

    private static <T> T getFieldValue(JsonObject json,
                                       String field,
                                       T defaultVal) {
        if (defaultVal != null) {
            return defaultVal;
        }
        JsonObject param = json.get(field);
        if (param == null) {
            return null;
        }
        return param.get("default");
    }

    private static void checkField(JsonObject configs, String name) {
        if (!configs.contains(name)) {
            throw new RuntimeException("Missing config field of " + name);
        }
    }

    private static void checkParam(JsonObject configs, String param) {
        JsonObject conf = configs.get(param);
        if (conf == null) {
            throw new RuntimeException("Missing config field of " + param);
        } else if (conf.get("default") == null) {
            throw new RuntimeException("Missing default value in config of " + param);
        }
    }

    public void setValuePersistenceEnabled(boolean valuePersistenceEnabled) {
        this.valuePersistenceEnabled = valuePersistenceEnabled;
    }

    public void setQosPersistenceEnabled(boolean qosPersistenceEnabled) {
        this.qosPersistenceEnabled = qosPersistenceEnabled;
    }

    /**
     * Persist qos2 and qos3 subscription to disk, default to false;
     */
    public boolean isQosPersistenceEnabled() {
        return qosPersistenceEnabled;
    }

    /**
     * Persist value setting to disk, default to true;
     **/
    public Boolean isValuePersistenceEnabled() {
        return valuePersistenceEnabled;
    }
}

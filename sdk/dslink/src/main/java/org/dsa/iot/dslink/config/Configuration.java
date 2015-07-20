package org.dsa.iot.dslink.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.log.LogManager;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Holds the configuration of a DSLink.
 *
 * @author Samuel Grenier
 */
public class Configuration {

    private static final ObjectMapper MAPPER;

    private URLInfo authEndpoint;
    private String dsId;
    private String zone;
    private boolean isRequester;
    private boolean isResponder;
    private ConnectionType type;
    private LocalKeys keys;
    private File serializationPath;

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
        } else if (!(isRequester || isResponder)) {
            throw new RuntimeException("Neither a requester nor a responder");
        }
    }

    public static Configuration autoConfigure(String[] args,
                                                boolean requester,
                                                boolean responder) {
        Configuration defaults = new Configuration();
        defaults.setConnectionType(ConnectionType.WEB_SOCKET);
        defaults.setRequester(requester);
        defaults.setResponder(responder);

        Arguments parsedArgs = Arguments.parse(args);
        if (parsedArgs == null) {
            return null;
        }

        JsonObject json = getAndValidateJson(parsedArgs.getDslinkJson());
        String name = getFieldValue(parsedArgs.getName(), json, "name");
        String logLevel = getFieldValue(parsedArgs.getLogLevel(), json, "log");
        String brokerHost = parsedArgs.getBrokerHost();
        String keyPath = getFieldValue(parsedArgs.getKeyPath(), json, "key");
        String nodePath = getFieldValue(parsedArgs.getNodesPath(), json, "nodes");

        defaults.setDsId(name);

        LogManager.configure();
        LogManager.setLevel(logLevel);
        defaults.setAuthEndpoint(brokerHost);

        File loc = new File(keyPath);
        defaults.setKeys(LocalKeys.getFromFileSystem(loc));

        loc = new File(nodePath);
        defaults.setSerializationPath(loc);
        return defaults;
    }

    private static JsonObject getAndValidateJson(String jsonPath) {
        JsonFactory factory = MAPPER.getFactory();
        File file = new File(jsonPath);
        try (JsonParser parser = factory.createParser(file)) {
            JsonObject json = null;
            parser.nextToken();
            while (parser.nextToken() != null) {
                String name = parser.getText();
                if ("configs".equals(name)) {
                    //noinspection unchecked
                    json = new JsonObject(parser.readValueAs(Map.class));
                    break;
                }
            }

            if (json == null) {
                throw new RuntimeException("Missing `configs` field");
            } else {
                JsonObject configs = json.getObject("configs");
                checkField(configs, "broker");
                checkParam(configs, "name");
                checkParam(configs, "log");
                checkParam(configs, "key");
                checkParam(configs, "nodes");
                return configs;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFieldValue(String argsVal,
                                        JsonObject json,
                                        String field) {
        if (argsVal == null) {
            JsonObject param = json.getObject(field);
            return param.getString("default");
        }
        return argsVal;
    }

    private static void checkField(JsonObject configs, String name) {
        if (!configs.containsField(name)) {
            throw new RuntimeException("Missing config field of " + name);
        }
    }

    private static void checkParam(JsonObject configs, String param) {
        JsonObject conf = configs.getObject(param);
        if (conf == null) {
            throw new RuntimeException("Missing config field of " + param);
        } else if (conf.getString("default") == null) {
            throw new RuntimeException("Missing default value in config of " + param);
        }
    }

    private static class JsonObjectSerializer extends JsonSerializer<JsonObject> {
        @Override
        public void serialize(JsonObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.toMap());
        }
    }

    private static class JsonArraySerializer extends JsonSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.toList());
        }
    }

    static {
        MAPPER = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        MAPPER.registerModule(module);
    }
}

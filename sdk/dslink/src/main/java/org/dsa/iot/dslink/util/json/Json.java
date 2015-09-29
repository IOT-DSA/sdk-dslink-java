package org.dsa.iot.dslink.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Json {

    static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();
    static final ObjectMapper MAPPER = new ObjectMapper();

    public static String encode(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodePrettily(Object obj) {
        try {
            return PRETTY_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(String content, Class<?> clazz) {
        try {
            return (T) MAPPER.readValue(content, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object checkAndUpdate(Object value) {
        if (value != null && !((value instanceof Byte)
                || (value instanceof Short)
                || (value instanceof Integer)
                || (value instanceof Long)
                || (value instanceof Float)
                || (value instanceof Double)
                || (value instanceof Boolean)
                || (value instanceof String)
                || (value instanceof Map)
                || (value instanceof List)
                || (value instanceof JsonObject)
                || (value instanceof JsonArray)
                || (value instanceof Value))) {
            throw new IllegalArgumentException("Invalid class: " + value.getClass());
        }
        if (value instanceof Map) {
            return new JsonObject((Map<String, Object>) value);
        } else if (value instanceof List) {
            return new JsonArray((List<Object>) value);
        } else if (value instanceof Value) {
            return ValueUtils.toObject((Value) value);
        }
        return value;
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    private static class JsonObjectSerializer extends JsonSerializer<JsonObject> {
        @Override
        public void serialize(JsonObject value,
                              JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeObject(value.getMap());
        }
    }

    private static class JsonArraySerializer extends JsonSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray value,
                              JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeObject(value.getList());
        }
    }

    static {
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        PRETTY_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        PRETTY_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);

        SimpleModule module = new SimpleModule();
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        MAPPER.registerModule(module);
        PRETTY_MAPPER.registerModule(module);
    }
}

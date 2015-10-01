package org.dsa.iot.dslink.util.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.json.decoders.ListDecoder;
import org.dsa.iot.dslink.util.json.decoders.MapDecoder;
import org.dsa.iot.dslink.util.json.encoders.ListEncoder;
import org.dsa.iot.dslink.util.json.encoders.MapEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Json {

    private static final JsonFactory FACTORY = new JsonFactory();

    private Json() {
    }

    public static String encode(Object obj) {
        return performEncode(obj, null);
    }

    public static String encodePrettily(Object obj) {
        return performEncode(obj, new DefaultPrettyPrinter());
    }

    private static String performEncode(Object obj, PrettyPrinter printer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoding enc = JsonEncoding.UTF8;
        try (JsonGenerator gen = FACTORY.createGenerator(baos, enc)) {
            gen.setPrettyPrinter(printer);
            if (obj instanceof JsonObject) {
                MapEncoder.write(gen, (JsonObject) obj);
            } else if (obj instanceof JsonArray) {
                ListEncoder.write(gen, (JsonArray) obj);
            }
            gen.close();
            return baos.toString("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> decodeMap(String content) {
        return MapDecoder.decode(FACTORY, content);
    }

    public static List<Object> decodeList(String content) {
        return ListDecoder.decode(FACTORY, content);
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

    @SuppressWarnings("unchecked")
    public static Object update(Object value) {
        if (value instanceof Map) {
            return new JsonObject((Map) value);
        } else if (value instanceof List) {
            return new JsonArray((List) value);
        }
        return value;
    }
}

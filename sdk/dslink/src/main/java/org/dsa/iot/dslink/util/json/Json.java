package org.dsa.iot.dslink.util.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.dsa.iot.dslink.connection.TransportFormat;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.json.decoders.ListDecoder;
import org.dsa.iot.dslink.util.json.decoders.MapDecoder;
import org.dsa.iot.dslink.util.json.encoders.ListEncoder;
import org.dsa.iot.dslink.util.json.encoders.MapEncoder;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Json {

    private static final MessagePackFactory MSG_FACTORY;
    private static final JsonFactory JSON_FACTORY;

    private Json() {
    }

    public static byte[] encode(TransportFormat format,
                                Object obj) {
        return performEncode(format, obj, null);
    }

    public static byte[] encodePrettily(TransportFormat format,
                                        Object obj) {
        return performEncode(format, obj, new DefaultPrettyPrinter());
    }

    private static byte[] performEncode(TransportFormat format,
                                        Object obj,
                                        PrettyPrinter printer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoding enc = JsonEncoding.UTF8;
        JsonGenerator gen;
        try {
            if (format == TransportFormat.JSON) {
                gen = JSON_FACTORY.createGenerator(baos, enc);
            } else if (format == TransportFormat.MESSAGE_PACK) {
                gen = MSG_FACTORY.createGenerator(baos, enc);
            } else {
                throw new UnsupportedOperationException(format.toJson());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            gen.setPrettyPrinter(printer);
            if (obj instanceof JsonObject) {
                MapEncoder.write(gen, (JsonObject) obj);
            } else if (obj instanceof JsonArray) {
                ListEncoder.write(gen, (JsonArray) obj);
            }
            gen.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> decodeMap(TransportFormat format,
                                                byte[] content) {
        if (format == TransportFormat.JSON) {
            return MapDecoder.decode(JSON_FACTORY, content);
        } else if (format == TransportFormat.MESSAGE_PACK) {
            return MapDecoder.decode(MSG_FACTORY, content);
        }
        throw new UnsupportedOperationException(format.toJson());
    }

    public static List<Object> decodeList(TransportFormat format,
                                          byte[] content) {
        if (format == TransportFormat.JSON) {
            return ListDecoder.decode(JSON_FACTORY, content);
        } else if (format == TransportFormat.MESSAGE_PACK) {
            return ListDecoder.decode(MSG_FACTORY, content);
        }
        throw new UnsupportedOperationException(format.toJson());
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

    static {
        MSG_FACTORY = new MessagePackFactory();
        JSON_FACTORY = new JsonFactory();
    }
}

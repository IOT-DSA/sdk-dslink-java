package org.dsa.iot.dslink.util.json.encoders;

import com.fasterxml.jackson.core.JsonGenerator;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class MapEncoder {

    public static void write(JsonGenerator gen, JsonObject json)
                                                throws IOException {
        gen.writeStartObject();
        performWrite(gen, json);
    }

    static void performWrite(JsonGenerator gen, JsonObject json)
                                                        throws IOException {
        for (Map.Entry<String, Object> entry : json) {
            String name = entry.getKey();
            Object instance = entry.getValue();
            if (instance instanceof Byte) {
                gen.writeNumberField(name, ((Number) instance).byteValue());
            } else if (instance instanceof Short) {
                gen.writeNumberField(name, ((Number) instance).shortValue());
            } else if (instance instanceof Integer) {
                gen.writeNumberField(name, ((Number) instance).intValue());
            } else if (instance instanceof Long) {
                gen.writeNumberField(name, ((Number) instance).longValue());
            } else if (instance instanceof Float) {
                gen.writeNumberField(name, ((Number) instance).floatValue());
            } else if (instance instanceof Double) {
                gen.writeNumberField(name, ((Number) instance).doubleValue());
            } else if (instance instanceof Boolean) {
                gen.writeBooleanField(name, (Boolean) instance);
            } else if (instance instanceof String) {
                gen.writeStringField(name, (String) instance);
            } else if (instance instanceof JsonObject) {
                gen.writeObjectFieldStart(name);
                performWrite(gen, (JsonObject) instance);
            } else if (instance instanceof JsonArray) {
                gen.writeArrayFieldStart(name);
                ListEncoder.performWrite(gen, (JsonArray) instance);
            }
        }
        gen.writeEndObject();
    }
}

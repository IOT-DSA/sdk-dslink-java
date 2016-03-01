package org.dsa.iot.dslink.util.json.encoders;

import com.fasterxml.jackson.core.JsonGenerator;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Samuel Grenier
 */
public class ListEncoder {

    public static void write(JsonGenerator gen, JsonArray json)
                                            throws IOException {
        gen.writeStartArray();
        performWrite(gen, json);
    }

    static void performWrite(JsonGenerator gen, JsonArray json)
                                            throws IOException {
        for (Object instance : json) {
            if (instance instanceof Byte) {
                gen.writeNumber(((Number) instance).byteValue());
            } else if (instance instanceof Short) {
                gen.writeNumber(((Number) instance).shortValue());
            } else if (instance instanceof Integer) {
                gen.writeNumber(((Number) instance).intValue());
            } else if (instance instanceof Long) {
                gen.writeNumber(((Number) instance).longValue());
            } else if (instance instanceof Float) {
                gen.writeNumber(((Number) instance).floatValue());
            } else if (instance instanceof Double) {
                gen.writeNumber(((Number) instance).doubleValue());
            } else if (instance instanceof BigDecimal) {
                gen.writeNumber((BigDecimal) instance);
            } else if (instance instanceof BigInteger) {
                gen.writeNumber((BigInteger) instance);
            } else if (instance instanceof Boolean) {
                gen.writeBoolean((Boolean) instance);
            } else if (instance instanceof String) {
                gen.writeString((String) instance);
            } else if (instance instanceof JsonObject) {
                gen.writeStartObject();
                MapEncoder.performWrite(gen, (JsonObject) instance);
            } else if (instance instanceof JsonArray) {
                gen.writeStartArray();
                performWrite(gen, (JsonArray) instance);
            } else if (instance instanceof byte[]) {
                gen.writeBinary((byte[]) instance);
            } else if (instance == null) {
                gen.writeNull();
            } else {
                String err = "Unsupported class: " + instance.getClass().getName();
                throw new RuntimeException(err);
            }
        }
        gen.writeEndArray();
    }
}

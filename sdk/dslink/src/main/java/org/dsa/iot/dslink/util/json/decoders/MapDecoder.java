package org.dsa.iot.dslink.util.json.decoders;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class MapDecoder {

    public static Map<String, Object> decode(JsonFactory factory,
                                                byte[] content) {
        final Map<String, Object> map = new LinkedHashMap<>();
        JsonParser parser = null;
        try {
            parser = factory.createParser(content);
            parser.nextToken();
            performDecodeMap(map, parser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (parser != null) {
                try {
                    parser.close();
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }

    static void performDecodeMap(Map<String, Object> in,
                                         JsonParser parser)
                                            throws IOException {
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            String name = parser.getText();
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            } else if (token == JsonToken.VALUE_NULL) {
                in.put(name, null);
            } else if (token == JsonToken.VALUE_STRING) {
                in.put(name, parser.getText());
            } else if (token == JsonToken.VALUE_FALSE) {
                in.put(name, false);
            } else if (token == JsonToken.VALUE_TRUE) {
                in.put(name, true);
            } else if (token == JsonToken.VALUE_NUMBER_INT
                    || token == JsonToken.VALUE_NUMBER_FLOAT) {
                in.put(name, parser.getNumberValue());
            } else if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
                in.put(name, parser.getBinaryValue());
            } else if (token == JsonToken.START_ARRAY) {
                List<Object> list = new LinkedList<>();
                ListDecoder.performDecodeList(list, parser);
                in.put(name, new JsonArray(list));
            } else if (token == JsonToken.START_OBJECT) {
                Map<String, Object> map = new LinkedHashMap<>();
                performDecodeMap(map, parser);
                in.put(name, new JsonObject(map));
            }
        }
    }
}

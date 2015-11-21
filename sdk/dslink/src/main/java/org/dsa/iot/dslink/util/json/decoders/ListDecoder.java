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
public class ListDecoder {

    public static List<Object> decode(JsonFactory factory,
                                      byte[] content,
                                      int offset,
                                      int length) {
        List<Object> list = new LinkedList<>();
        JsonParser parser = null;
        try {
            parser = factory.createParser(content, offset, length);
            parser.nextToken();
            performDecodeList(list, parser);
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
        return list;
    }

    static void performDecodeList(List<Object> in,
                                          JsonParser parser)
            throws IOException {
        JsonToken token;
        while (!((token = parser.nextToken()) == JsonToken.END_ARRAY
                || token == null)) {
            if (token == JsonToken.VALUE_NULL) {
                in.add(null);
            } else if (token == JsonToken.VALUE_STRING) {
                in.add(parser.getText());
            } else if (token == JsonToken.VALUE_FALSE) {
                in.add(false);
            } else if (token == JsonToken.VALUE_TRUE) {
                in.add(true);
            } else if (token == JsonToken.VALUE_NUMBER_INT
                    || token == JsonToken.VALUE_NUMBER_FLOAT) {
                in.add(parser.getNumberValue());
            } else if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
                in.add(parser.getBinaryValue());
            } else if (token == JsonToken.START_ARRAY) {
                List<Object> list = new LinkedList<>();
                performDecodeList(list, parser);
                in.add(new JsonArray(list));
            } else if (token == JsonToken.START_OBJECT) {
                Map<String, Object> map = new LinkedHashMap<>();
                MapDecoder.performDecodeMap(map, parser);
                in.add(new JsonObject(map));
            }
        }
    }
}

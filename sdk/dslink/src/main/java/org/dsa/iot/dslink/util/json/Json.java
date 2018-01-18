package org.dsa.iot.dslink.util.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.UrlBase64;
import org.dsa.iot.dslink.util.json.decoders.ListDecoder;
import org.dsa.iot.dslink.util.json.decoders.MapDecoder;
import org.dsa.iot.dslink.util.json.encoders.ListEncoder;
import org.dsa.iot.dslink.util.json.encoders.MapEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Json {

    private static final JsonFactory JSON_FACTORY;

    private Json() {
    }

    public static byte[] encode(EncodingFormat format,
                                Object obj) {
        return performEncode(format, obj, null);
    }

    public static byte[] encodePrettily(EncodingFormat format,
                                        Object obj) {
        return performEncode(format, obj, new DefaultPrettyPrinter());
    }

    private static byte[] performEncode(EncodingFormat format,
                                        Object obj,
                                        PrettyPrinter printer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoding enc = JsonEncoding.UTF8;
        JsonGenerator gen;
        try {
            if (format == EncodingFormat.JSON) {
                gen = JSON_FACTORY.createGenerator(baos, enc);
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

    public static Map<String, Object> decodeMap(EncodingFormat format,
                                                byte[] content,
                                                int offset,
                                                int length) {
        if (format == EncodingFormat.JSON) {
            return MapDecoder.decode(JSON_FACTORY, content, offset, length);
        }
        throw new UnsupportedOperationException(format.toJson());
    }

    public static List<Object> decodeList(EncodingFormat format,
                                          byte[] content,
                                          int offset,
                                          int length) {
        if (format == EncodingFormat.JSON) {
            return ListDecoder.decode(JSON_FACTORY, content, offset, length);
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
                || (value instanceof BigDecimal)
                || (value instanceof BigInteger)
                || (value instanceof Boolean)
                || (value instanceof String)
                || (value instanceof Map)
                || (value instanceof List)
                || (value instanceof JsonObject)
                || (value instanceof JsonArray)
                || (value instanceof Value)
                || (value instanceof byte[]))) {
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
        JSON_FACTORY = new JsonFactory() {
            @Override
            protected JsonGenerator _createGenerator(Writer out,
                                                     IOContext ctxt)
                                                    throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            protected JsonGenerator _createUTF8Generator(OutputStream out,
                                                         IOContext ctxt)
                                                        throws IOException {
                UTF8JsonGenerator gen = new UTF8JsonGenerator(ctxt,
                        _generatorFeatures, _objectCodec, out) {
                    @Override
                    public void writeBinary(Base64Variant bv,
                                            byte[] data,
                                            int offset,
                                            int len) throws IOException {
                        String s = "\u001Bbytes:" + UrlBase64.encode(data);
                        writeString(s);
                    }
                };
                if (_characterEscapes != null) {
                    gen.setCharacterEscapes(_characterEscapes);
                }
                gen.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
                SerializableString rootSep = _rootValueSeparator;
                if (rootSep != DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR) {
                    gen.setRootValueSeparator(rootSep);
                }
                return gen;
            }
        };
    }
}

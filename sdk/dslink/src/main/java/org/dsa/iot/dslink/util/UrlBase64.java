package org.dsa.iot.dslink.util;

/**
 * Handles base64 encodings. All encodings are UTF-8.
 *
 * @author Samuel Grenier
 */
public class UrlBase64 {

    /**
     * @param data UTF-8 encoded string.
     * @return UTF-8 encoded base64 string.
     */
    public static String encode(String data) {
        return encode(data, "UTF-8");
    }

    /**
     * Encodes a string and outputs an encoded string.
     *
     * @param data String to encode.
     * @param encoding Encoding the data and encoded data should be in.
     * @return Base64 encoded string.
     */
    public static String encode(String data, String encoding) {
        try {
            byte[] bytes = data.getBytes(encoding);
            return encode(bytes, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param data Bytes to encode.
     * @return UTF-8 encoded base64 string.
     */
    public static String encode(byte[] data) {
        return encode(data, "UTF-8");
    }

    /**
     * Encodes the following data into a string for base64 for the specified
     * encoding. The padding is explicitly removed.
     *
     * @param data Array of data to encode.
     * @param encoding Encoding to encode the bytes into after the base64
     *                 encoding.
     * @return A base64 encoded string.
     */
    public static String encode(byte[] data, String encoding) {
        try {
            String s = new String(bouncyEncode(data), encoding);
            return stripPadding(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param encoded UTF-8 encoded string
     * @return Array of base64 decoded bytes.
     */
    public static byte[] decode(String encoded) {
        return decode(encoded, "UTF-8");
    }

    /**
     * The provided encoded data.
     *
     * @param encoded Base64 encoded string.
     * @param encoding Encoding to decode the base64 encoded data into.
     * @return Byte array of the decoded Base64 string.
     */
    public static byte[] decode(String encoded, String encoding) {
        try {
            encoded = addPadding(encoded);
            byte[] data = encoded.getBytes(encoding);
            return bounceDecode(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The base64 encoded data must be UTF-8 encoded.
     *
     * @param encoded Base64 encoded string in UTF-8 encoding.
     * @return Decoded string in UTF-8 encoding.
     */
    public static String decodeToString(String encoded) {
        return decodeToString(encoded, "UTF-8");
    }

    /**
     * Decodes an encoded string into a decoded string.
     *
     * @param encoded Base64 encoded data
     * @param encoding Encoding to use in the encoded and decoded information
     * @return Decoded information
     */
    public static String decodeToString(String encoded, String encoding) {
        try {
            byte[] data = decode(encoded, encoding);
            return new String(data, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param encoded Encoded data to remove the padding from.
     * @return Padding free base64 encoded string.
     */
    public static String stripPadding(String encoded) {
        while (encoded.endsWith(".")) {
            encoded = encoded.substring(0, encoded.length() - 1);
        }
        return encoded;
    }

    /**
     * Must be UTF-8 compatible.
     *
     * @param encoded Adds padding to the encoding.
     * @return Padded base64 encoding.
     */
    public static String addPadding(String encoded) {
        StringBuilder buffer = new StringBuilder(encoded);
        while (buffer.length() % 4 != 0) {
            buffer.append(".");
        }
        return buffer.toString();
    }

    /**
     * Shortcut reference to the bouncy castle base64 encoder.
     *
     * @param data Data to encode
     * @return An array of the encoded data
     */
    private static byte[] bouncyEncode(byte[] data) {
        return org.bouncycastle.util.encoders.UrlBase64.encode(data);
    }

    /**
     * Shortcut reference to the bouncy castle base4 decoder.
     *
     * @param data Data to decode. Must contain padding.
     * @return An array of the decoded data.
     */
    private static byte[] bounceDecode(byte[] data) {
        return org.bouncycastle.util.encoders.UrlBase64.decode(data);
    }
}

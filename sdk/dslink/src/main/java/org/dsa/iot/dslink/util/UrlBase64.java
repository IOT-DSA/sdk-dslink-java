package org.dsa.iot.dslink.util;

import java.io.UnsupportedEncodingException;

/**
 * Handles base64 encodings. All encodings are UTF-8.
 *
 * @author Samuel Grenier
 */
public class UrlBase64 {

    /**
     * Encodes a string and outputs an encoded string.
     *
     * @param data String must be UTF-8 compatible
     * @return Base64 encoded string in UTF-8.
     */
    public static String encode(String data) {
        try {
            return encode(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the following data into a UTF-8 string encoding for
     * base64. The padding is explicitly removed.
     *
     * @param data Array of data to encode.
     * @return A UTF-8 encoded base64 string.
     */
    public static String encode(byte[] data) {
        try {
            String s = new String(bouncyEncode(data), "UTF-8");
            return stripPadding(s);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The provided encoded data must be UTF-8 compatible.
     *
     * @param encoded Encoded data in UTF-8
     * @return Decoded information
     */
    public static byte[] decode(String encoded) {
        try {
            encoded = addPadding(encoded);
            byte[] data = encoded.getBytes("UTF-8");
            return bounceDecode(data);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes an encoded string into a decoded string.
     *
     * @param encoded Encoded data in UTF-8
     * @return Decoded information in a UTF-8 string
     */
    public static String decodeToString(String encoded) {
        try {
            byte[] data = decode(encoded);
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
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

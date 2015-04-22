package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * URL base64 encoding tests.
 *
 * @author Samuel Grenier
 */
public class UrlBase64Test {

    @Before
    public void setup() {
        // Coverage
        new UrlBase64();
    }

    /**
     * Ensures that there is no padding during the encoding process.
     */
    @Test
    public void noPadding() {
        String encoded = UrlBase64.stripPadding("MTIzNA..");
        Assert.assertFalse(encoded.endsWith("."));

        encoded = UrlBase64.encode("1234");
        Assert.assertFalse(encoded.endsWith("."));
    }

    /**
     * Ensures that the padding is added. This is essential when performing a
     * decode.
     */
    @Test
    public void padding() {
        String encoded = UrlBase64.addPadding("MTIzNA");
        Assert.assertTrue(encoded.endsWith(".."));

        String decoded = UrlBase64.decodeToString("MTIzNA");
        Assert.assertEquals("1234", decoded);
    }

    @Test
    public void badEncoding() {
        boolean exception = false;
        try {
            UrlBase64.encode("", "UNKNOWN");
        } catch (RuntimeException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try {
            UrlBase64.encode(new byte[0], "UNKNOWN");
        } catch (RuntimeException e) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void badDecoding() {
        boolean exception = false;
        try {
            UrlBase64.decodeToString("", "UNKNOWN");
        } catch (RuntimeException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try {
            UrlBase64.decode("", "UNKNOWN");
        } catch (RuntimeException e) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }
}

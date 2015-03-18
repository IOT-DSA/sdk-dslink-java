package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * URL base64 encoding tests.
 *
 * @author Samuel Grenier
 */
public class UrlBase64Test {

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
}

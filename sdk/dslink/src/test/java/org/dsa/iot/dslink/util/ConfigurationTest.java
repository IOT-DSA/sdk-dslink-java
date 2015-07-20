package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.junit.Test;

/**
 * Tests the configuration and its validation schemes.
 *
 * @author Samuel Grenier
 */
public class ConfigurationTest {

    /**
     * Ensures the validation fails on invalid setup data.
     */
    @Test(expected = RuntimeException.class)
    public void validationNoID() {
        Configuration config = new Configuration();
        config.validate();
    }

    /**
     * Ensures that empty IDs can't be set.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validationEmptyID() {
        Configuration config = new Configuration();
        config.setDsId("");
    }

    /**
     * Ensures that null IDs can't be set.
     */
    @Test(expected = NullPointerException.class)
    public void validationNullID() {
        Configuration config = new Configuration();
        config.setDsId(null);
    }

    /**
     * Ensures that null connection types can't be set.
     */
    @Test(expected = NullPointerException.class)
    public void validationNullConnType() {
        Configuration config = new Configuration();
        config.setConnectionType(null);
    }

    /**
     * Ensures that null keys can't be set.
     */
    @Test(expected = NullPointerException.class)
    public void validationNullStringKeys() {
        Configuration config = new Configuration();
        config.setKeys((String) null);
    }

    /**
     * Ensures that null keys can't be set.
     */
    @Test(expected = NullPointerException.class)
    public void validationNullLocalKeys() {
        Configuration config = new Configuration();
        config.setKeys((LocalKeys) null);
    }

    /**
     * Ensures that the validation passes on proper data.
     */
    @Test
    public void validationPass() {
        Configuration config = new Configuration();
        config.setDsId("test");
        config.setConnectionType(ConnectionType.WEB_SOCKET);
        config.setAuthEndpoint("http://localhost");
        config.setKeys(LocalKeys.generate());
        config.setRequester(true);
        config.setResponder(false);
        config.validate();
    }
}

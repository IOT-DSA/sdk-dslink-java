package org.dsa.iot.dslink.handshake;

import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the local handshake json information
 *
 * @author Samuel Grenier
 */
public class LocalHandshakeTest {

    /**
     * Ensures the proper json is outputted in the handshake that needs to be
     * sent to the server.
     */
    @Test
    public void ensureFieldsExist() {
        Configuration config = new Configuration();
        config.setConnectionType(ConnectionType.WEB_SOCKET);
        config.setAuthEndpoint("http://localhost");
        config.setKeys(LocalKeys.generate());
        config.setDsId("test");
        config.setResponder(true);

        LocalHandshake handshake = new LocalHandshake(config);

        JsonObject object = handshake.toJson();
        Assert.assertNotNull(object);

        Assert.assertFalse(object.contains("zone"));
        config.setZone("testZone");
        handshake = new LocalHandshake(config);
        object = handshake.toJson();
        Assert.assertTrue(object.contains("zone"));

        Assert.assertTrue(object.contains("publicKey"));
        Assert.assertTrue(object.contains("isRequester"));
        Assert.assertTrue(object.contains("isResponder"));
    }
}

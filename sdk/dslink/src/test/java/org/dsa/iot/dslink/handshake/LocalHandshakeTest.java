package org.dsa.iot.dslink.handshake;

import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.util.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

/**
 * Tests the local handshake json information
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

        LocalHandshake handshake = new LocalHandshake(config);

        JsonObject object = handshake.toJson();
        Assert.assertNotNull(object);

        Assert.assertFalse(object.containsField("zone"));
        config.setZone("testZone");
        handshake = new LocalHandshake(config);
        object = handshake.toJson();
        Assert.assertTrue(object.containsField("zone"));

        Assert.assertTrue(object.containsField("publicKey"));
        Assert.assertTrue(object.containsField("isRequester"));
        Assert.assertTrue(object.containsField("isResponder"));
    }
}

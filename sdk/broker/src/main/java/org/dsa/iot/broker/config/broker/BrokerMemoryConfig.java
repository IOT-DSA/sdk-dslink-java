package org.dsa.iot.broker.config.broker;

import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Broker configuration stored in memory.
 *
 * @author Samuel Grenier
 */
public class BrokerMemoryConfig extends BrokerConfig {

    private JsonObject opts;

    @Override
    public JsonObject getConfig() {
        return opts;
    }

    @Override
    public void readAndUpdate() {
        opts = new JsonObject();
        addDefaultOpts();
    }

    protected void addDefaultOpts() {
        JsonObject server = new JsonObject();
        {
            JsonObject http = new JsonObject();
            http.put("enabled", true);
            http.put("host", "0.0.0.0");
            http.put("port", 8080);
            server.put("http", http);
        }
        {
            JsonObject https = new JsonObject();
            https.put("enabled", false);
            https.put("host", "0.0.0.0");
            https.put("port", 8443);
            https.put("certChainFile", null);
            https.put("certKeyFile", null);
            https.put("certKeyPass", null);
            server.put("https", https);
        }
        opts.put("server", server);
    }
}

package org.dsa.iot.broker.config.broker;

import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.io.File;
import java.io.IOException;

/**
 * @author Samuel Grenier
 */
public class BrokerFileConfig extends BrokerConfig {

    private final File path;
    private JsonObject opts;

    public BrokerFileConfig() {
        this("server.json");
    }

    public BrokerFileConfig(String file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file");
        }
        this.path = new File(file);
    }

    @Override
    public JsonObject get() {
        return opts;
    }

    @Override
    public void readAndUpdate() {
        opts = getDefaultConfig();
        if (path.exists()) {
            try {
                byte[] bytes = FileUtils.readAllBytes(path);
                opts.mergeIn(new JsonObject(new String(bytes, "UTF-8")), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        writeConfig(opts);
    }

    protected void writeConfig(JsonObject opts) {
        try {
            String data = opts.encodePrettily();
            FileUtils.write(path, data.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonObject getDefaultConfig() {
        BrokerConfig conf = new BrokerMemoryConfig();
        conf.readAndUpdate();
        return conf.get();
    }
}

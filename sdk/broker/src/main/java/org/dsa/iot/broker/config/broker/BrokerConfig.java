package org.dsa.iot.broker.config.broker;

import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class BrokerConfig {

    public abstract JsonObject get();

    public abstract void readAndUpdate();
}

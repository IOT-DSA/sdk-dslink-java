package org.dsa.iot.dslink.node.storage;

import org.dsa.iot.dslink.node.SubscriptionManager.Subscription;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.json.JsonArray;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public interface StorageDriver {

    void read(Map<String, Subscription> map);

    void store(Subscription sub, Value value);

    JsonArray getUpdates(Subscription sub);
}

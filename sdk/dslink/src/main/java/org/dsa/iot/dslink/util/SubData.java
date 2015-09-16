package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.NodeManager;

/**
 * Contains data for a specific path when performing subscription requests.
 *
 * @author Samuel Grenier
 */
public class SubData {

    private final String path;
    private final Integer qos;

    /**
     * Constructs a data container used when making a subscription
     * request.
     *
     * @param path Path of the subscription.
     * @param qos QoS of the subscription.
     */
    public SubData(String path, Integer qos) {
        this.path = NodeManager.normalizePath(path, true);
        this.qos = qos;
        if (qos != null) {
            int q = qos;
            if (q > 3 || q < 0) {
                throw new IllegalArgumentException("Invalid QoS setting");
            }
        }
    }

    public String getPath() {
        return path;
    }

    public Integer getQos() {
        return qos;
    }
}

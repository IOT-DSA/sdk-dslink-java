package org.dsa.iot.broker.methods;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListResponse {

    private final Broker broker;
    private final String path;

    public ListResponse(Broker broker, String path) {
        if (broker == null) {
            throw new NullPointerException("broker");
        } else if (path == null) {
            throw new NullPointerException("path");
        }
        this.broker = broker;
        this.path = path;
    }

    public JsonObject getResponse(Client client, int rid) {
        ParsedPath pp = ParsedPath.parse(broker.getDownstreamName(), path);
        BrokerNode<?> node = broker.getTree().getRoot();
        {
            String[] split = pp.split();
            for (String name : split) {
                BrokerNode tmp = node.getChild(name);
                if (tmp == null) {
                    break;
                }
                node = tmp;
            }
        }
        return node.list(pp, client, rid);
    }

    public static String getBasePath(String[] split) {
        String[] tmp = new String[split.length - 2];
        System.arraycopy(split, 2, tmp, 0, tmp.length);
        return "/" + StringUtils.join(tmp, "/");
    }
}

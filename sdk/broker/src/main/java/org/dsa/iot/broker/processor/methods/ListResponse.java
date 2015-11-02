package org.dsa.iot.broker.processor.methods;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListResponse extends Response {

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
                    if (!pp.isRemote()) {
                        node = null;
                    }
                    break;
                }
                node = tmp;
            }
        }
        if (node != null && node.accessible()) {
            return node.list(pp, client, rid);
        }
        return null;
    }

    public static JsonObject generateRequest(ParsedPath path, int rid) {
        JsonObject req = new JsonObject();
        req.put("method", "list");
        req.put("rid", rid);
        req.put("path", path.base());

        JsonArray reqs = new JsonArray();
        reqs.add(req);
        JsonObject top = new JsonObject();
        top.put("requests", reqs);
        return top;
    }
}

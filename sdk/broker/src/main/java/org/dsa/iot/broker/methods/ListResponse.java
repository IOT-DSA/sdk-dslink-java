package org.dsa.iot.broker.methods;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.utils.Dispatch;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListResponse {

    private final Broker broker;
    private final String path;
    private Integer remoteRid;

    public ListResponse(Broker broker, String path) {
        if (broker == null) {
            throw new NullPointerException("broker");
        } else if (path == null) {
            throw new NullPointerException("path");
        }
        this.broker = broker;
        this.path = path;
    }

    public Integer getRemoteRid() {
        return remoteRid;
    }

    public JsonObject getResponse(Client client, int rid) {
        ParsedPath pp = ParsedPath.parse(broker.getDownstreamName(), path);
        BrokerNode<?> node = broker.getTree().getRoot();
        {
            String[] split = pp.splitPath();
            for (String name : split) {
                BrokerNode tmp = node.getChild(name);
                if (tmp == null) {
                    break;
                }
                node = tmp;
            }
        }
        if (pp.isRemote()) {
            DSLinkNode link = (DSLinkNode) node;
            Client remote = link.getClient();
            if (remote != null) {
                remoteRid = link.getNextRid();
                JsonObject resp = new JsonObject();
                {
                    resp.put("method", "list");
                    resp.put("rid", remoteRid);
                    resp.put("path", getBasePath(pp.splitPath()));
                }

                JsonArray reqs = new JsonArray();
                reqs.add(resp);

                JsonObject top = new JsonObject();
                top.put("requests", reqs);

                remote.addDispatch(getRemoteRid(), new Dispatch(client, rid));
                remote.write(top.encode());
            } else {
                return link.list();
            }
        }
        return pp.isRemote() ? null : node.list();
    }

    public String getBasePath(String[] split) {
        String[] tmp = new String[split.length - 2];
        System.arraycopy(split, 2, tmp, 0, tmp.length);
        return "/" + StringUtils.join(tmp, "/");
    }
}

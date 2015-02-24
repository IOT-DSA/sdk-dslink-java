package org.dsa.iot.broker.overrides;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.responses.ListResponse;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class BrokerListResponse extends ListResponse {
    
    private final String prefix;
    
    public BrokerListResponse(ListRequest request,
                              NodeManager manager,
                              String prefix) {
        super(request, manager);
        this.prefix = prefix;
    }

    @Override
    public void populate(JsonArray array) {
        String reqPath = getRequest().getPath();
        if (prefix != null) {
            if (reqPath.startsWith("/"))
                reqPath = reqPath.substring(1);
            setPath(prefix + "/" + reqPath);
        } else {
            setPath(reqPath);
        }
        Node node = getManager().getNode(getPath(), true).getNode();
        iterate(node, array);
    }
}

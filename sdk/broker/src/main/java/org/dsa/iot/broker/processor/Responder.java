package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.processor.stream.manager.StreamManager;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Responder extends LinkHandler {

    private final StreamManager streamManager = new StreamManager(this);
    private final AtomicInteger rid = new AtomicInteger();

    public Responder(DSLinkNode node) {
        super(node);
    }

    public StreamManager stream() {
        return streamManager;
    }

    public int nextRid() {
        return rid.incrementAndGet();
    }

    public void requesterDisconnected(Client client) {
        Requester req = client.node().processor().requester();
        Map<Integer, Stream> streams = req.getReqStreams();
        if (streams != null) {
            stream().close(client, streams.values());
        }
    }

    public void responderConnected() {
        streamManager.responderConnected();
    }

    public void responderDisconnected() {
        streamManager.responderDisconnected();
    }

    @Override
    protected void process(JsonObject response) {
        Integer rid = response.get("rid");
        StreamState state = StreamState.toEnum((String) response.get("stream"));
        Stream stream;
        if (state == StreamState.CLOSED) {
            stream = streamManager.remove(rid);
        } else {
            stream = streamManager.get(rid);
        }
        if (stream != null) {
            stream.dispatch(state, response);
        }
    }
}

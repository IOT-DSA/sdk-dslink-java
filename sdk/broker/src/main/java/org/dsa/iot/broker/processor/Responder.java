package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.processor.stream.manager.StreamManager;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Responder extends LinkHandler {

    private final StreamManager streamManager = new StreamManager(this);
    private final AtomicInteger rid = new AtomicInteger();
    private final AtomicInteger sid = new AtomicInteger();

    public Responder(DSLinkNode node) {
        super(node);
    }

    public StreamManager stream() {
        return streamManager;
    }

    public int nextRid() {
        return rid.incrementAndGet();
    }

    public int nextSid() {
        return sid.incrementAndGet();
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
        if (rid == null) {
            return;
        } else if (rid == 0) {
            JsonArray updates = response.get("updates");
            stream().sub().dispatch(updates);
            return;
        }

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

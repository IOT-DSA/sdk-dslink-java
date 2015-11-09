package org.dsa.iot.broker.processor.stream;

import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Stream {

    private final Responder responder;
    private final ParsedPath path;

    public Stream(Responder responder, ParsedPath path) {
        this.responder = responder;
        this.path = path;
    }

    public final Responder responder() {
        return responder;
    }

    public final ParsedPath path() {
        return path;
    }

    public void close(Client requester, boolean write) {
        responder().stream().close(requester, this, write);
    }

    public abstract void add(Client requester, int requesterRid);

    public abstract void remove(Client requester);

    public abstract boolean isEmpty();

    public abstract void dispatch(StreamState state, JsonObject response);

    public abstract void responderConnected();

    public abstract void responderDisconnected();
}

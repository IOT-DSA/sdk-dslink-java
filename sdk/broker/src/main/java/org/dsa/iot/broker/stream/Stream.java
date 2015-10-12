package org.dsa.iot.broker.stream;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Stream {

    private final Client responder;
    private final String path;

    public Stream(Client responder, ParsedPath path) {
        this.responder = responder;
        this.path = path.full();
    }

    public final Client responder() {
        return responder;
    }

    public final String path() {
        return path;
    }

    public abstract void add(Client requester, int requesterRid);

    public abstract void remove(Client requester);

    public abstract boolean isEmpty();

    public abstract void dispatch(StreamState state, JsonObject response);
}

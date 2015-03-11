package org.dsa.iot.dslink.requester;

import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.requester.requests.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class RequestTracker {

    private final Map<Integer, Request> reqs = new HashMap<>();
    private int currentID = 0;

    public synchronized int getNextID() {
        return ++currentID;
    }
    
    public synchronized int track(Request req) throws DuplicateException {
        return track(++currentID, req);
    }
    
    public synchronized int track(int rid, Request req)
                                    throws DuplicateException {
        if (reqs.containsKey(rid))
            throw new DuplicateException("request");
        else
            reqs.put(rid, req);
        return rid;
    }

    public synchronized void untrack(int id) {
        reqs.remove(id);
    }

    public synchronized Request getRequest(int id) {
        return reqs.get(id);
    }
}

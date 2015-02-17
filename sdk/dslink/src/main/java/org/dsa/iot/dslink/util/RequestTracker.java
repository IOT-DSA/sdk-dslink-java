package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.requests.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class RequestTracker {

    private final Map<Integer, Request> reqs = new HashMap<>();
    private int currentID = 0;

    public synchronized int track(Request req) {
        reqs.put(++currentID, req);
        return currentID;
    }

    public synchronized void untrack(int id) {
        reqs.remove(id);
    }

    public synchronized Request getRequest(int id) {
        return reqs.get(id);
    }
}

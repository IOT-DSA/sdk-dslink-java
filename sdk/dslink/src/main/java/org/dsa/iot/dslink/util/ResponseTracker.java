package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.exceptions.DuplicateException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class ResponseTracker {

    private final List<Integer> reqs = new ArrayList<>();

    public synchronized boolean isTracking(int id) {
        return reqs.contains(id);
    }

    public synchronized int track(int id) {
        if (reqs.contains(id))
            throw new DuplicateException("ID already being tracked");
        reqs.add(id);
        return id;
    }

    public synchronized void untrack(int id) {
        reqs.remove(id);
    }
}

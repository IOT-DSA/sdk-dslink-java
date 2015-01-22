package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.exceptions.DuplicateException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class RequestTracker {

    private final List<Integer> reqs = new ArrayList<>();

    public boolean isTracking(int id) {
        return reqs.contains(id);
    }

    public void track(int id) {
        if (reqs.contains(id))
            throw new DuplicateException("ID already being tracked");
        reqs.add(id);
    }

    public void untrack(int id) {
        reqs.remove(id);
    }
}

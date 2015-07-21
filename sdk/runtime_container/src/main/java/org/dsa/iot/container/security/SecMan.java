package org.dsa.iot.container.security;

import java.security.Permission;

/**
 * @author Samuel Grenier
 */
public class SecMan extends SecurityManager {

    private final ThreadGroup group;

    public SecMan(ThreadGroup group) {
        this.group = group;
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return group;
    }

    @Override
    public void checkExit(int status) {
        throw new SecurityException("Exit not allowed");
    }

    @Override
    public void checkPermission(Permission perm) {
        // no-op
    }
}

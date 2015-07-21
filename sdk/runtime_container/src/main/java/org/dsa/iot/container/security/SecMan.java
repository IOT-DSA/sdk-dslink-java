package org.dsa.iot.container.security;

import java.security.Permission;

/**
 * @author Samuel Grenier
 */
public class SecMan extends SecurityManager {

    @Override
    public void checkExit(int status) {
        throw new SecurityException("Exit not allowed");
    }

    @Override
    public void checkPermission(Permission perm) {
        // no-op
    }
}

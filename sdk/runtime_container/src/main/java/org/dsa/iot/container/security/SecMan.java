package org.dsa.iot.container.security;

import java.security.Permission;

/**
 * @author Samuel Grenier
 */
public class SecMan extends SecurityManager {

    @Override
    public ThreadGroup getThreadGroup() {
        return super.getThreadGroup();
    }

    @Override
    public void checkExit(int status) {
        throw new SecurityException("Exit not allowed");
    }

    @Override
    public void checkPermission(Permission perm) {
        if (perm instanceof RuntimePermission) {
            if ("setSecurityManager".equals(perm.getName())) {
                String err = "Setting a security manager is not allowed";
                throw new SecurityException(err);
            }
        }
    }
}

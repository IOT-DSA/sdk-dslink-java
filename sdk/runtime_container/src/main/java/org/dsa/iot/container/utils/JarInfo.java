package org.dsa.iot.container.utils;

import java.net.URL;

/**
 * @author Samuel Grenier
 */
public class JarInfo {

    private final URL url;
    private final boolean isNative;

    public JarInfo(URL url, boolean isNative) {
        this.url = url;
        this.isNative = isNative;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isNative() {
        return isNative;
    }
}

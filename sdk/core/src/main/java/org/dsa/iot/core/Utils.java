package org.dsa.iot.core;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

/**
 * @author Samuel Grenier
 */
public class Utils {

    public static final Vertx VERTX;

    /**
     * Gets the default port of a specified protocol URI scheme.
     * @param scheme Scheme to get the default port of.
     * @return Default port of the scheme, or -1 if unsupported.
     */
    public static int getDefaultPort(String scheme) {
        if (scheme == null)
            throw new NullPointerException("scheme");

        if ("ws".equals(scheme) || "http".equals(scheme))
            return 80;
        else if ("wss".equals(scheme) || "https".equals(scheme))
            return 443;
        return -1;
    }

    public static String addPadding(String encoded, boolean urlSafe) {
        String padding = urlSafe ? "." : "=";
        while (encoded.length() % 4 != 0) {
            encoded += padding;
        }
        return encoded;
    }

    static {
        VERTX = VertxFactory.newVertx();
    }
}

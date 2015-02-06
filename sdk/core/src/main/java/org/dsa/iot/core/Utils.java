package org.dsa.iot.core;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;

import java.util.List;
import java.util.Map;

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
        StringBuilder buffer = new StringBuilder(encoded);
        while (buffer.length() % 4 != 0) {
            buffer.append(padding);
        }
        return buffer.toString();
    }

    public static MultiMap parseQueryParams(String uri) {
        if (uri == null)
            return null;
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> prms = queryStringDecoder.parameters();
        MultiMap params = new CaseInsensitiveMultiMap();
        if (!prms.isEmpty()) {
            for (Map.Entry<String, List<String>> entry: prms.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    static {
        VERTX = VertxFactory.newVertx();
    }
}

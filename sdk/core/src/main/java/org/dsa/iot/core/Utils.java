package org.dsa.iot.core;

import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.NonNull;
import lombok.val;
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
    public static int getDefaultPort(@NonNull String scheme) {
        if ("ws".equals(scheme) || "http".equals(scheme))
            return 80;
        else if ("wss".equals(scheme) || "https".equals(scheme))
            return 443;
        return -1;
    }

    public static String addPadding(@NonNull String encoded, boolean urlSafe) {
        val padding = urlSafe ? "." : "=";
        val buffer = new StringBuilder(encoded);
        while (buffer.length() % 4 != 0) {
            buffer.append(padding);
        }
        return buffer.toString();
    }

    public static MultiMap parseQueryParams(String uri) {
        if (uri == null)
            return null;
        val queryStringDecoder = new QueryStringDecoder(uri);
        val decoded = queryStringDecoder.parameters();
        val params = new CaseInsensitiveMultiMap();
        if (!decoded.isEmpty()) {
            for (Map.Entry<String, List<String>> entry: decoded.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    static {
        VERTX = VertxFactory.newVertx();
    }
}

package org.dsa.iot.core;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

/**
 * @author Samuel Grenier
 */
public class Utils {

    public static final Vertx VERTX;

    static {
        VERTX = VertxFactory.newVertx();
    }
}

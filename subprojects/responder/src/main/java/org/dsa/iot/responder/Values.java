package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Values {

    private static final Logger LOGGER;

    public static void init(Node parent) {
        NodeBuilder builder = parent.createChild("values");
        parent = builder.build();

        builder = parent.createChild("number");
        builder.setValue(new Value(0));
        builder.build();

        builder = parent.createChild("bool");
        builder.setValue(new Value(false));
        builder.build();

        builder = parent.createChild("string");
        builder.setValue(new Value("Hello world"));
        builder.build();

        builder = parent.createChild("dynamic");
        builder.setValue(new Value("test", true));
        builder.build();

        builder = parent.createChild("map");
        {
            JsonObject object = new JsonObject();
            object.putString("key", "value");
            builder.setValue(new Value(object));
        }
        builder.build();

        builder = parent.createChild("array");
        {
            JsonArray array = new JsonArray();
            array.addNumber(0);
            array.addString("Hello world");
            builder.setValue(new Value(array));
        }
        builder.build();

        builder = parent.createChild("writable");
        {
            builder.setValue(new Value(0));
            builder.setWritable(Writable.WRITE);
            builder.getListener().setValueHandler(new Handler<Value>() {
                @Override
                public void handle(Value event) {
                    LOGGER.info("Writable has a new value of {}", event);
                }
            });
        }
        builder.build();
    }

    static {
        LOGGER = LoggerFactory.getLogger(Values.class);
    }
}

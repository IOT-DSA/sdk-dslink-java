package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Values {

    public static void start(Node parent) {
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
    }

}

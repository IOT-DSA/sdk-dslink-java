package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
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
        builder.setValueType(ValueType.NUMBER);
        builder.setValue(new Value(0));
        builder.build();

        builder = parent.createChild("bool");
        builder.setValueType(ValueType.BOOL);
        builder.setValue(new Value(false));
        builder.build();

        builder = parent.createChild("string");
        builder.setValueType(ValueType.STRING);
        builder.setValue(new Value("Hello world"));
        builder.build();

        builder = parent.createChild("dynamic");
        builder.setValueType(ValueType.DYNAMIC);
        builder.setValue(new Value("test", true));
        builder.build();

        builder = parent.createChild("map");
        builder.setValueType(ValueType.MAP);
        {
            JsonObject object = new JsonObject();
            object.putString("key", "value");
            builder.setValue(new Value(object));
        }
        builder.build();

        builder = parent.createChild("array");
        builder.setValueType(ValueType.ARRAY);
        {
            JsonArray array = new JsonArray();
            array.addNumber(0);
            array.addString("Hello world");
            builder.setValue(new Value(array));
        }
        builder.build();

        builder = parent.createChild("writable");
        builder.setValueType(ValueType.DYNAMIC);
        {
            builder.setValue(new Value(0, true));
            builder.setWritable(Writable.WRITE);
            builder.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public void handle(ValuePair event) {
                    String from = event.getPrevious().toString();
                    String to = event.getCurrent().toString();
                    LOGGER.info("Writable value changed from `{}` to `{}`", from, to);
                }
            });
        }
        builder.build();
    }

    static {
        LOGGER = LoggerFactory.getLogger(Values.class);
    }
}

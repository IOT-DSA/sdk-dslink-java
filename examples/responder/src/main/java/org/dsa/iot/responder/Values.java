package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        builder.setValue(new Value("test"));
        builder.build();

        builder = parent.createChild("map");
        builder.setValueType(ValueType.MAP);
        {
            JsonObject object = new JsonObject();
            object.put("key", "value");
            builder.setValue(new Value(object));
        }
        builder.build();

        builder = parent.createChild("array");
        builder.setValueType(ValueType.ARRAY);
        {
            JsonArray array = new JsonArray();
            array.add(0);
            array.add("Hello world");
            builder.setValue(new Value(array));
        }
        builder.build();

        builder = parent.createChild("writable");
        builder.setValueType(ValueType.DYNAMIC);
        {
            builder.setValue(new Value(0));
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

        builder = parent.createChild("writable_enum");
        builder.setValueType(ValueType.makeEnum("a", "b", "c"));
        {
            builder.setValue(new Value("a"));
            builder.setWritable(Writable.WRITE);
            builder.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public void handle(ValuePair event) {
                    String from = event.getPrevious().toString();
                    String to = event.getCurrent().toString();
                    LOGGER.info("Writable enum value changed from `{}` to `{}`", from, to);
                }
            });
        }
        builder.build();
    }

    static {
        LOGGER = LoggerFactory.getLogger(Values.class);
    }
}

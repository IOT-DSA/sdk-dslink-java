package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class Echo {

    public static void init(Node superRoot) {
        {
            NodeBuilder builder = superRoot.createChild("echoEnum");
            builder.setAction(getEchoEnumAction());
            builder.build();
        }

        {
            NodeBuilder builder = superRoot.createChild("echoText");
            builder.setAction(getEchoTextAction());
            builder.build();
        }
    }

    private static Action getEchoTextAction() {
        Action a = new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Value v = event.getParameter("text", new Value(""));
                Table t = event.getTable();
                t.addRow(Row.make(v));
            }
        });
        a.addParameter(new Parameter("text", ValueType.STRING)
                        .setDescription("Text to echo")
                        .setPlaceHolder("Hello world!"));
        a.addResult(new Parameter("echo", ValueType.STRING));
        return a;
    }

    private static Action getEchoEnumAction() {
        Action a = new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Value v = event.getParameter("type", new Value(""));
                Table t = event.getTable();
                t.addRow(Row.make(v));
            }
        });
        Set<String> enums = new LinkedHashSet<>();
        enums.add("A");
        enums.add("B");
        enums.add("C");
        a.addParameter(new Parameter("type", ValueType.makeEnum(enums))
                        .setDescription("Enumeration string to echo"));
        a.addResult(new Parameter("echo", ValueType.STRING));
        return a;
    }
}

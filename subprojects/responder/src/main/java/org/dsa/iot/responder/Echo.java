package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class Echo {

    public static void init(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("echo");
        builder.setAction(getEchoAction());
        builder.build();
    }

    private static Action getEchoAction() {
        Action a = new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                JsonArray updates = new JsonArray();
                JsonArray update = new JsonArray();
                update.addString(event.getParameter("type", new Value("")).getString());
                updates.addArray(update);
                event.setUpdates(updates);
            }
        });
        Set<String> enums = new LinkedHashSet<>();
        enums.add("A");
        enums.add("B");
        enums.add("C");
        a.addParameter(new Parameter("type", ValueType.ENUM, new Value(enums)));
        a.addResult(new Parameter("echo", ValueType.STRING));
        return a;
    }
}

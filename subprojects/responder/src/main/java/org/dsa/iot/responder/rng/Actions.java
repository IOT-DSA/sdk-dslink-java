package org.dsa.iot.responder.rng;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Actions {

    private static final Action ADD_ACTION;
    private static final Action REMOVE_ACTION;

    private static final Handler<Node> SUB_HANDLER;
    private static final Handler<Node> UNSUB_HANDLER;

    static Action getAddAction() {
        return ADD_ACTION;
    }

    static Action getRemoveAction() {
        return REMOVE_ACTION;
    }

    static Handler<Node> getSubHandler() {
        return SUB_HANDLER;
    }

    static Handler<Node> getUnsubHandler() {
        return UNSUB_HANDLER;
    }

    static {
        {
            ADD_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Value val = event.getParameter("count", new Value(1));
                    int count = val.getNumber().intValue();
                    if (count < 0) {
                        throw new IllegalArgumentException("count < 0");
                    }

                    RNG rng = event.getNode().getParent().getMetaData();
                    count = rng.addRNG(count);

                    Table t = event.getTable();
                    t.addRow(Row.make(new Value(count)));
                }
            });

            Value def = new Value(1);
            ADD_ACTION.addParameter(new Parameter("count", ValueType.NUMBER, def)
                    .setDescription("How many RNGs to add"));
            ADD_ACTION.addResult(new Parameter("count", ValueType.NUMBER));
        }

        {
            REMOVE_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Value val = event.getParameter("count", new Value(1));
                    int count = val.getNumber().intValue();
                    if (count < 0) {
                        throw new IllegalArgumentException("count < 0");
                    }

                    RNG rng = event.getNode().getParent().getMetaData();
                    count = rng.removeRNG(count);

                    Table t = event.getTable();
                    t.addRow(Row.make(new Value(count)));
                }
            });

            Value def = new Value(1);
            REMOVE_ACTION.addParameter(new Parameter("count", ValueType.NUMBER, def)
                    .setDescription("How many RNGs to remove"));
            REMOVE_ACTION.addResult(new Parameter("count", ValueType.NUMBER));
        }

        SUB_HANDLER = new Handler<Node>() {
            @Override
            public void handle(final Node event) {
                RNG rng = event.getParent().getMetaData();
                rng.subscribe(event);
            }
        };

        UNSUB_HANDLER = new Handler<Node>() {
            @Override
            public void handle(Node event) {
                RNG rng = event.getParent().getMetaData();
                rng.unsubscribe(event);
            }
        };
    }
}

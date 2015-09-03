package org.dsa.iot.commons;

import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.junit.Assert;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

import static org.dsa.iot.commons.ParameterizedAction.ParameterInfo;

/**
 * @author Samuel Grenier
 */
public class ParameterizedActionTest {

    @Test
    public void validateTest() {
        NodeManager manager = new NodeManager(null, null);
        ParameterizedAction a = new ParameterizedAction(Permission.READ) {
            @Override
            public void handle(ActionResult actRes,
                               Map<String, Value> params) {
                Value value = params.get("test");
                Assert.assertTrue(value.getBool());
            }
        };
        {
            ParameterInfo info = new ParameterInfo("test", ValueType.BOOL);
            info.setDefaultValue(new Value(false));
            info.setOptional(true);
            info.setPersistent(true);
            info.setValidator(new Handler<Value>() {
                @Override
                public void handle(Value event) {
                    event.set(true);
                }
            });
            a.addParameter(info);
        }

        JsonObject params = new JsonObject();
        params.putBoolean("test", false);

        JsonObject in = new JsonObject();
        in.putObject("params", params);

        a.invoke(new ActionResult(manager.getSuperRoot(), a, in));
    }

}

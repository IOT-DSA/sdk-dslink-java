package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.Objects;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class InvokeResponse implements Response {

    private final DSLink link;

    private final Node node;
    private final int rid;
    private Table results;
    private ActionResult actionResult;

    public InvokeResponse(DSLink link, int rid, Node node) {
        this.link = link;
        this.rid = rid;
        this.node = node;
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
        results = new Table();
        {
            JsonArray cols = in.getArray("columns");
            if (cols != null) {
                for (Object object : cols) {
                    JsonObject col = (JsonObject) object;
                    String name = col.getString("name");
                    String type = col.getString("type");
                    JsonObject meta = col.getObject("meta");
                    ValueType vt = ValueType.toValueType(type);

                    Parameter p = new Parameter(name, vt);
                    p.setMetaData(meta);
                    results.addColumn(p);
                }
            }
        }
        {
            JsonArray updates = in.getArray("updates");
            if (updates != null) {
                for (Object object : updates) {
                    Row row = new Row();
                    JsonArray rowArray = (JsonArray) object;
                    for (Object rowValue : rowArray) {
                        row.addValue(ValueUtils.toValue(rowValue));
                    }
                    results.addRow(row);
                }
            }
        }
    }

    public Table getTable() {
        return results;
    }

    @Override
    public JsonObject getJsonResponse(final JsonObject in) {
        final Action action = node.getAction();
        if (action == null) {
            throw new RuntimeException("Node not invokable");
        }

        JsonObject out = new JsonObject();
        StreamState streamState = StreamState.INITIALIZED;
        Objects.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                actionResult = new ActionResult(node, in);
                action.invoke(actionResult);

                JsonArray results = new JsonArray();
                Table table = actionResult.getTable();
                List<Row> rows = table.getRows();
                if (rows != null) {
                    rows = new LinkedList<>(rows);
                    for (Row r : rows) {
                        JsonArray row = new JsonArray();
                        List<Value> values = r.getValues();
                        if (values != null) {
                            for (Value v : values) {
                                if (v != null) {
                                    ValueUtils.toJson(row, v);
                                } else {
                                    row.add(null);
                                }
                            }
                        }
                        results.addArray(row);
                    }
                }

                StreamState state = actionResult.getStreamState();
                JsonObject out = new JsonObject();
                out.putNumber("rid", rid);
                out.putString("stream", state.getJsonName());
                processColumns(action, out);
                {
                    Table.Mode mode = table.getMode();
                    if (mode != null) {
                        JsonObject def = new JsonObject();
                        JsonObject meta = out.getObject("meta", def);
                        meta.putString("mode", mode.getName());
                        {
                            JsonObject obj = table.getTableMeta();
                            if (obj != null) {
                                meta.putObject("meta", obj);
                            }
                            table.setTableMeta(null);
                        }
                        out.putObject("meta", meta);
                    }
                }
                out.putArray("updates", results);

                DataHandler writer = link.getWriter();
                writer.writeResponse(out);
                if (state == StreamState.CLOSED) {
                    link.getResponder().removeResponse(rid);
                } else {
                    Handler<Void> ch = actionResult.getCloseHandler();
                    table.setStreaming(rid, writer, ch);
                }
            }
        });

        out.putNumber("rid", rid);
        out.putString("stream", streamState.getJsonName());
        return out;
    }

    @Override
    public JsonObject getCloseResponse() {
        if (actionResult != null) {
            Handler<Void> handler = actionResult.getCloseHandler();
            if (handler != null) {
                handler.handle(null);
            }

            Table table = actionResult.getTable();
            table.setClosed();
        }
        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    private void processColumns(Action act, JsonObject obj) {
        Table table = actionResult.getTable();
        List<Parameter> cols = table.getColumns();
        JsonArray array = null;
        if (!act.isHidden() && cols == null) {
            array = act.getColumns();
        } else if (cols != null) {
            array = new JsonArray();
            for (Parameter p : cols) {
                JsonObject o = new JsonObject();
                o.putString("name", p.getName());
                o.putString("type", p.getType().toJsonString());
                JsonObject meta = p.getMetaData();
                if (meta != null) {
                    o.putObject("meta", meta);
                }
                array.addObject(o);
            }
        }
        if (cols != null) {
            obj.putArray("columns", array);
        }
    }
}

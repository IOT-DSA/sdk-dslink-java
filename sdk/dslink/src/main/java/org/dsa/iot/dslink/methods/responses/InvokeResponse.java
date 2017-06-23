package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Modify;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.actions.table.Table.Mode;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.List;

/**
 * @author Samuel Grenier
 */
public class InvokeResponse extends Response {

    private final DSLink link;

    private final String path;
    private final int rid;

    private Table results;
    private ActionResult actRes;
    private StreamState state;

    public InvokeResponse(DSLink link, int rid, String path) {
        this.link = link;
        this.rid = rid;
        this.path = NodeManager.normalizePath(path, false);
    }

    @Override
    public int getRid() {
        return rid;
    }

    public Table getTable() {
        return results;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public StreamState getState() {
        return state;
    }

    /**
     * Continuously invokes the action. The stream must not be closed.
     *
     * @param params Parameters for the invocation.
     * @see Requester#continuousInvoke
     * @throws IllegalStateException If the stream has been closed.
     */
    public void invoke(JsonObject params) {
        if (state == StreamState.CLOSED) {
            throw new IllegalStateException("Stream already closed");
        }
        link.getRequester().continuousInvoke(getRid(), params);
    }

    public void setStreamState(StreamState state) {
        this.state = state;
    }

    @Override
    public void populate(JsonObject in) {
        if (results == null) {
            results = new Table();
        }
        {
        	JsonObject metaData = in.get("meta");
        	if (metaData != null) {
        		String mode = metaData.get("mode");
        		if (mode != null) {
        			results.setMode(Mode.valueOf(mode.toUpperCase()));
        		}
        		String modify = metaData.get("modify");
        		if (modify != null) {
        			results.setModify(Modify.fromString(modify));
        		}
        		JsonObject tableMeta = metaData.get("meta");
        		if (tableMeta != null) {
        			results.setTableMeta(tableMeta);
        		}
        	}
        }
        {
            JsonArray cols = in.get("columns");
            if (cols != null) {
                for (Object object : cols) {
                    JsonObject col = (JsonObject) object;
                    String name = col.get("name");
                    String type = col.get("type");
                    JsonObject meta = col.get("meta");
                    ValueType vt = ValueType.toValueType(type);

                    Parameter p = new Parameter(name, vt);
                    p.setMetaData(meta);
                    results.addColumn(p);
                }
            }
        }
        {
            JsonArray updates = in.get("updates");
            if (updates != null) {
                for (Object object : updates) {
                    if (object == null) {
                        continue;
                    }

                    Row row = new Row();
                    if (object instanceof JsonArray) {
                        JsonArray rowArray = (JsonArray) object;
                        for (Object rowValue : rowArray) {
                            row.addValue(ValueUtils.toValue(rowValue));
                        }
                    } else if (object instanceof JsonObject) {
                        JsonObject rowObject = (JsonObject) object;
                        for (Parameter col : results.getColumns()) {
                            Object cellValue = rowObject.get(col.getName());
                            row.addValue(ValueUtils.toValue(cellValue));
                        }
                    }
                    results.addRow(row);
                }
            }
        }
    }

    @Override
    public JsonObject getJsonResponse(final JsonObject in) {
        NodeManager man = link.getNodeManager();
        Node node = man.getNode(path, false, false).getNode();
        if (node == null) {
            DSLinkHandler handler = link.getLinkHandler();
            node = handler.onInvocationFail(path);
        }

        final Action action;
        if (node == null || (action = node.getAction()) == null) {
            throw new RuntimeException("Node not invokable at " + path);
        }

        actRes = new ActionResult(node, in);
        action.invoke(actRes);

        final Table table = actRes.getTable();
        final StreamState state = actRes.getStreamState();

        JsonObject out = new JsonObject();
        out.put("rid", rid);
        out.put("stream", state.getJsonName());

        // Handle columns
        processColumns(action, out);

        // Handle mode and metadata
        {
            Table.Mode mode = table.getMode();
            if (mode != null) {
                JsonObject def = new JsonObject();
                JsonObject meta = out.get("meta", def);
                meta.put("mode", mode.getName());
                {
                    JsonObject obj = table.getTableMeta();
                    if (obj != null) {
                        meta.put("meta", obj);
                    }
                    table.setTableMeta(null);
                    
                    Modify modify = table.getModify();
                    if (modify != null) {
                    	meta.put("modify", modify.toString());
                    }
                }
                out.put("meta", meta);
            }
        }

        // Handle results
        {
            JsonArray results = new JsonArray();
            List<Row> rows = table.getRows(true);
            if (rows != null) {
                for (Row r : rows) {
                    JsonArray row = new JsonArray();
                    List<Value> values = r.getValues();
                    if (values != null) {
                        for (Value v : values) {
                            row.add(v);
                        }
                    }
                    results.add(row);
                }
                out.put("updates", results);
            }
        }

        if (state == StreamState.CLOSED) {
            link.getResponder().removeResponse(rid);
        } else {
            Handler<Void> ch = actRes.getCloseHandler();
            DataHandler writer = link.getWriter();
            Responder resp = link.getResponder();
            table.setStreaming(rid, writer, resp, ch);
        }

        return out;
    }

    @Override
    public JsonObject getCloseResponse() {
        if (actRes != null) {
            Handler<Void> handler = actRes.getCloseHandler();
            if (handler != null) {
                handler.handle(null);
            }

            Table table = actRes.getTable();
            table.setClosed();
        }
        JsonObject obj = new JsonObject();
        obj.put("rid", rid);
        obj.put("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    private void processColumns(Action act, JsonObject obj) {
        Table table = actRes.getTable();
        List<Parameter> cols = table.getColumns();
        JsonArray array = null;
        if (!act.isHidden() && cols == null) {
            array = act.getColumns();
        } else if (cols != null) {
            array = new JsonArray();
            for (Parameter p : cols) {
                JsonObject o = new JsonObject();
                o.put("name", p.getName());
                o.put("type", p.getType().toJsonString());
                JsonObject meta = p.getMetaData();
                if (meta != null) {
                    o.put("meta", meta);
                }
                array.add(o);
            }
        }
        if (cols != null) {
            obj.put("columns", array);
        }
    }
}

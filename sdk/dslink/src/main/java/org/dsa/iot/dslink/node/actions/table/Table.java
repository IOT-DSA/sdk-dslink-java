package org.dsa.iot.dslink.node.actions.table;

import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Constructs a table for action results. This class is not thread safe.
 *
 * @author Samuel Grenier
 */
public class Table {

    private List<Parameter> columns;
    private List<Row> rows;

    private int rid;
    private DataHandler writer;
    private Handler<Void> closeHandler;

    /**
     * Adds a column to the table.
     *
     * @param parameter Column to add.
     */
    public void addColumn(Parameter parameter) {
        if (parameter == null) {
            throw new NullPointerException("parameter");
        } else if (columns == null) {
            columns = new LinkedList<>();
        }
        columns.add(parameter);
    }

    /**
     * Adds a row to the internal row buffer or streams it directly to
     * the requester.
     *
     * @param row Row to add to the table.
     */
    public void addRow(Row row) {
        DataHandler writer = this.writer;
        if (row == null) {
            throw new NullPointerException("row");
        } else if (rows == null && writer == null) {
            rows = new LinkedList<>();
        }

        if (writer == null) {
            rows.add(row);
        } else {
            JsonObject obj = new JsonObject();
            obj.putNumber("rid", rid);
            obj.putString("stream", StreamState.OPEN.getJsonName());
            {
                JsonArray rowArray = new JsonArray();
                List<Value> values = row.getValues();
                if (values != null) {
                    for (Value v : values) {
                        if (v != null) {
                            ValueUtils.toJson(rowArray, v);
                        } else {
                            rowArray.add(null);
                        }
                    }
                }

                JsonArray updates = new JsonArray();
                updates.addArray(rowArray);
                obj.putArray("updates", updates);
            }
            writer.writeResponse(obj);
        }
    }

    /**
     * Used to set the table to streaming mode. This is called automatically
     * for any tables that keep their stream open.
     *
     * @param rid Request ID
     * @param writer Writer endpoint
     * @param closeHandler Close handler
     */
    public void setStreaming(int rid,
                             DataHandler writer,
                             Handler<Void> closeHandler) {
        this.rid = rid;
        this.writer = writer;
        this.closeHandler = closeHandler;

        rows = null;
        columns = null;
    }

    /**
     * Closes a streaming table. This is not necessary to call for one-shot
     * tables.
     */
    public void close() {
        DataHandler writer = this.writer;
        if (writer != null) {
            JsonObject obj = new JsonObject();
            obj.putNumber("rid", rid);
            obj.putString("stream", StreamState.CLOSED.getJsonName());
            writer.writeResponse(obj);
            this.writer = null;
            Handler<Void> closeHandler = this.closeHandler;
            if (closeHandler != null) {
                this.closeHandler = null;
                closeHandler.handle(null);
            }
        }
    }

    /**
     *
     * @return Columns of the table.
     */
    public List<Parameter> getColumns() {
        return columns != null ? Collections.unmodifiableList(columns) : null;
    }

    /**
     *
     * @return Rows of the table.
     */
    public List<Row> getRows() {
        return rows != null ? Collections.unmodifiableList(rows) : null;
    }
}

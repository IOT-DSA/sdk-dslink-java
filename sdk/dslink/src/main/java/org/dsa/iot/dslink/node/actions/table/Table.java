package org.dsa.iot.dslink.node.actions.table;

import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

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
    private Mode mode;
    private JsonObject meta;
    private boolean ready;

    private int rid;
    private DataHandler writer;
    private Handler<Void> closeHandler;

    /**
     * Adds a column to the table.
     *
     * @param parameter Column to add.
     */
    public synchronized void addColumn(Parameter parameter) {
        if (parameter == null) {
            throw new NullPointerException("parameter");
        } else if (columns == null) {
            columns = new LinkedList<>();
        }
        columns.add(parameter);
    }

    /**
     * Sets the metadata of the table.
     *
     * @param meta Metadata to set.
     */
    public synchronized void setTableMeta(JsonObject meta) {
        this.meta = meta;
    }

    /**
     * Batch metadata is ignored if the table is not streaming.
     *
     * @param batch Batch of rows.
     */
    public void addBatchRows(BatchRow batch) {
        addBatchRows(null, batch);
    }

    /**
     * Batch metadata is ignored if the table is not streaming.
     *
     * @param cols Columns to dynamically adjust columns of the table.
     * @param batch Batch of rows.
     */
    public synchronized void addBatchRows(List<Parameter> cols, BatchRow batch) {
        DataHandler writer = this.writer;
        if (batch == null) {
            throw new NullPointerException("batch");
        } else if (rows == null && writer == null) {
            rows = new LinkedList<>();
        }

        if (writer == null) {
            setColumns(cols);
            rows.addAll(batch.getRows());
        } else {
            JsonArray updates = new JsonArray();
            for (Row r : batch.getRows()) {
                updates.add(processRow(r));
            }

            BatchRow.Modifier m = batch.getModifier();
            JsonObject meta = null;
            if (m != null) {
                meta = new JsonObject();
                meta.put("modify", batch.getModifier().get());
            }
            write(writer, cols, updates, meta);
        }
    }

    /**
     * Adds a row to the internal row buffer or streams it directly to
     * the requester.
     *
     * @param row Row to add to the table.
     */
    public void addRow(Row row) {
        addRow(null, row);
    }

    /**
     * Adds a row to the internal row buffer or streams it directly to
     * the requester.
     *
     * @param cols Columns to dynamically adjust columns of the table.
     * @param row Row to add to the table.
     */
    public synchronized void addRow(List<Parameter> cols, Row row) {
        DataHandler writer = this.writer;
        if (row == null) {
            throw new NullPointerException("row");
        } else if (rows == null && writer == null) {
            rows = new LinkedList<>();
        }

        if (writer == null) {
            setColumns(cols);
            rows.add(row);
        } else {
            JsonArray updates = new JsonArray();
            updates.add(processRow(row));
            write(writer, cols, updates, null);
        }
    }

    /**
     * Sets the mode of the table.
     *
     * @param mode Mode to set.
     */
    public synchronized void setMode(Mode mode) {
        DataHandler writer = this.writer;
        if (mode == null) {
            throw new NullPointerException("mode");
        } else if (writer == null) {
            this.mode = mode;
        } else {
            JsonObject meta = new JsonObject();
            meta.put("mode", mode.getName());
            write(writer, null, null, meta);
        }
    }

    public synchronized Mode getMode() {
        return mode;
    }

    /**
     * Used to set the table to streaming mode. This is called automatically
     * for any tables that keep their stream open.
     *
     * @param rid Request ID
     * @param writer Writer endpoint
     * @param closeHandler Close handler
     */
    public synchronized void setStreaming(int rid,
                             DataHandler writer,
                             Handler<Void> closeHandler) {
        this.rid = rid;
        this.writer = writer;
        this.closeHandler = closeHandler;

        rows = null;
        columns = null;
        mode = null;
    }

    /**
     * Closes a streaming table. This is not necessary to call for one-shot
     * tables.
     */
    public synchronized void close() {
        DataHandler writer = this.writer;
        if (writer != null) {
            JsonObject obj = new JsonObject();
            obj.put("rid", rid);
            obj.put("stream", StreamState.CLOSED.getJsonName());
            writer.writeResponse(obj);
            this.writer = null;
            Handler<Void> closeHandler = this.closeHandler;
            if (closeHandler != null) {
                this.closeHandler = null;
                closeHandler.handle(null);
            }
        }
        setClosed();
    }

    /**
     * Sets the table to closed if it was streaming without notifying the
     * network. Automatically called when {@link #close} is called or the
     * requester closes the table to prevent unnecessary updates.
     */
    public synchronized void setClosed() {
        this.writer = null;
        this.columns = null;
        this.rows = null;
        this.mode = null;
        this.closeHandler = null;
        this.meta = null;
    }

    /**
     * @return Table metadata.
     */
    public synchronized JsonObject getTableMeta() {
        return meta;
    }

    /**
     *
     * @return Columns of the table.
     */
    public synchronized List<Parameter> getColumns() {
        return columns != null ? Collections.unmodifiableList(columns) : null;
    }

    /**
     * @return The rows in the table.
     */
    public List<Row> getRows() {
        return getRows(false);
    }

    /**
     *
     * @param copy Whether to copy all the rows into a new list.
     * @return Rows of the table.
     */
    public synchronized List<Row> getRows(boolean copy) {
        if (copy) {
            return rows != null ? new LinkedList<>(rows) : null;
        }
        return rows != null ? Collections.unmodifiableList(rows) : null;
    }

    public synchronized void sendReady() {
        if (writer == null) {
            ready = true;
            return;
        }
        JsonObject obj = new JsonObject();
        obj.put("rid", rid);
        obj.put("stream", StreamState.OPEN.getJsonName());
        writer.writeResponse(obj);
    }

    private void setColumns(List<Parameter> cols) {
        if (cols == null) {
            return;
        }
        synchronized (this) {
            if (columns == null) {
                columns = new LinkedList<>(cols);
            } else {
                columns.addAll(cols);
            }
        }
    }

    private void write(DataHandler writer,
                       List<Parameter> cols,
                       JsonArray updates,
                       JsonObject meta) {
        JsonObject obj = new JsonObject();
        obj.put("rid", rid);
        if (ready) {
            ready = false;
            obj.put("stream", StreamState.OPEN.getJsonName());
        }
        if (meta != null) {
            obj.put("meta", meta);
        }
        if (cols != null) {
            JsonArray array = processColumns(cols);
            if (array != null) {
                obj.put("columns", array);
            }
        }
        if (updates != null) {
            obj.put("updates", updates);
        }
        writer.writeResponse(obj);
    }

    private JsonArray processRow(Row row) {
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
        return rowArray;
    }

    private JsonArray processColumns(List<Parameter> cols) {
        if (cols == null || cols.isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray();
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
        return array;
    }

    public enum Mode {
        REFRESH("refresh"),
        APPEND("append"),
        STREAM("stream");

        private final String mode;

        Mode(String mode) {
            this.mode = mode;
        }

        public String getName() {
            return mode;
        }
    }
}

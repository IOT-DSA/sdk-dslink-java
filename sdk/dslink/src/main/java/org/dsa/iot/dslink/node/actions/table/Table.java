package org.dsa.iot.dslink.node.actions.table;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
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
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class Table {

    private List<Parameter> columns;
    private List<Row> rows;
    private Mode mode;
    private Modify modify;
    private JsonObject meta;
    private boolean ready;

    private int rid;
    private DataHandler writer;
    private Responder responder;
    private Handler<Void> closeHandler;
    private Object streamMutex;

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

    public synchronized Mode getMode() {
        return mode;
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

    /**
     * Used to set the table to streaming mode. This is called automatically
     * for any tables that keep their stream open.
     *
     * @param rid Request ID
     * @param writer Writer endpoint
     * @param responder Responder that holds the stream
     * @param closeHandler Close handler
     */
    public synchronized void setStreaming(int rid,
                                          DataHandler writer,
                                          Responder responder,
                                          Handler<Void> closeHandler) {
        this.rid = rid;
        this.writer = writer;
        this.responder = responder;
        this.closeHandler = closeHandler;
        if (ready) {
            sendReady();
        }
        rows = null;
        columns = null;
        mode = null;
        modify = null;
        //Wake up anyone waiting for a stream
        if (streamMutex != null) {
            synchronized (streamMutex) {
                streamMutex.notifyAll();
            }
            //The waiters store the mutex in a local var,
            //so we can clear the field and save a little mem.
            streamMutex = null;
        }
    }

    /**
     * Returns as soon as stream is established or the wait has expired.
     * @param millis How long to wait for a stream, &lt;= 0 means wait forever
     * and is discouraged.
     * @return True if the stream is established.
     */
    public boolean waitForStream(long millis) {
        return waitForStream(millis,false);
    }

        /**
        * Returns as soon as stream is established or the wait has expired.
        * @param millis How long to wait for a stream, &lt;= 0 means wait forever
        * and is discouraged.
        * @param throwException If true, and a stream is not acquired, this will
        * throw an IllegalStateException.
        * @return True if the stream is established.
        */
    @SuppressFBWarnings("WA_NOT_IN_LOOP")
    public boolean waitForStream(long millis, boolean throwException) {
        Object mutex = null;
        synchronized (this) {
            if (writer != null) {
                return true;
            }
            if (streamMutex == null) {
                streamMutex = new Object();
            }
            mutex = streamMutex;
        }
        synchronized (mutex) {
            if (writer == null) {
                try {
                    if (millis <= 0) {
                        mutex.wait();
                    }
                    else {
                        mutex.wait(millis);
                    }
                } catch (Exception ignorable) {}
            }
            if (writer == null) {
                if (throwException) {
                    throw new IllegalStateException("Stream not acquired");
                }
                return false;
            }
            return true;
        }
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
            writer.writeResponse(obj, false);
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
        if (responder != null) {
            responder.removeResponse(rid);
        }
        this.writer = null;
        this.columns = null;
        this.rows = null;
        this.mode = null;
        this.modify = null;
        this.closeHandler = null;
        this.meta = null;
        this.responder = null;
    }

    /**
     * @return Table metadata.
     */
    public synchronized JsonObject getTableMeta() {
        return meta;
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
     * @return "modify" metadata field 
     */
    public synchronized Modify getModify() {
    	return modify;
    }
    
    /**
     * Sets the modify field of the table.
     *
     * @param modify Modify type to set.
     */
    public synchronized void setModify(Modify modify) {
        this.modify = modify;
    }

    /**
     *
     * @return Columns of the table.
     */
    public synchronized List<Parameter> getColumns() {
        return columns != null ? Collections.unmodifiableList(columns) : null;
    }

    private void setColumns(List<Parameter> columns) {
        if (columns == null) {
            return;
        }
        synchronized (this) {
            if (this.columns == null) {
                this.columns = new LinkedList<>(columns);
            } else {
                this.columns.addAll(columns);
            }
        }
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
        ready = false;
        JsonObject obj = new JsonObject();
        obj.put("rid", rid);
        obj.put("stream", StreamState.OPEN.getJsonName());
        writer.writeResponse(obj, false);
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
        writer.writeResponse(obj, false);
    }

    private JsonArray processRow(Row row) {
        JsonArray rowArray = new JsonArray();
        List<Value> values = row.getValues();
        if (values != null) {
            for (Value v : values) {
                rowArray.add(v);
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

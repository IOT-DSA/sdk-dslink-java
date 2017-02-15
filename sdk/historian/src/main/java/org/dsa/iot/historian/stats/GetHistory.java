package org.dsa.iot.historian.stats;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.*;
import org.dsa.iot.dslink.node.actions.table.BatchRow;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.*;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.CompleteHandler;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.historian.database.Database;
import org.dsa.iot.historian.database.Watch;
import org.dsa.iot.historian.stats.interval.IntervalParser;
import org.dsa.iot.historian.stats.interval.IntervalProcessor;
import org.dsa.iot.historian.stats.rollup.Rollup;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.TimeParser;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Samuel Grenier
 */
public class GetHistory implements Handler<ActionResult> {

    private final Database db;
    private final String path;

    public GetHistory(Node node, Database db) {
        Value useNewEncodingMethod = node.getConfig(Watch.USE_NEW_ENCODING_METHOD_CONFIG_NAME);
        if (useNewEncodingMethod == null || !useNewEncodingMethod.getBool()) {
            this.path = node.getName().replaceAll("%2F", "/").replaceAll("%2E", ".");
        } else {
            this.path = StringUtils.decodeName(node.getName());
        }

        this.db = db;
    }

    @Override
    public void handle(final ActionResult event) {
        final Calendar from;
        final Calendar to;
        {
            Value v = event.getParameter("Timerange");
            if (v != null) {
                String range = event.getParameter("Timerange").getString();
                String[] split = range.split("/");
                from = TimeUtils.decode(split[0],null);
                to = TimeUtils.decode(split[1],null);
            } else { // Assume date is today
                from = TimeUtils.alignDay(Calendar.getInstance());
                to = Calendar.getInstance(); //now
            }
        }

        final Value def = new Value("none");
        final String sInterval = event.getParameter("Interval", def).getString();
        final String sRollup = event.getParameter("Rollup", def).getString();
        final boolean rt = event.getParameter("Real Time", new Value(false)).getBool();

        final Table table = event.getTable();
        event.setStreamState(StreamState.INITIALIZED);
        if (rt) {
            table.setMode(Table.Mode.STREAM);
        } else {
            table.setMode(Table.Mode.APPEND);
        }

        IntervalParser parser = IntervalParser.parse(sInterval);
        Rollup.Type type = Rollup.Type.toEnum(sRollup);
        process(event, from, to, rt, type, parser);
    }

    protected void process(final ActionResult event,
                           final Calendar from,
                           final Calendar to,
                           final boolean realTime,
                           final Rollup.Type rollup,
                           final IntervalParser parser) {
        final IntervalProcessor interval = IntervalProcessor.parse(
                parser, rollup, from.getTimeZone());
        LoopProvider.getProvider().schedule(new Runnable() {

            private boolean open = true;
            Handler<QueryData> handler;

            @Override
            public void run() {
                final Table table = event.getTable();
                final StringBuilder buffer = new StringBuilder();
                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(from.getTimeZone());
                event.setCloseHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void ignored) {
                        open = false;
                        if (handler != null) {
                            Watch w = event.getNode().getParent().getMetaData();
                            w.removeHandler(handler);
                        }
                    }
                });

                query(from.getTimeInMillis(), to.getTimeInMillis(), rollup, parser,
                        new CompleteHandler<QueryData>() {

                    private List<QueryData> updates = new LinkedList<>();

                    @Override
                    public void handle(QueryData data) {
                        List<QueryData> updates = this.updates;
                        if (updates != null) {
                            updates.add(data);
                            if (updates.size() >= 500) {
                                processQueryData(table, interval, updates, calendar, buffer);
                            }
                        }
                    }

                    @Override
                    public void complete() {
                        if (!updates.isEmpty()) {
                            processQueryData(table, interval, updates, calendar, buffer);
                        }
                        updates = null;

                        if (!realTime) {
                            if (interval != null) {
                                Row row = interval.complete();
                                if (row != null) {
                                    table.addRow(row);
                                }
                            }
                            table.close();
                        } else if (open) {
                            handler = new Handler<QueryData>() {
                                @Override
                                public void handle(QueryData event) {
                                    processQueryData(table, interval, event, calendar, buffer);
                                }
                            };
                            table.sendReady();
                            Watch w = event.getNode().getParent().getMetaData();
                            w.addHandler(handler);
                        }
                    }
                });
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    protected void query(long from,
                         long to,
                         Rollup.Type type,
                         IntervalParser parser,
                         CompleteHandler<QueryData> handler) {
        db.query(path, from, to, handler);
    }

    protected void processQueryData(Table table,
                                    IntervalProcessor interval,
                                    Collection<QueryData> data,
                                    Calendar calendar,
                                    StringBuilder buffer) {
        if (data.isEmpty()) {
            return;
        }
        BatchRow batch = null;
        Iterator<QueryData> it = data.iterator();
        while (it.hasNext()) {
            QueryData update = it.next();
            it.remove();
            Row row;
            long time = update.getTimestamp();
            if (interval == null) {
                row = new Row();
                calendar.setTimeInMillis(time);
                buffer.setLength(0);
                String t = TimeUtils.encode(calendar,true,buffer).toString();
                row.addValue(new Value(t));
                row.addValue(update.getValue());
            } else {
                row = interval.getRowUpdate(update, time);
            }

            if (row != null) {
                if (batch == null) {
                    batch = new BatchRow();
                }
                batch.addRow(row);
            }
        }
        if (batch != null) {
            table.waitForStream(5000, true);
            table.addBatchRows(batch);
        }
    }

    protected void processQueryData(Table table,
                                    IntervalProcessor interval,
                                    QueryData data,
                                    Calendar calendar,
                                    StringBuilder buffer) {
        if (data == null) {
            return;
        }
        Row row;
        long time = data.getTimestamp();
        if (interval == null) {
            row = new Row();
            calendar.setTimeInMillis(time);
            buffer.setLength(0);
            String t = TimeUtils.encode(calendar,true,buffer).toString();
            row.addValue(new Value(t));
            row.addValue(data.getValue());
        } else {
            row = interval.getRowUpdate(data, time);
        }

        if (row != null) {
            table.addRow(row);
        }
    }

    public static void initAction(Node node, Database db) {
        initAction(node, new GetHistory(node, db));
    }

    public static void initAction(Node node, GetHistory history) {
        Action a =  new Action(Permission.READ, history);
        a.setHidden(true);

        NodeBuilder b = node.createChild("getHistory", "getHistory_");
        b.setDisplayName("Get History");
        b.setSerializable(false);
        b.setAction(a);
        b.build();
    }

    public static void initProfile(Action act) {
        {
            Parameter param = new Parameter("Timerange", ValueType.STRING);
            param.setEditorType(EditorType.DATE_RANGE);
            act.addParameter(param);
        }

        {
            Parameter param = new Parameter("Interval", ValueType.STRING);
            param.setDefaultValue(new Value("none"));
            act.addParameter(param);
        }

        {
            Set<String> enums = new LinkedHashSet<>();
            enums.add("none");
            enums.add("and");
            enums.add("or");
            enums.add("avg");
            enums.add("min");
            enums.add("max");
            enums.add("sum");
            enums.add("first");
            enums.add("last");
            enums.add("count");
            enums.add("delta");
            ValueType e = ValueType.makeEnum(enums);
            Parameter param = new Parameter("Rollup", e);
            act.addParameter(param);
        }

        {
            Parameter param = new Parameter("Real Time", ValueType.BOOL);
            param.setDefaultValue(new Value(false));
            act.addParameter(param);
        }

        {
            Parameter param = new Parameter("timestamp", ValueType.TIME);
            act.addResult(param);
        }

        {
            Parameter param = new Parameter("value", ValueType.DYNAMIC);
            act.addResult(param);
        }

        act.setResultType(ResultType.STREAM);
    }

}

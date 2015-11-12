package org.dsa.iot.historian.stats;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.*;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.handler.CompleteHandler;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.historian.database.Database;
import org.dsa.iot.historian.database.Watch;
import org.dsa.iot.historian.stats.interval.IntervalParser;
import org.dsa.iot.historian.stats.interval.IntervalProcessor;
import org.dsa.iot.historian.stats.rollup.Rollup;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.TimeParser;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Samuel Grenier
 */
public class GetHistory implements Handler<ActionResult> {

    private final Database db;
    private final String path;

    public GetHistory(Node node, Database db) {
        this.path = StringUtils.decodeName(node.getName());
        this.db = db;
    }

    @Override
    public void handle(final ActionResult event) {
        final long from;
        final long to;
        {
            Value v = event.getParameter("Timerange");
            if (v != null) {
                String range = event.getParameter("Timerange").getString();
                String[] split = range.split("/");

                final String sFrom = split[0];
                final String sTo = split[1];

                from = TimeParser.parse(sFrom);
                to = TimeParser.parse(sTo);
            } else {
                // Assume date is today
                Calendar c = Calendar.getInstance();
                Date date = new Date();
                c.setTime(date);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                from = c.getTime().getTime();
                to = date.getTime();
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
                           final long from,
                           final long to,
                           final boolean realTime,
                           final Rollup.Type rollup,
                           final IntervalParser parser) {
        final IntervalProcessor interval = IntervalProcessor.parse(parser, rollup);
        ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
        stpe.execute(new Runnable() {

            private boolean open = true;
            Handler<QueryData> handler;

            @Override
            public void run() {
                final Table table = event.getTable();
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

                query(from, to, rollup, parser, new CompleteHandler<QueryData>() {
                    @Override
                    public void handle(QueryData data) {
                        processQueryData(table, interval, data);
                    }

                    @Override
                    public void complete() {
                        table.sendReady();
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
                                    processQueryData(table, interval, event);
                                }
                            };
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
                                    QueryData data) {
        Row row;
        Value value = data.getValue();
        long time = data.getTimestamp();
        if (interval == null) {
            row = new Row();
            String t = TimeParser.parse(time);
            row.addValue(new Value(t));
            row.addValue(value);
        } else {
            row = interval.getRowUpdate(value, time);
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

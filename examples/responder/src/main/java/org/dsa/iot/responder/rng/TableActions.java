package org.dsa.iot.responder.rng;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.ResultType;
import org.dsa.iot.dslink.node.actions.table.BatchRow;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.responder.util.FutureCloseHandler;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class TableActions {

    private static final Action TABLE_ACTION;
    private static final Action TABLE_STREAM_ACTION;
    private static final Action TABLE_REFRESH_ACTION;
    private static final Action TABLE_REPLACE_ACTION;

    public static void init(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("tableStream");
        builder.setAction(TABLE_STREAM_ACTION);
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("tableRefresh");
        builder.setAction(TABLE_REFRESH_ACTION);
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("tableReplace");
        builder.setAction(TABLE_REPLACE_ACTION);
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("table");
        builder.setAction(TABLE_ACTION);
        builder.setSerializable(false);
        builder.build();
    }

    static {
        {
            TABLE_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Table table = event.getTable();
                    table.addRow(Row.make(new Value("a"), new Value(1)));
                    table.addRow(Row.make(new Value("b"), new Value(2)));
                    table.addRow(Row.make(new Value("c"), new Value(3)));
                }
            });
            TABLE_ACTION.addResult(new Parameter("letter", ValueType.STRING));
            TABLE_ACTION.addResult(new Parameter("number", ValueType.NUMBER));
            TABLE_ACTION.setResultType(ResultType.TABLE);
        }

        {
            TABLE_STREAM_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {

                private ScheduledFuture<?> future;

                @Override
                public void handle(final ActionResult event) {
                    event.setStreamState(StreamState.INITIALIZED);
                    event.getTable().sendReady();
                    ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
                    future = stpe.scheduleWithFixedDelay(new Runnable() {

                        private int counter = 0;

                        @Override
                        public void run() {
                            Table t = event.getTable();
                            t.addRow(Row.make(new Value(counter)));
                            if (counter++ == 10) {
                                t.close();
                            }
                        }
                    }, 0, 300, TimeUnit.MILLISECONDS);
                    event.setCloseHandler(new FutureCloseHandler(future));
                }
            });
            TABLE_STREAM_ACTION.addResult(new Parameter("number", ValueType.STRING));
            TABLE_STREAM_ACTION.setResultType(ResultType.STREAM);
        }

        {
            TABLE_REFRESH_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {

                private ScheduledFuture<?> future;

                @Override
                public void handle(final ActionResult event) {
                    event.setStreamState(StreamState.INITIALIZED);
                    ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
                    event.getTable().setMode(Table.Mode.REFRESH);
                    event.getTable().sendReady();
                    future = stpe.scheduleWithFixedDelay(new Runnable() {

                        private final Random random = new Random();

                        @Override
                        public void run() {
                            Table t = event.getTable();

                            BatchRow row = new BatchRow();
                            for (int i = 0; i < 10; ++i) {
                                row.addRow(Row.make(new Value(random.nextInt())));
                            }
                            t.addBatchRows(row);
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                    event.setCloseHandler(new FutureCloseHandler(future));
                }
            });
            TABLE_REFRESH_ACTION.addResult(new Parameter("number", ValueType.STRING));
            TABLE_REFRESH_ACTION.setResultType(ResultType.STREAM);
        }

        {
            TABLE_REPLACE_ACTION = new Action(Permission.READ, new Handler<ActionResult>() {

                private ScheduledFuture<?> future;

                @Override
                public void handle(final ActionResult event) {
                    event.setStreamState(StreamState.INITIALIZED);
                    final Random random = new Random();
                    event.getTable().setMode(Table.Mode.APPEND);
                    event.getTable().sendReady();
                    for (int i = 0; i < 10; ++i) {
                        Value value = new Value(random.nextInt());
                        event.getTable().addRow(Row.make(value));
                    }
                    ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
                    future = stpe.scheduleWithFixedDelay(new Runnable() {

                        private int counter;

                        @Override
                        public void run() {
                            Table t = event.getTable();

                            BatchRow batch = new BatchRow();
                            {
                                counter = (counter + 1) % 10;
                                Value value = new Value(random.nextInt());
                                batch.addRow(Row.make(value));

                                int i = counter;
                                BatchRow.Modifier m = BatchRow.Modifier.makeReplace(i, i);
                                batch.setModifier(m);
                            }
                            t.addBatchRows(batch);
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                    event.setCloseHandler(new FutureCloseHandler(future));
                }
            });
            TABLE_REPLACE_ACTION.addResult(new Parameter("number", ValueType.STRING));
            TABLE_REPLACE_ACTION.setResultType(ResultType.STREAM);
        }
    }

}

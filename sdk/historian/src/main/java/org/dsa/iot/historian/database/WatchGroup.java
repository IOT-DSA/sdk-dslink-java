package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.WatchUpdate;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Samuel Grenier
 */
public class WatchGroup {
    private static final int MINIMUM_AMOUNT_OF_THREADS = 3;
    private static final long DEFAULT_INTERVAL_IN_SECONDS = 5;
    private static final int DEFAULT_BUFFER_FLUSH_TIME_IN_SECONDS = 5;
    private static final LoggingType DEFAULT_LOGGING_TYPE = LoggingType.ALL_DATA;

    private final Permission permission;
    private final Database db;
    private final Node node;

    private final Queue<WatchUpdate> queue = new ConcurrentLinkedDeque<>();
    private final Object writeLoopLock = new Object();
    private final ScheduledExecutorService intervalScheduler;

    private ScheduledFuture<?> bufferFut;
    private List<Watch> watches = new ArrayList<>();
    private ScheduledFuture<?> scheduledIntervalWriter;
    private LoggingType loggingType = DEFAULT_LOGGING_TYPE;
    private long interval = DEFAULT_INTERVAL_IN_SECONDS;
    private int bufferFlushTime = DEFAULT_BUFFER_FLUSH_TIME_IN_SECONDS;

    /**
     * @param perm Permission all actions should be set to.
     * @param node Watch group node.
     * @param db   Database the group writes to.
     */
    public WatchGroup(Permission perm, Node node, Database db) {
        this.permission = perm;
        this.node = node;
        this.db = db;

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        intervalScheduler = Executors.newScheduledThreadPool(Math.min(MINIMUM_AMOUNT_OF_THREADS, availableProcessors));
    }

    public void close() {
        if (bufferFut != null) {
            bufferFut.cancel(true);
        }
    }

    /**
     * Watches should never write data to the database directly
     * unless absolutely necessary.
     *
     * @return The database the group operates on.
     */
    public Database getDb() {
        return db;
    }

    /**
     * Writes to the database based on the watch group settings.
     *
     * @param watch Watch to write from.
     * @param sv    Subscription update received from the server.
     */
    public void write(Watch watch, SubscriptionValue sv) {
        boolean doWrite = false;
        switch (loggingType) {
            case ALL_DATA: {
                doWrite = true;
                break;
            }
            case INTERVAL: {
                break;
            }
            case POINT_CHANGE: {
                Value curr = watch.getLastValue();
                Value update = sv.getValue();
                if ((curr != null && update == null) || (curr == null && update != null) || (curr != null && !curr.equals(update))) {
                    doWrite = true;
                    watch.setLastValue(update);
                }
                break;
            }
        }

        if (doWrite) {
            WatchUpdate update = new WatchUpdate(watch, sv);
            if (bufferFut != null) {
                queue.add(update);
                return;
            } else if (!queue.isEmpty()) {
                handleQueue();
            }
            dbWrite(update);
            watch.handleLastWritten(sv.getValue());
        }
    }

    private void writeWatchesToBuffer() {
        for (Watch watch : watches) {
            if (!watch.isEnabled()) {
               continue;
            }

            WatchUpdate update = watch.getLastWatchUpdate();

            if (update != null) {
                addWatchUpdateToBuffer(update);
            }
        }
    }

    /**
     * Initializes the watch for the group.
     *
     * @param path Watch path.
     */
    protected void initWatch(String path) {
        Watch watch;
        {
            NodeBuilder b = node.createChild(path);
            b.setValueType(ValueType.DYNAMIC);
            b.setValue(null);
            Node n = b.build();
            watch = new Watch(this, n);
            watch.init(permission);
            n.setMetaData(watch);
            db.getProvider().onWatchAdded(watch);
        }

        scheduleWriteToBuffer();
    }


    private void scheduleWriteToBuffer() {
        if (!LoggingType.INTERVAL.equals(loggingType)) {
            return;
        }

        if (scheduledIntervalWriter == null || scheduledIntervalWriter.isDone()) {
            scheduledIntervalWriter = intervalScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    writeWatchesToBuffer();
                }
            }, 0, interval, TimeUnit.SECONDS);
        }
    }

    /**
     * Subscribes to the entire watch group.
     */
    public void subscribe() {
        Map<String, Node> children = node.getChildren();
        for (Node n : children.values()) {
            if (n.getAction() == null) {
                initWatch(n.getName().replaceAll("%2F", "/").replaceAll("%2E", "."));
            }
        }

        scheduleWriteToBuffer();
        scheduleBufferFlush();
    }

    /**
     * Unsubscribes from the entire watch group.
     */
    public void unsubscribe() {
        Map<String, Node> children = node.getChildren();

        cancelIntervalScheduler();

        for (Node n : children.values()) {
            if (n.getAction() == null) {
                Watch w = n.getMetaData();
                w.unsubscribe();
            }
        }
    }

    protected void initSettings() {
        useExistingValuesForEditAction();

        {
            NodeBuilder b = node.createChild("addWatchPath");
            b.setDisplayName("Add Watch Path");
            {
                Action a = new Action(permission, new Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult event) {
                        ValueType vt = ValueType.STRING;
                        Value v = event.getParameter("Path", vt);
                        String path = v.getString();
                        initWatch(path);

                    }
                });
                {
                    Parameter p = new Parameter("Path", ValueType.STRING);
                    p.setDescription("Path to start watching for value changes");
                    a.addParameter(p);
                }
                b.setAction(a);
            }
            b.build();
        }
        {
            NodeBuilder b = node.createChild("edit");
            b.setDisplayName("Edit");
            // Buffer flush time
            b.setRoConfig("bft", new Value(bufferFlushTime));
            // Logging type
            b.setRoConfig("lt", new Value(loggingType.getName()));
            // Interval
            b.setRoConfig("i", new Value(interval));

            final Parameter bft;
            {
                bft = new Parameter("Buffer Flush Time", ValueType.NUMBER);
                {
                    String desc = "Buffer flush time controls the interval ";
                    desc += "when data gets written into the database\n";
                    desc += "Setting a time to 0 means to record data ";
                    desc += "immediately";
                    bft.setDescription(desc);
                }
                bft.setDefaultValue(new Value(bufferFlushTime));
            }

            final Parameter lt;
            {
                Set<String> enums = LoggingType.buildEnums();
                lt = new Parameter("Logging Type", ValueType.makeEnum(enums));
                lt.setDefaultValue(new Value(loggingType.getName()));
                {
                    String desc = "Logging type controls what kind of data ";
                    desc += "gets stored into the database";
                    lt.setDescription(desc);
                }
            }

            final Parameter i;
            {
                i = new Parameter("Interval", ValueType.NUMBER);
                {
                    String desc = "Interval controls how long to wait before ";
                    desc += "buffering the next value update.\n";
                    desc += "This setting has no effect when logging type is ";
                    desc += "not interval.";
                    i.setDescription(desc);
                }
                i.setDefaultValue(new Value(interval));
            }

            EditSettingsHandler handler = new EditSettingsHandler();
            {
                handler.setBufferFlushTimeParam(bft);
                handler.setLoggingTypeParam(lt);
                handler.setIntervalParam(i);

                Action a = new Action(permission, handler);
                a.addParameter(bft);
                a.addParameter(lt);
                a.addParameter(i);
                handler.setAction(a);

                b.setAction(a);
            }

            b.build();
        }
        {
            NodeBuilder b = node.createChild("delete");
            b.setDisplayName("Delete");
            b.setAction(new Action(permission, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Node node = event.getNode().getParent();
                    unsubscribe();
                    node.delete();
                }
            }));
            b.build();
        }

        createRestoreGetHistoryAction();

        if (LoggingType.INTERVAL.equals(loggingType)) {
            scheduleWriteToBuffer();
            scheduleBufferFlush();
        }
    }

    private void createRestoreGetHistoryAction() {
        NodeBuilder nodeBuilder = node.createChild("restoreGetHistoryAction");
        nodeBuilder.setDisplayName("Restore GetHistory aliases");
        nodeBuilder.setAction(new Action(permission, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                for (Watch watch : watches) {
                    watch.addGetHistoryActionAlias();
                }
            }
        }));

        nodeBuilder.build();
    }

    private void useExistingValuesForEditAction() {
        Node existingEditNode = node.getChild("edit");

        if (existingEditNode == null) {
            return;
        }

        Value bufferFlushTime = existingEditNode.getRoConfig("bft");
        if (bufferFlushTime != null) {
            this.bufferFlushTime = bufferFlushTime.getNumber().intValue();
        }

        Value loggingType = existingEditNode.getRoConfig("lt");
        if (loggingType != null) {
            this.loggingType = LoggingType.toEnum(loggingType.getString());
        }

        Value interval = existingEditNode.getRoConfig("i");
        if (interval != null) {
            this.interval = interval.getNumber().longValue();
        }
    }

    private void scheduleBufferFlush() {
        synchronized (writeLoopLock) {
            if (bufferFut != null) {
                bufferFut.cancel(false);
                bufferFut = null;
            }

            if (this.bufferFlushTime <= 0) {
                return;
            }

            bufferFut = LoopProvider.getProvider().schedulePeriodic(new Runnable() {
                @Override
                public void run() {
                    handleQueue();
                }
            }, this.bufferFlushTime, this.bufferFlushTime, TimeUnit.SECONDS);
        }
    }

    private void handleQueue() {
        int size = queue.size();

        WatchUpdate update = null;
        for (int i = 0; i < size; ++i) {
            update = queue.poll();
            dbWrite(update);
        }

        if (update != null) {
            Value value = update.getUpdate().getValue();
            update.getWatch().handleLastWritten(value);
        }
    }

    private void dbWrite(WatchUpdate update) {
        Value value = update.getUpdate().getValue();
        if (value != null) {
            long time;
            if (LoggingType.INTERVAL == loggingType) {
                time = update.getIntervalTimestamp();
            } else {
                time = value.getTime();
            }
            Watch watch = update.getWatch();
            db.write(watch.getPath(), value, time);
            watch.notifyHandlers(new QueryData(value, time));
        }
    }

    public boolean canWriteOnNewData() {
        return !LoggingType.INTERVAL.equals(loggingType);
    }

    public synchronized void addWatchUpdateToBuffer(WatchUpdate watchUpdate) {
        queue.add(watchUpdate);

        long nowTimestamp = new Date().getTime();
        watchUpdate.updateTimestamp(nowTimestamp);
    }

    private void cancelBufferWrite() {
        if (bufferFut != null) {
            bufferFut.cancel(true);
        }
        queue.clear();
    }

    public void cancelIntervalScheduler() {
        if (scheduledIntervalWriter != null) {
            scheduledIntervalWriter.cancel(true);
        }
        cancelBufferWrite();
    }

    public void addWatch(Watch watch) {
        watches.add(watch);

        db.getProvider().getPool().subscribe(watch.getPath(), watch);
    }

    public void removeFromWatches(Watch watch) {
        watches.remove(watch);
    }

    private class EditSettingsHandler implements Handler<ActionResult> {
        private Action action;

        private Parameter bufferFlushTimeParameter;
        private Parameter loggingTypeParameter;
        private Parameter intervalInSecondsParameter;

        public void setAction(Action a) {
            this.action = a;
        }

        public void setBufferFlushTimeParam(Parameter bufferFlushTime) {
            this.bufferFlushTimeParameter = bufferFlushTime;
        }

        public void setLoggingTypeParam(Parameter loggingType) {
            this.loggingTypeParameter = loggingType;
        }

        public void setIntervalParam(Parameter intervalInSeconds) {
            this.intervalInSecondsParameter = intervalInSeconds;
        }

        @Override
        public void handle(ActionResult event) {
            Node node = event.getNode();

            cancelIntervalScheduler();

            Value loggingTypeValue = event.getParameter(loggingTypeParameter.getName(), ValueType.STRING);

            Value bufferFlushTimeValue = event.getParameter(bufferFlushTimeParameter.getName(), bufferFlushTimeParameter.getType());
            if (bufferFlushTime < 0) {
                bufferFlushTimeValue.set(0);
            }

            Value intervalInSecondsAsValue = event.getParameter(intervalInSecondsParameter.getName(), intervalInSecondsParameter.getType());
            if (intervalInSecondsAsValue.getNumber().intValue() < 0) {
                intervalInSecondsAsValue.set(0);
            }

            node.setRoConfig("bft", bufferFlushTimeValue);
            bufferFlushTimeParameter.setDefaultValue(bufferFlushTimeValue);
            bufferFlushTime = bufferFlushTimeValue.getNumber().intValue();

            node.setRoConfig("lt", loggingTypeValue);
            loggingTypeParameter.setDefaultValue(loggingTypeValue);
            loggingType = LoggingType.toEnum(loggingTypeValue.getString());

            node.setRoConfig("i", intervalInSecondsAsValue);
            intervalInSecondsParameter.setDefaultValue(intervalInSecondsAsValue);
            interval = intervalInSecondsAsValue.getNumber().longValue();

            List<Parameter> params = new LinkedList<>();
            params.add(bufferFlushTimeParameter);
            params.add(loggingTypeParameter);
            params.add(intervalInSecondsParameter);
            action.setParams(params);

            scheduleBufferFlush();
            scheduleWriteToBuffer();
        }
    }
}

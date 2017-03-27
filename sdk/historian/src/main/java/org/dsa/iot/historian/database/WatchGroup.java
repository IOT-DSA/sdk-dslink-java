package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.ResultType;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.StringUtils;
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
    private final List<Watch> watches = new ArrayList<>();

    private ScheduledFuture<?> bufferFut;
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

    private void writeWatchesToBuffer(Date nowTimestamp) {
        for (Watch watch : watches) {
            if (!watch.isEnabled()) {
                continue;
            }

            WatchUpdate update = watch.getLastWatchUpdate();
            if (update != null) {
                addWatchUpdateToBuffer(update, nowTimestamp);
            }
        }
    }

    /**
     * Initializes the watch for the group.
     *
     * @param path Watch path.
     * @param useNewEncodingMethod
     */
    protected void initWatch(String path, boolean useNewEncodingMethod) {
        NodeBuilder b = node.createChild(path);
        b.setValueType(ValueType.DYNAMIC);
        b.setValue(null);
        b.setConfig("useNewEncodingMethod", new Value(useNewEncodingMethod));
        Node n = b.build();
        Watch watch = new Watch(this, n);
        watch.init(permission, db);
        n.setMetaData(watch);
        db.getProvider().onWatchAdded(watch);

        scheduleWriteToBuffer();
        scheduleBufferFlush();
    }

    private void scheduleWriteToBuffer() {
        if (!LoggingType.INTERVAL.equals(loggingType)) {
            return;
        }

        if (scheduledIntervalWriter == null || scheduledIntervalWriter.isDone()) {
            long now = new Date().getTime();
            long initialDelayBeforeLogging = findInitialDelayOfLogging(now);
            scheduledIntervalWriter = intervalScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    writeWatchesToBuffer(new Date());
                }
            }, initialDelayBeforeLogging, interval * 1000, TimeUnit.MILLISECONDS);
        }
    }

    private long findInitialDelayOfLogging(long now) {
        return interval * 1000 - now % (interval * 1000);
    }

    /**
     * Subscribes to the entire watch group.
     */
    public void subscribe() {
        Map<String, Node> children = node.getChildren();
        for (Node n : children.values()) {
            if (n.getAction() == null) {
                Value useNewEncodingMethod = n.getConfig(Watch.USE_NEW_ENCODING_METHOD_CONFIG_NAME);
                if (useNewEncodingMethod == null || !useNewEncodingMethod.getBool()) {
                    String path = n.getName().replaceAll("%2F", "/").replaceAll("%2E", ".");
                    initWatch(path, false);
                } else {
                    String path = StringUtils.decodeName(n.getName());
                    initWatch(path, true);
                }
            }
        }
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

        createAddWatchAction();
        createEditAction();
        createDeleteAction();
        createRestoreGetHistoryAction();

        if (LoggingType.INTERVAL.equals(loggingType)) {
            scheduleWriteToBuffer();
            scheduleBufferFlush();
        }
    }

    private void createDeleteAction() {
        NodeBuilder deleteBuilder = node.createChild("delete");
        deleteBuilder.setDisplayName("Delete");
        deleteBuilder.setAction(new Action(permission, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node node = event.getNode().getParent();
                unsubscribe();
                node.delete(false);
            }
        }));
        deleteBuilder.build();
    }

    private void createEditAction() {
        NodeBuilder editBuilder = node.createChild("edit");
        editBuilder.setDisplayName("Edit");
        // Buffer flush time
        editBuilder.setRoConfig("bft", new Value(bufferFlushTime));
        // Logging type
        editBuilder.setRoConfig("lt", new Value(loggingType.getName()));
        // Interval
        editBuilder.setRoConfig("i", new Value(interval));

        final Parameter bufferFlushTime = createBufferFlushTimeParameter();
        final Parameter loggingTypeParameter = createLoggingTypeParameter();
        final Parameter intervalParameter = createIntervalParameter();
        Action editAction = createEditAction(bufferFlushTime, loggingTypeParameter, intervalParameter);

        editBuilder.setAction(editAction);
        editBuilder.build();
    }

    private void createAddWatchAction() {
        NodeBuilder addWatchPathBuilder = node.createChild("addWatchPath");
        addWatchPathBuilder.setDisplayName("Add Watch Path");
        Action addWatchPathAction = new Action(permission, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                ValueType valueType = ValueType.STRING;
                Value pathValue = event.getParameter("Path", valueType);
                String path = pathValue.getString();

                event.getTable().addColumn(new Parameter("Success", ValueType.BOOL));
                if (node.hasChild(path)) {
                    event.getTable().addColumn(new Parameter("Message", ValueType.STRING));
                    Row make = Row.make(new Value(false),
                            new Value("Couldn't watch the path " +
                                    path + " because it is already watched in this Watch Group."));
                    event.getTable().addRow(make);
                } else {
                    initWatch(path, true);
                    event.getTable().addRow(Row.make(new Value(true)));
                }
            }
        });

        addWatchPathAction.setResultType(ResultType.TABLE);

        Parameter pathParameter = new Parameter("Path", ValueType.STRING);
        pathParameter.setDescription("Path to start watching for value changes");
        addWatchPathAction.addParameter(pathParameter);

        addWatchPathBuilder.setAction(addWatchPathAction);
        addWatchPathBuilder.build();
    }

    private Action createEditAction(Parameter bufferFlushTime, Parameter loggingTypeParameter, Parameter intervalParameter) {
        EditSettingsHandler editSettingsHandler = new EditSettingsHandler();
        Action editAction = new Action(permission, editSettingsHandler);
        editSettingsHandler.setAction(editAction);
        editSettingsHandler.setBufferFlushTimeParam(bufferFlushTime);
        editSettingsHandler.setLoggingTypeParam(loggingTypeParameter);
        editSettingsHandler.setIntervalParam(intervalParameter);

        editAction.addParameter(bufferFlushTime);
        editAction.addParameter(loggingTypeParameter);
        editAction.addParameter(intervalParameter);
        return editAction;
    }

    private Parameter createIntervalParameter() {
        final Parameter intervalParameter = new Parameter("Interval", ValueType.NUMBER);
        String description = "Interval controls how long to wait before buffering the next value update.\n"
                + "This setting has no effect when logging type is not interval.";
        intervalParameter.setDescription(description);
        intervalParameter.setDefaultValue(new Value(interval));
        return intervalParameter;
    }

    private Parameter createLoggingTypeParameter() {
        Set<String> loggingTypeValues = LoggingType.buildEnums();
        final Parameter loggingTypeParameter = new Parameter("Logging Type", ValueType.makeEnum(loggingTypeValues));
        loggingTypeParameter.setDefaultValue(new Value(loggingType.getName()));
        loggingTypeParameter.setDescription("Logging type controls what kind of data gets stored into the database");
        return loggingTypeParameter;
    }

    private Parameter createBufferFlushTimeParameter() {
        final Parameter bufferFlushTimeParameter = new Parameter("Buffer Flush Time", ValueType.NUMBER);
        String description = "Buffer flush time controls the interval when data gets written into the database\n"
                + "Setting a time to 0 means to record data immediately";
        bufferFlushTimeParameter.setDescription(description);
        bufferFlushTimeParameter.setDefaultValue(new Value(this.bufferFlushTime));
        return bufferFlushTimeParameter;
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
        Node existingEditNode = node.getChild("edit", false);

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
        if (!LoggingType.INTERVAL.equals(loggingType)) {
            return;
        }

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

    public synchronized void addWatchUpdateToBuffer(WatchUpdate watchUpdate, Date date) {
        long withoutMs = ((date.getTime() + 500) / 1000) * 1000;
        watchUpdate.updateTimestamp(withoutMs);
        queue.add(watchUpdate);
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

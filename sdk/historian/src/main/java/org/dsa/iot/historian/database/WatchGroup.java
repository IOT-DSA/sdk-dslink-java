package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.methods.requests.SetRequest;
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
import org.dsa.iot.dslink.util.NodeUtils;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.WatchUpdate;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Samuel Grenier
 */
public class WatchGroup {
    public static final int MINIMUM_AMOUNT_OF_THREADS = 3;
    private final Permission permission;
    private final Database db;
    private final Node node;

    private final Queue<WatchUpdate> queue = new ConcurrentLinkedDeque<>();
    private final Object writeLoopLock = new Object();
    private final ScheduledExecutorService intervalScheduler;
    private ScheduledFuture<?> bufferFut;

    private LoggingType loggingType;
    private ScheduledFuture<?> scheduledIntervalWrite;

    public long getInterval() {
        return interval;
    }

    private long interval;

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
            case POINT_TIME: {
                Value vCurr = watch.getLastValue();
                Value vUpdate = sv.getValue();
                long curr = (vCurr == null) ? 0 : vCurr.getTime();
                long update = (vUpdate == null) ? 0 : vUpdate.getTime();

                if ((vCurr != null) && (curr < update)) {
                    doWrite = true;
                    watch.setLastValue(vUpdate);
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
        if (watch.isEnabled()) {
            db.getProvider().getPool().subscribe(path, watch);
        }
    }

    /**
     * Subscribes to the entire watch group.
     */
    public void subscribe() {
        Map<String, Node> children = node.getChildren();
        for (Node n : children.values()) {
            if (n.getAction() == null) {
                initWatch(n.getName().replaceAll("%2F", "/"));
            }
        }
    }

    /**
     * Unsubscribes from the entire watch group.
     */
    public void unsubscribe() {
        Map<String, Node> children = node.getChildren();

        for (Node n : children.values()) {
            if (n.getAction() == null) {
                Watch w = n.getMetaData();
                w.unsubscribe();
            }
        }
    }

    /**
     * All settings must be as actions and their default parameters should be
     * updated when changed. Be sure to call {@code super} on this method.
     */
    protected void initSettings() {
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

                        JsonObject obj = new JsonObject();
                        obj.put("@", "merge");
                        obj.put("type", "paths");

                        String p = node.getLink().getDSLink().getPath();
                        p += node.getPath() + "/";
                        p += StringUtils.encodeName(path) + "/getHistory";
                        JsonArray array = new JsonArray();
                        array.add(p);
                        obj.put("val", array);
                        v = new Value(obj);

                        DSLinkHandler h = node.getLink().getHandler();
                        DSLinkProvider pr = h.getProvider();
                        String dsId = h.getConfig().getDsIdWithHash();
                        DSLink link = pr.getRequesters().get(dsId);
                        Requester req = link.getRequester();
                        path += "/@@getHistory";
                        req.set(new SetRequest(path, v), null);
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
            b.setRoConfig("bft", new Value(5));
            // Logging type
            b.setRoConfig("lt", new Value(LoggingType.ALL_DATA.getName()));
            // Interval
            b.setRoConfig("i", new Value(5));

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
                bft.setDefaultValue(NodeUtils.getRoConfig(b, "bft"));
            }

            final Parameter lt;
            {
                Set<String> enums = LoggingType.buildEnums();
                lt = new Parameter("Logging Type", ValueType.makeEnum(enums));
                lt.setDefaultValue(NodeUtils.getRoConfig(b, "lt"));
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
                i.setDefaultValue(NodeUtils.getRoConfig(b, "i"));
            }

            EditSettingsHandler handler = new EditSettingsHandler();
            {
                handler.setBufferFlushTimeParam(bft);
                setupTimer(bft.getDefault().getNumber().intValue());

                handler.setLoggingTypeParam(lt);
                loggingType = LoggingType.toEnum(lt.getDefault().getString());

                handler.setIntervalParam(i);
                setInterval(i.getDefault().getNumber().longValue());

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
                    node.delete();
                    unsubscribe();
                }
            }));
            b.build();
        }
    }

    private void setupTimer(int time) {
        synchronized (writeLoopLock) {
            if (bufferFut != null) {
                bufferFut.cancel(false);
                bufferFut = null;
            }

            if (time <= 0) {
                return;
            }

            bufferFut = LoopProvider.getProvider().schedulePeriodic(new Runnable() {
                @Override
                public void run() {
                    handleQueue();
                }
            }, time, time, TimeUnit.SECONDS);
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

    private void setInterval(long interval) {
        this.interval = TimeUnit.SECONDS.toMillis(interval);
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
        bufferFut.cancel(true);
        queue.clear();
    }

    public void scheduleInterval(Runnable writeValues) {
        if (!canWriteOnNewData()) {
            if (scheduledIntervalWrite == null || scheduledIntervalWrite.isDone()) {
                scheduledIntervalWrite = intervalScheduler.scheduleAtFixedRate(writeValues, 0, interval, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void cancelIntervalScheduler() {
        if (LoggingType.INTERVAL.equals(loggingType)) {
            scheduledIntervalWrite.cancel(true);
            cancelBufferWrite();
        }
    }

    private class EditSettingsHandler implements Handler<ActionResult> {
        private Action action;

        private Parameter bft;
        private Parameter lt;
        private Parameter i;

        public void setAction(Action a) {
            this.action = a;
        }

        public void setBufferFlushTimeParam(Parameter bft) {
            this.bft = bft;
        }

        public void setLoggingTypeParam(Parameter lt) {
            this.lt = lt;
        }

        public void setIntervalParam(Parameter i) {
            this.i = i;
        }

        @Override
        public void handle(ActionResult event) {
            Node node = event.getNode();

            Value vLt = event.getParameter(lt.getName(), ValueType.STRING);
            Value vBft = event.getParameter(bft.getName(), bft.getType());
            if (vBft.getNumber().intValue() < 0) {
                vBft.set(0);
            }

            Value vI = event.getParameter(i.getName(), i.getType());
            if (vI.getNumber().intValue() < 0) {
                vI.set(0);
            }

            node.setRoConfig("bft", vBft);
            bft.setDefaultValue(vBft);
            setupTimer(vBft.getNumber().intValue());

            node.setRoConfig("lt", vLt);
            loggingType = LoggingType.toEnum(vLt.getString());
            lt.setDefaultValue(vLt);

            node.setRoConfig("i", vI);
            i.setDefaultValue(vI);
            setInterval(vI.getNumber().longValue());

            lt.setDefaultValue(vLt);

            List<Parameter> params = new LinkedList<>();
            params.add(bft);
            params.add(lt);
            params.add(i);
            action.setParams(params);
        }
    }
}

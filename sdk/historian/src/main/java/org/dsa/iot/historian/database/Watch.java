package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.*;
import org.dsa.iot.dslink.link.*;
import org.dsa.iot.dslink.methods.requests.*;
import org.dsa.iot.dslink.node.*;
import org.dsa.iot.dslink.node.actions.*;
import org.dsa.iot.dslink.node.value.*;
import org.dsa.iot.dslink.util.handler.*;
import org.dsa.iot.dslink.util.json.*;
import org.dsa.iot.historian.stats.*;
import org.dsa.iot.historian.utils.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * @author Samuel Grenier
 */
public class Watch {
    private static final Logger LOGGER = LoggerFactory.getLogger(Watch.class);

    private final ReentrantReadWriteLock rtLock = new ReentrantReadWriteLock();
    private final List<Handler<QueryData>> rtHandlers = new ArrayList<>();
    private final WatchGroup group;
    private final Node node;

    private Node realTimeValue;
    private String watchedPath;

    private Node lastWrittenValue;
    private Node startDate;
    private Node endDate;
    private boolean enabled;

    // Values that must be handled before the buffer queue
    private long lastWrittenTime;
    private Value lastValue;

    public WatchUpdate getLastWatchUpdate() {
        if (lastWatchUpdate == null) {
            Value value = node.getValue();
            if (value != null) {
                SubscriptionValue subscriptionValue = new SubscriptionValue(watchedPath,
                                                                            value, null,
                                                                            null, null,
                                                                            null);
                lastWatchUpdate = new WatchUpdate(this, subscriptionValue);
            }
        }
        return lastWatchUpdate;
    }

    private WatchUpdate lastWatchUpdate;

    public Watch(final WatchGroup group, Node node) {
        this.group = group;
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public WatchGroup getGroup() {
        return group;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPath() {
        return watchedPath;
    }

    public void handleLastWritten(Value value) {
        if (value == null) {
            return;
        }

        lastWrittenValue.setValue(value);
        value = new Value(value.getTimeStamp());
        if (startDate != null) {
            startDate.setValue(value);
            startDate = null;
        }

        endDate.setValue(value);
        lastWrittenTime = value.getTime();
    }

    public void setLastWrittenTime(long time) {
        lastWrittenTime = time;
    }

    public long getLastWrittenTime() {
        return lastWrittenTime;
    }

    public void setLastValue(Value value) {
        lastValue = value;
    }

    public Value getLastValue() {
        return lastValue;
    }

    public void unsubscribe() {
        group.removeFromWatches(this);
        removeFromSubscriptionPool();

        node.delete();
    }

    private void removeFromSubscriptionPool() {
        DatabaseProvider provider = group.getDb().getProvider();
        SubscriptionPool pool = provider.getPool();
        pool.unsubscribe(watchedPath, this);
    }

    public void init(Permission perm, Database db) {
        watchedPath = node.getName().replaceAll("%2F", "/").replaceAll("%2E", ".");
        initData(node);

        createUnsubscribeAction(perm);

        new OverwriteHistoryAction(this, node, perm, db);
        GetHistory.initAction(node, getGroup().getDb());

        addGetHistoryActionAlias();
        group.addWatch(this);
    }

    private void createUnsubscribeAction(Permission perm) {
        NodeBuilder b = node.createChild("unsubscribe");
        b.setSerializable(false);
        b.setDisplayName("Unsubscribe");
        b.setAction(new Action(perm, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                unsubscribe();
            }
        }));
        b.build();
    }

    public void addGetHistoryActionAlias() {
        JsonObject mergePathsObject = new JsonObject();
        mergePathsObject.put("@", "merge");
        mergePathsObject.put("type", "paths");

        String linkPath = node.getLink().getDSLink().getPath();
        String getHistoryPath = String.format("%s%s/getHistory", linkPath,
                                              node.getPath());
        JsonArray array = new JsonArray();
        array.add(getHistoryPath);
        mergePathsObject.put("val", array);
        Value mergeValue = new Value(mergePathsObject);

        Requester requester = getRequester();
        String actionAliasPath = watchedPath + "/@@getHistory";
        requester.set(new SetRequest(actionAliasPath, mergeValue), null);
    }

    private Requester getRequester() {
        DSLinkHandler handler = node.getLink().getHandler();
        DSLinkProvider linkProvider = handler.getProvider();
        String dsId = handler.getConfig().getDsIdWithHash();
        DSLink link = linkProvider.getRequesters().get(dsId);
        return link.getRequester();
    }

    protected void initData(final Node node) {
        realTimeValue = node;

        {
            NodeBuilder b = node.createChild("enabled");
            b.setDisplayName("Enabled");
            b.setWritable(Writable.CONFIG);
            b.setValueType(ValueType.BOOL);
            b.setValue(new Value(true));
            b.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public synchronized void handle(ValuePair event) {
                    enabled = event.getCurrent().getBool();
                    String path = node.getName().replaceAll("%2F", "/").replaceAll("%2E",
                                                                                   ".");
                    SubscriptionPool pool = group.getDb().getProvider().getPool();
                    if (enabled) {
                        pool.subscribe(path, Watch.this);
                    } else {
                        pool.unsubscribe(path, Watch.this);
                    }
                }
            });
            Node n = b.build();
            enabled = n.getValue().getBool();
        }

        try {
            NodeBuilder b = node.createChild("startDate");
            b.setDisplayName("Start Date");
            b.setValueType(ValueType.TIME);
            Node n = b.build();
            if (n.getValue() == null) {
                startDate = n;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to set start date", e);
        }

        {
            NodeBuilder b = node.createChild("endDate");
            b.setDisplayName("End Date");
            b.setValueType(ValueType.TIME);
            endDate = b.build();
        }

        {
            NodeBuilder b = node.createChild("lwv");
            b.setDisplayName("Last Written Value");
            b.setValueType(ValueType.DYNAMIC);
            lastWrittenValue = b.build();
        }
    }

    /**
     * Called when the watch receives data.
     *
     * @param sv Received data.
     */
    public void onData(SubscriptionValue sv) {
        sv = tryConvert(sv);
        realTimeValue.setValue(sv.getValue());
        if (group.canWriteOnNewData()) {
            group.write(this, sv);
        } else {
            lastWatchUpdate = new WatchUpdate(this, sv);
        }
    }

    public void addHandler(Handler<QueryData> handler) {
        if (handler == null) {
            return;
        }
        rtLock.writeLock().lock();
        try {
            rtHandlers.add(handler);
        } finally {
            rtLock.writeLock().unlock();
        }
    }

    public void removeHandler(Handler<QueryData> handler) {
        rtLock.writeLock().lock();
        try {
            rtHandlers.remove(handler);
        } finally {
            rtLock.writeLock().unlock();
        }
    }

    public void notifyHandlers(QueryData data) {
        rtLock.readLock().lock();
        try {
            for (Handler<QueryData> h : rtHandlers) {
                h.handle(data);
            }
        } finally {
            rtLock.readLock().unlock();
        }
    }

    /**
     * Attempts to convert the value of the argument to the value type of the
     * realTimeValue node.
     *
     * @param arg The candidate for conversion.
     * @return A new SubscriptionValue if the value in the argument was immutable.
     */
    private SubscriptionValue tryConvert(SubscriptionValue arg) {
        Value value = arg.getValue();
        ValueType toType = realTimeValue.getValueType();
        if (value.getType() == toType) {
            return arg;
        }
        if (value.isImmutable()) {
            value = ValueUtils.mutableCopy(value);
            arg = new SubscriptionValue(arg.getPath(), value, arg.getCount(),
                                        arg.getSum(), arg.getMin(), arg.getMax());
        }
        if (toType == ValueType.BOOL) {
            toBoolean(value);
        } else if (toType == ValueType.NUMBER) {
            toNumber(value);
        } else if (toType == ValueType.STRING) {
            toString(value);
        }
        return arg;
    }

    /**
     * Converts the value to a boolean if possible, otherwise does nothing.
     *
     * @param value A mutable value to convert in place.
     */
    private void toBoolean(Value value) {
        ValueType type = value.getType();
        if (type == ValueType.STRING) {
            String s = value.getString();
            if ("true".equalsIgnoreCase(s)) {
                value.set(Boolean.TRUE);
            } else if ("false".equalsIgnoreCase(s)) {
                value.set(Boolean.FALSE);
            } else if ("0".equals(s)) {
                value.set(Boolean.FALSE);
            } else if ("1".equals(s)) {
                value.set(Boolean.TRUE);
            } else {
                //Test if it's a number other than "0" or "1".
                try {
                    double d = Double.parseDouble(s);
                    value.set(d != 0d);
                } catch (Exception ignore) {
                }
            }
        } else if (type == ValueType.NUMBER) {
            Number num = value.getNumber();
            if (num instanceof Double) {
                value.set(num.doubleValue() != 0d);
            } else if (num instanceof Float) {
                value.set(num.floatValue() != 0f);
            } else {
                value.set(num.longValue() != 0l);
            }
        }
    }

    /**
     * Converts the value to a number if possible, otherwise does nothing.
     *
     * @param value A mutable value to convert in place.
     */
    private void toNumber(Value value) {
        ValueType type = value.getType();
        if (type == ValueType.STRING) {
            try {
                String s = value.getString();
                if (s.indexOf('.') >= 0) {
                    value.set(Double.parseDouble(s));
                } else {
                    value.set(Long.parseLong(s));
                }
            } catch (Exception ignore) {
            }
        } else if (type == ValueType.BOOL) {
            if (value.getBool()) {
                value.set(1);
            } else {
                value.set(0);
            }
        }
    }

    /**
     * Converts the value to a string if necessary.
     *
     * @param value A mutable value to convert in place.
     */
    private void toString(Value value) {
        if (value.getType() == ValueType.STRING) {
            return;
        }
        value.set(value.toString());
    }

}

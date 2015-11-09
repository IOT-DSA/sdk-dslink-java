package org.dsa.iot.broker.utils;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Metrics {

    private final BrokerNode<?> messagesInNode;
    private final BrokerNode<?> messagesOutNode;

    private AtomicInteger messagesIn;
    private AtomicInteger messagesOut;
    private ScheduledFuture<?> future;

    public Metrics(BrokerNode msgIn, BrokerNode msgOut) {
        this.messagesInNode = msgIn;
        this.messagesOutNode = msgOut;
    }

    public void incrementIn() {
        AtomicInteger in = this.messagesIn;
        if (in != null) {
            in.incrementAndGet();
        }
    }

    public void incrementOut() {
        AtomicInteger out = this.messagesOut;
        if (out != null) {
            out.incrementAndGet();
        }
    }

    public synchronized void start() {
        stop();
        messagesIn = new AtomicInteger();
        messagesOut = new AtomicInteger();
        future = Objects.getDaemonThreadPool().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                AtomicInteger in = messagesIn;
                AtomicInteger out = messagesOut;
                if (in != null && out != null) {
                    Value i = new Value(in.getAndSet(0));
                    Value o = new Value(out.getAndSet(0));
                    messagesInNode.setValue(i);
                    messagesOutNode.setValue(o);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
            messagesIn = null;
            messagesOut = null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Metrics create(Broker broker) {
        BrokerNode node = broker.tree().getRoot();
        BrokerNode sys = new BrokerNode(node, "sys");
        node.addChild(sys);

        BrokerNode msgIn = new BrokerNode(sys, "messagesInPerSecond");
        msgIn.setValueType(ValueType.NUMBER);
        msgIn.setValue(new Value(0));
        sys.addChild(msgIn);

        BrokerNode msgOut = new BrokerNode(sys, "messagesOutPerSecond");
        msgOut.setValueType(ValueType.NUMBER);
        msgOut.setValue(new Value(0));
        sys.addChild(msgOut);

        return new Metrics(msgIn, msgOut);
    }
}

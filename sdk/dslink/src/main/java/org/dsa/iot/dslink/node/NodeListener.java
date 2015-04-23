package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.Handler;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles listening to node updates.
 *
 * @author Samuel Grenier
 */
public class NodeListener {

    private final Node node;

    private final Set<Handler<Value>> valueHandlers;
    private final Set<Handler<ValueUpdate>> configHandlers;
    private final Set<Handler<ValueUpdate>> attribHandlers;

    private final Set<Handler<Node>> listHandlers;
    private final Set<Handler<Node>> onSubscribedHandlers;
    private final Set<Handler<Node>> onUnsubscribedHandlers;

    public NodeListener(Node node) {
        this.node = node;

        valueHandlers = new HashSet<>();
        configHandlers = new HashSet<>();
        attribHandlers = new HashSet<>();

        listHandlers = new HashSet<>();
        onSubscribedHandlers = new HashSet<>();
        onUnsubscribedHandlers = new HashSet<>();
    }

    /**
     * Handles when a node updates its value. The value can be {@code null} if
     * the value on the node was removed.
     *
     * @param handler Callback.
     */
    public void addValueHandler(Handler<Value> handler) {
        checkHandler(handler);
        valueHandlers.add(handler);
    }

    /**
     * Posts a value update calling all the value handler callbacks.
     *
     * @param value Updated value.
     */
    protected void postValueUpdate(Value value) {
        for (Handler<Value> handler : valueHandlers) {
            handler.handle(value);
        }
    }

    /**
     * Handles when a node updates its configuration value.
     *
     * @param handler Callback.
     */
    public void addConfigHandler(Handler<ValueUpdate> handler) {
        checkHandler(handler);
        configHandlers.add(handler);
    }

    /**
     * Posts a value update calling all the handler callbacks
     * when a configuration value has been updated or removed.
     *
     * @param update Update to post.
     */
    protected void postConfigUpdate(ValueUpdate update) {
        for (Handler<ValueUpdate> handler : configHandlers) {
            handler.handle(update);
        }
    }

    /**
     * Handles when a node updates its attribute value.
     *
     * @param handler Callback.
     */
    public void addAttributeHandler(Handler<ValueUpdate> handler) {
        checkHandler(handler);
        attribHandlers.add(handler);
    }

    /**
     * Posts a value update calling all the handler callbacks
     * when an attribute value has been updated or removed.
     *
     * @param update Update to post.
     */
    protected void postAttributeUpdate(ValueUpdate update) {
        for (Handler<ValueUpdate> handler : attribHandlers) {
            handler.handle(update);
        }
    }

    /**
     * Adds a list handler listener. The handler will be called every time a
     * remote endpoint performs a list request on the node.
     *
     * @param handler Callback.
     */
    public void addOnListHandler(Handler<Node> handler) {
        checkHandler(handler);
        listHandlers.add(handler);
    }

    /**
     * Posts an update that the node is currently being listed.
     */
    public void postListUpdate() {
        for (Handler<Node> handler : listHandlers) {
            handler.handle(node);
        }
    }

    /**
     * Adds a subscription handler for the node to take action when
     * a node's value has been subscribed to.
     *
     * @param handler Callback.
     */
    public void addOnSubscribeHandler(Handler<Node> handler) {
        checkHandler(handler);
        onSubscribedHandlers.add(handler);
    }

    /**
     * Posts a subscription update. The node's value has been subscribed
     * to.
     */
    protected void postOnSubscription() {
        for (Handler<Node> handler : onSubscribedHandlers) {
            handler.handle(node);
        }
    }

    /**
     * Adds an unsubscription handler for the node to take action when
     * a node's value is unsubscribed.
     *
     * @param handler Callback.
     */
    public void addOnUnsubcriptionHandler(Handler<Node> handler) {
        checkHandler(handler);
        onUnsubscribedHandlers.add(handler);
    }

    /**
     * Posts an unsubscription update. The node's value has been unsubscribed
     * to.
     */
    protected void postOnUnsubscription() {
        for (Handler<Node> handler : onUnsubscribedHandlers) {
            handler.handle(node);
        }
    }

    private void checkHandler(Handler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
    }

    public static class ValueUpdate {

        private final String name;
        private final Value value;
        private final boolean removed;

        public ValueUpdate(String name, Value value, boolean removed) {
            this.name = name;
            this.value = value;
            this.removed = removed;
        }

        /**
         * The name will be {@code null} if the value changed was on a node
         * rather than a configuration or attribute.
         *
         * @return The name of the value that was updated.
         */
        public String name() {
            return name;
        }

        /**
         * If the node metadata was removed, then value will be the previously
         * set value.
         *
         * @return Updated or removed value. Value is never {@code null}.
         */
        public Value value() {
            return value;
        }

        /**
         * @return Whether the value was removed or not.
         */
        public boolean removed() {
            return removed;
        }
    }
}

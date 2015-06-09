package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.vertx.java.core.Handler;

/**
 * Handles listening to node updates.
 *
 * @author Samuel Grenier
 */
public class NodeListener {

    private final Node node;

    private Handler<ValuePair> valueHandler;
    private Handler<ValueUpdate> configHandler;
    private Handler<ValueUpdate> attribHandler;

    private Handler<Node> listHandler;
    private Handler<Node> onSubscribedHandler;
    private Handler<Node> onUnsubscribedHandler;

    public NodeListener(Node node) {
        this.node = node;
    }

    /**
     * Handles when a node updates its value. The value can be {@code null} if
     * the value on the node was removed.
     *
     * @param handler Callback.
     */
    public void setValueHandler(Handler<ValuePair> handler) {
        valueHandler = handler;
    }

    /**
     * Posts a value update calling all the value handler callbacks.
     *
     * @param previous Old value.
     * @param current Updated value.
     * @return Whether the new value was rejected or not.
     */
    protected boolean postValueUpdate(Value previous, Value current) {
        Handler<ValuePair> handler = valueHandler;
        if (handler != null) {
            ValuePair pair = new ValuePair(previous, current);
            handler.handle(pair);
            return pair.isRejected();
        }
        return false;
    }

    /**
     * Handles when a node updates its configuration value.
     *
     * @param handler Callback.
     */
    public void setConfigHandler(Handler<ValueUpdate> handler) {
        configHandler = handler;
    }

    /**
     * Posts a value update calling all the handler callbacks
     * when a configuration value has been updated or removed.
     *
     * @param update Update to post.
     */
    protected void postConfigUpdate(ValueUpdate update) {
        Handler<ValueUpdate> handler = configHandler;
        if (handler != null) {
            handler.handle(update);
        }
    }

    /**
     * Handles when a node updates its attribute value.
     *
     * @param handler Callback.
     */
    public void setAttributeHandler(Handler<ValueUpdate> handler) {
        attribHandler = handler;
    }

    /**
     * Posts a value update calling all the handler callbacks
     * when an attribute value has been updated or removed.
     *
     * @param update Update to post.
     */
    protected void postAttributeUpdate(ValueUpdate update) {
        Handler<ValueUpdate> handler = attribHandler;
        if (handler != null) {
            handler.handle(update);
        }
    }

    /**
     * Sets a list handler listener. The handler will be called every time a
     * remote endpoint performs a list request on the node.
     *
     * @param handler Callback.
     */
    public void setOnListHandler(Handler<Node> handler) {
        listHandler = handler;
    }

    /**
     * Posts an update that the node is currently being listed.
     */
    public void postListUpdate() {
        Handler<Node> handler = listHandler;
        if (handler != null) {
            handler.handle(node);
        }
    }

    /**
     * Sets a subscription handler for the node to take action when
     * a node's value has been subscribed to.
     *
     * @param handler Callback.
     */
    public void setOnSubscribeHandler(Handler<Node> handler) {
        onSubscribedHandler = handler;
    }

    /**
     * Posts a subscription update. The node's value has been subscribed
     * to.
     */
    protected void postOnSubscription() {
        Handler<Node> handler = onSubscribedHandler;
        if (handler != null) {
            handler.handle(node);
        }
    }

    /**
     * Sets an unsubscription handler for the node to take action when
     * a node's value is unsubscribed.
     *
     * @param handler Callback.
     */
    public void setOnUnsubscribeHandler(Handler<Node> handler) {
        onUnsubscribedHandler = handler;
    }

    /**
     * Posts an unsubscription update. The node's value has been unsubscribed
     * to.
     */
    protected void postOnUnsubscription() {
        Handler<Node> handler = onUnsubscribedHandler;
        if (handler != null) {
            handler.handle(node);
        }
    }

    /**
     * Removes all handlers
     */
    public void kill() {
        setValueHandler(null);
        setOnSubscribeHandler(null);
        setOnUnsubscribeHandler(null);
        setOnListHandler(null);
        setAttributeHandler(null);
        setConfigHandler(null);
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

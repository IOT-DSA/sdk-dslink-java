package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.util.handler.Handler;

import java.lang.ref.WeakReference;

/**
 * Handles listening to node updates.
 *
 * @author Samuel Grenier
 */
public class NodeListener {

    private WeakReference<Node> node;

    private Handler<ValuePair> valueHandler;
    private Handler<ValueUpdate> configHandler;
    private Handler<ValueUpdate> attribHandler;

    private Handler<Node> listHandler;
    private Handler<Node> listClosedHandler;
    private Handler<Node> nodeRemovedHandler;

    private Handler<Node> onSubscribedHandler;
    private Handler<Node> onUnsubscribedHandler;

    public NodeListener(Node node) {
        this.node = new WeakReference<>(node);
    }
    
    public void setNode(Node node) {
    	this.node = new WeakReference<Node>(node);
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
     * @param pair The value pair of the previous and new values.
     * @return Whether the new value was rejected or not.
     */
    protected boolean postValueUpdate(ValuePair pair) {
        Handler<ValuePair> handler = valueHandler;
        if (handler != null) {
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
     * Sets a list stream closed handler listener. The handler will be called
     * every time the list stream is closed.
     *
     * @param handler Callback.
     */
    @SuppressWarnings("unused")
    public void setOnListClosedHandler(Handler<Node> handler) {
        listClosedHandler = handler;
    }

    /**
     * Sets a node removal handler listener. The handler will be called
     * every time the node is removed.
     *
     * @param handler Callback.
     */
    @SuppressWarnings("unused")
    public void setNodeRemovedHandler(Handler<Node> handler) {
        nodeRemovedHandler = handler;
    }

    /**
     * Posts an update that the node is currently being listed.
     */
    public void postListUpdate() {
        Handler<Node> handler = listHandler;
        if (handler != null) {
            Node node = this.node.get();
            if (node != null) {
                handler.handle(node);
            }
        }
    }

    /**
     * Posts a list closed update. The responder no longer wants the list
     * stream to remain open.
     */
    public void postListClosed() {
        Handler<Node> handler = listClosedHandler;
        if (handler != null) {
            Node node = this.node.get();
            if (node != null) {
                handler.handle(node);
            }
        }
    }

    /**
     * Posts a node removed update. This happens when the node is removed.
     */
    public void postNodeRemoved() {
        Handler<Node> handler = nodeRemovedHandler;
        if (handler != null) {
            Node node = this.node.get();
            if (node != null) {
                handler.handle(node);
            }
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
            Node node = this.node.get();
            if (node != null) {
                handler.handle(node);
            }
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
            Node node = this.node.get();
            if (node != null) {
                handler.handle(node);
            }
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
        setNodeRemovedHandler(null);
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
        @SuppressWarnings("unused")
        public boolean removed() {
            return removed;
        }
    }
}

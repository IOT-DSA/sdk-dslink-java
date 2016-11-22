package org.dsa.iot.dslink;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.json.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Top level API for handling the configuration of nodes and responses to
 * requests. Note that {@link #onRequesterInitialized} and
 * {@link #onResponderInitialized} can be each called for the same link ID.
 * This allows for the node managers to be completely isolated between a
 * a requester and responder.
 *
 * @author Samuel Grenier
 */
@SuppressWarnings("UnusedParameters")
public abstract class DSLinkHandler {

    private Configuration configuration;
    private DSLinkProvider provider;
    private File workingDir = new File("").getAbsoluteFile();

    /**
     * The default setting from here is what is used in the
     * {@link Configuration} instance during an auto configuration. The result
     * must be strictly static.
     *
     * @return The extra link data that can be used by any requester.
     */
    public JsonObject getLinkData() {
        return null;
    }

    /**
     * The default setting from here is what is used in the
     * {@link Configuration} instance during an auto configuration. The result
     * must be strictly static.
     *
     * @return Whether this DSLink is a responder or not.
     */
    public boolean isResponder() {
        return false;
    }

    /**
     * The default setting from here is what is used in the
     * {@link Configuration} instance during an auto configuration. The result
     * must be strictly static.
     *
     * @return Whether this DSLink is a requester or not.
     */
    public boolean isRequester() {
        return false;
    }

    /**
     * Stops the entire DSLink. The DSLink is expected to close all resources
     * and stop all threads. This method is called when {@link DSLinkProvider}
     * is stopped. SDK resources such as {@link Objects} should not be shut
     * down here. However, all locally created thread pools must be shut down.
     * Any scheduled tasks created in any thread pool must be shut down.
     */
    public void stop() {
    }

    /**
     * Sets the configuration of the handler.
     *
     * @param configuration Configuration of the link
     */
    public void setConfig(Configuration configuration) {
        if (configuration == null) {
            throw new NullPointerException("configuration");
        }
        this.configuration = configuration;
    }

    /**
     * @return Configuration of the DSLink
     */
    public Configuration getConfig() {
        return configuration;
    }

    /**
     * Nodes in links can specify an icon with the @icon attribute.  The default
     * implementation of this method looks for the icon in the icons
     * directory in the root of the jar containing the DSLinkHandler subclass:
     * getClass().getResourceAsStream("/icons/" + name).
     *
     * @param name Name or path of the desired icon.
     * @return The bytes representing the icon image.
     */
    @SuppressFBWarnings("DE_MIGHT_IGNORE")
    public byte[] getIcon(String name) {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            StringBuilder builder = new StringBuilder("/icons");
            if (name.charAt(0) != '/') {
                builder.append('/');
            }
            builder.append(name);
            in = getClass().getResourceAsStream(builder.toString());
            out = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[512];
                int len = in.read(buf);
                while (len > 0) {
                    out.write(buf, 0, len);
                    len = in.read(buf);
                }
                out.flush();
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
            return out.toByteArray();
        } finally {
            try {
                in.close();
                out.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Sets the provider that is attached to this handler.
     *
     * @param provider DSLink provider.
     */

    public void setProvider(DSLinkProvider provider) {
        this.provider = provider;
    }

    /**
     * @return DSLink provider attached to this DSLink.
     */
    public DSLinkProvider getProvider() {
        return provider;
    }

    /**
     * Sets the new working directory the DSLink should work in.
     * This will typical be used by the runtime container.
     *
     * @param workingDir The new working directory of the DSLink.
     */
    @SuppressWarnings("unused")
    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * @return The working directory of the DSLink.
     */
    public File getWorkingDir() {
        return workingDir;
    }

    /**
     * Pre initializes the handler. If this link is a responder any actions
     * must be populated here.
     */
    public void preInit() {
    }

    /**
     * This method is asynchronously called. The link is not yet connected
     * to the server.
     *
     * @param link The link that needs to be initialized.
     */
    public void onRequesterInitialized(DSLink link) {
    }

    /**
     * This method is asynchronously called. The link is connected to the
     * server at this stage.
     *
     * @param link The link that has completed a connection.
     */
    public void onRequesterConnected(DSLink link) {
    }

    /**
     * The link has lost a connection from the server.
     *
     * @param link The link that has been disconnected.
     */
    public void onRequesterDisconnected(DSLink link) {
    }

    /**
     * This method is asynchronously called. The link is not yet connected
     * to the server.
     *
     * @param link The link that needs to be initialized.
     */
    public void onResponderInitialized(DSLink link) {
    }

    /**
     * This method is asynchronously called. The link is connected to the
     * server at this stage.
     *
     * @param link The link that has completed a connection.
     */
    public void onResponderConnected(DSLink link) {
    }

    /**
     * The link has lost a connection from the server.
     *
     * @param link The link that has been disconnected.
     */
    public void onResponderDisconnected(DSLink link) {
    }

    /**
     * Callback when a subscription fails as a result of trying to
     * subscribing to a non-existent node.
     *
     * @param path Path the requester wants to subscribe to. The path is
     *             normalized without a leading forward-slash.
     * @return A created node to force a subscription to succeed or
     * {@code null} to let the subscription fail.
     */
    public Node onSubscriptionFail(String path) {
        return null;
    }

    /**
     * Callback when an invocation fails as a result of trying to
     * invoke a non-existent node.
     *
     * @param path Path the requester is trying to invoke. The path is
     *             normalized without a leading forward-flash.
     * @return A created node with an action set on it to force an
     * invocation.
     */
    public Node onInvocationFail(String path) {
        return null;
    }

    /**
     * Callback when a set fails as a result of trying to set
     * on a non-existent node. If the node
     *
     * @param path  Path the requester wants to set.
     * @param value Value the requester wants the path to have.
     */
    public void onSetFail(String path, Value value) {
        throw new NoSuchPathException(path);
    }

    /**
     * Called to create a DSLink instance for a responder.
     *
     * @param path Responder path.
     * @return DSLink instance.
     */
    public DSLink createResponderLink(String path) {
        return new DSLink(this, false, path);
    }

    /**
     * Called to create a DSLink instance for a requester.
     *
     * @param path Requester path.
     * @return DSLink instance.
     */
    public DSLink createRequesterLink(String path) {
        return new DSLink(this, true, path);
    }

    /**
     * Called to create a node manager instance for a requester.
     *
     * @param requester Requester instance.
     * @param profile   Default profile name.
     * @return Node manager.
     */
    public NodeManager createRequesterNodeManager(Requester requester, String profile) {
        return new NodeManager(requester, profile);
    }

    /**
     * Called to create a node manager instance for a responder.
     *
     * @param responder Responder instance.
     * @param profile   Default profile name.
     * @return Node manager.
     */
    public NodeManager createResponderNodeManager(Responder responder, String profile) {
        return new NodeManager(responder, profile);
    }
}

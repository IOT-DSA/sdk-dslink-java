package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.SubscriptionManager;

import java.lang.ref.WeakReference;

/**
 * @author Samuel Grenier
 */
public abstract class Linkable {

    private final DSLinkHandler handler;
    private WeakReference<DSLink> link;

    /**
     * @param handler Link handler
     */
    public Linkable(DSLinkHandler handler) {
        this.handler = handler;
    }

    /**
     * @return The subscription manager of the link
     */
    public SubscriptionManager getSubscriptionManager() {
        DSLink dsLink = getDSLink();
        if (dsLink != null) {
            return dsLink.getSubscriptionManager();
        }
        return null;
    }

    /**
     * @return Handler of the link
     */
    public DSLinkHandler getHandler() {
        return handler;
    }

    /**
     * The DSLink object is used for the client and node manager.
     * @param link The link to set.
     */
    public void setDSLink(DSLink link) {
        if (link == null)
            throw new NullPointerException("link");
        this.link = new WeakReference<>(link);
    }

    /**
     * @return A reference to the dslink, can be null
     */
    public DSLink getDSLink() {
        return link.get();
    }
}

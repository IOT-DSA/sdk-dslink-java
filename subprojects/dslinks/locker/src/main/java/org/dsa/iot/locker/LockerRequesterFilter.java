package org.dsa.iot.locker;

import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.subscription.SubscriptionContext;

import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.requester.responses.InvokeResponse;
import org.dsa.iot.dslink.requester.responses.ListResponse;
import org.dsa.iot.dslink.requester.responses.SubscriptionResponse;

/**
 * Filter for @Handler annotation in Main
 * 
 * @author pshvets
 *
 */
public class LockerRequesterFilter {

    public final static class SubscriptionResponseFilter implements
            IMessageFilter<ResponseEvent> {
        @Override
        public boolean accepts(ResponseEvent message,
                SubscriptionContext context) {
            return message.getResponse() instanceof SubscriptionResponse;
        }
    }

    public final static class ListResponseFilter implements
            IMessageFilter<ResponseEvent> {
        @Override
        public boolean accepts(ResponseEvent message,
                SubscriptionContext context) {
            return message.getResponse() instanceof ListResponse;
        }
    }

    public final static class InvokeResponseFilter implements
            IMessageFilter<ResponseEvent> {
        @Override
        public boolean accepts(ResponseEvent message,
                SubscriptionContext context) {
            return message.getResponse() instanceof InvokeResponse;
        }
    }
}

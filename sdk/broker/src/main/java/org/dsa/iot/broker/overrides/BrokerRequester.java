package org.dsa.iot.broker.overrides;

import lombok.val;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.broker.events.ListResponseEvent;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.requests.Request;
import org.dsa.iot.dslink.requester.responses.Response;

/**
 * @author Samuel Grenier
 */
public class BrokerRequester extends Requester {
    
    public BrokerRequester(MBassador<Event> bus) {
        super(bus);
    }
    
    @Override
    public Response<?> getResponse(int gid, Request req) {
        if ("list".equals(req.getName())) {
            val list = (ListRequest) req;
            val man = getManager();
            val event = new ListResponseEvent(gid);
            getBus().publish(event);
            return new BrokerListResponse(list, man, event.getPrefix());
        }
        return super.getResponse(gid, req);
    }
}

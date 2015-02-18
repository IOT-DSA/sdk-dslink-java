package org.dsa.iot.dslink;

import com.google.common.eventbus.EventBus;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.methods.*;
import org.dsa.iot.dslink.util.*;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.dsa.iot.dslink.node.NodeManager.NodeStringTuple;

/**
 * @author Samuel Grenier
 */
public class Responder extends Linkable {

    public Responder(@NonNull EventBus bus) {
        super(bus);
    }

    /**
     * @param requests The requests the other endpoint wants
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized void parse(Client client, JsonArray requests) {
        val it = requests.iterator();
        val responses = new JsonArray();
        for (JsonObject o; it.hasNext();) {
            o = (JsonObject) it.next();

            val rid = o.getNumber("rid");
            val sMethod = o.getString("method");
            val path = o.getString("path");

            val resp = new JsonObject();
            try {
                resp.putNumber("rid", rid);
                NodeStringTuple node = null;
                if (path != null) {
                    node = getManager().getNode(path);
                }

                val method = getMethod(client, sMethod, rid.intValue(), node);
                val updates = method.invoke(o);
                val state = method.getState();

                if (state == null) {
                    throw new IllegalStateException("state");
                }
                if (state != StreamState.INITIALIZED) {
                    // This is a first response, default value is initialized if omitted
                    resp.putString("stream", state.jsonName);
                }
                if (state == StreamState.OPEN
                        || state == StreamState.INITIALIZED) {
                    client.getResponseTracker().track(rid.intValue());
                }
                if (updates != null && updates.size() > 0) {
                    resp.putElement("updates", updates);
                }
            } catch (Exception e) {
                handleInvocationError(resp, e);
            } finally {
                responses.addElement(resp);
            }
        }

        val top = new JsonObject();
        top.putElement("responses", responses);
        client.write(top);
    }

    public void closeStream(Client client, int rid) {
        if (client.getResponseTracker().isTracking(rid)) {
            client.getResponseTracker().untrack(rid);

            val array = new JsonArray();
            {
                val obj = new JsonObject();
                obj.putNumber("rid", rid);
                obj.putString("stream", StreamState.CLOSED.jsonName);
                array.add(obj);
            }

            val resp = new JsonObject();
            resp.putArray("responses", array);
            client.write(resp);
        }
    }

    protected Method getMethod(@NonNull Client client,
                                @NonNull String name,
                                int rid,
                                NodeStringTuple tuple) {
        switch (name) {
            case "list":
                return new ListMethod(this, client, tuple.getNode(), rid);
            case "set":
                return new SetMethod(tuple.getNode(), tuple.getString());
            case "remove":
                return new RemoveMethod(tuple.getNode(), tuple.getString());
            case "invoke":
                return new InvokeMethod(tuple.getNode());
            case "subscribe":
                return new SubscribeMethod(getManager());
            case "unsubscribe":
                return new UnsubscribeMethod(getManager());
            case "close":
                return new CloseMethod(getBus(),
                                        client,
                                        client.getResponseTracker(),
                                        this, rid);
            default:
                throw new RuntimeException("Unknown method");
        }
    }

    protected void handleInvocationError(JsonObject resp, Exception e) {
        e.printStackTrace(System.err);
        resp.putString("stream", StreamState.CLOSED.jsonName);

        val error = new JsonObject();
        error.putString("msg", e.getMessage());

        val writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        error.putString("detail", writer.toString());

        resp.putElement("error", error);
    }
}

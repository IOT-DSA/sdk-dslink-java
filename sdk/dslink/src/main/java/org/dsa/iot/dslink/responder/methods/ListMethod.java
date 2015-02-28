package org.dsa.iot.dslink.responder.methods;

import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.responder.Responder;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class ListMethod extends Method {

	@NonNull
	private final Responder responder;
	@NonNull
	private final Client client;
	@NonNull
	private final Node parent;
	private final int rid;

	public ListMethod(Responder responder, Client client, Node parent, int rid,
			JsonObject request) {
		super(request);
		this.responder = responder;
		this.client = client;
		this.parent = parent;
		this.rid = rid;
	}

	@Override
	public JsonArray invoke() {
		setState(StreamState.OPEN);
		JsonArray resp = getResponse();
		parent.subscribeToChildren(client, responder, rid);
		return resp;
	}

	private JsonArray getResponse() {
		JsonArray array = new JsonArray();
		writeParentData(array, "$", parent.getConfigurations());
		writeParentData(array, "@", parent.getAttributes());

		Map<String, Node> children = parent.getChildren();
		if (children != null) {
			for (Node node : children.values()) {
				if (!(parent.getPath().equals("/conns")
                        && node.getName().equals(client.getDsId()))) {
                    array.addArray(getChildUpdate(node, false));
                }
			}
		}
		return array;
	}

	private void writeParentData(JsonArray out, String prefix,
			Map<String, Value> data) {
		if (data != null) {
			for (Map.Entry<String, Value> entry : data.entrySet()) {
				JsonArray valArray = new JsonArray();
				valArray.addString(prefix + entry.getKey());

				Value value = entry.getValue();
				ValueUtils.toJson(valArray, value);
				out.addElement(valArray);
			}
		}
	}

	public static JsonArray getChildUpdate(Node node, boolean removed) {
		val array = new JsonArray();
		array.addString(node.getName());

		JsonObject obj = new JsonObject();
		{ // API information
			iterateAndAdd(obj, "$", node.getConfigurations());
			iterateAndAdd(obj, "@", node.getAttributes());
		}

		{ // Action information
			val action = node.getAction();
			if (action != null) {
				action.toJson(obj);
			}
		}

		{ // Internal information
			obj.putString("$is", node.getConfiguration("is").getString());

			String name = node.getDisplayName();
			if (name != null) {
				obj.putString("$name", name);
			}

			List<String> interfaces = node.getInterfaces();
			StringBuilder builder = new StringBuilder();
			if (interfaces != null) {
				for (String i : interfaces) {
					builder.append(i);
					builder.append("|");
				}
				String built = builder.substring(0, builder.length() - 1);
				obj.putString("$interface", built);
			}

			if (removed) {
				obj.putString("$change", "remove");
			}

			array.addObject(obj);
		}
		return array;
	}

	private static void iterateAndAdd(@NonNull JsonObject obj,
			@NonNull String prefix, Map<String, Value> valueMap) {
		if (valueMap != null) {
			for (Map.Entry<String, Value> entry : valueMap.entrySet()) {
				val name = entry.getKey();
				val value = entry.getValue();
				ValueUtils.toJson(obj, prefix + name, value);
			}
		}
	}
}

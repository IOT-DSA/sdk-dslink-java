package org.dsa.iot.responder;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.NodeManager.NodeStringTuple;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.responder.action.Action;
import org.dsa.iot.dslink.responder.action.Parameter;
import org.dsa.iot.dslink.util.Permission;
import org.vertx.java.core.json.JsonObject;

/**
 * @author pshvets
 *
 */
public class RandomNumberGeneratorResponder {

	private static AtomicInteger counter = new AtomicInteger(0);
	private static final Random random = new Random();

	public static void main(String[] args) {
		// Create executor
		final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(
				1, new ThreadFactory() {
					@Override
					public Thread newThread(Runnable runnable) {
						Thread thread = new Thread(runnable);
						thread.setDaemon(true);
						return thread;
					}
				});

		// Create bus
		MBassador<Event> bus = EventBusFactory.create();
		RandomNumberGeneratorResponder generator = new RandomNumberGeneratorResponder();
		bus.subscribe(generator);

		// DSLink creation
		String url = "http://localhost:8080/conn";
		DSLinkFactory factory = DSLinkFactory.create();
		DSLink link = factory
				.generate(bus, url, ConnectionType.WS, "responder");

		// Create parent Node
		NodeManager manager = link.getNodeManager();
		final Node parent = manager.createRootNode("test");

		// Create Action for parent Node
		Node actionNode = parent.createChild("generate");
		// Handler when "invoke" is called
		Action action = new Action(Permission.READ,
				new org.vertx.java.core.Handler<JsonObject>() {

					@Override
					public void handle(JsonObject event) {
						if (event == null) {
							return;
						}
						// to measure execution time
						long startTime = System.currentTimeMillis();

						JsonObject params = event.getValue("params");
						Integer numberOfNodes = params.getInteger("numbers");
						Integer time = params.getInteger("time");

						for (int i = 0; i < numberOfNodes; i++) {
							Node a = parent.createChild("random_generated_"
									+ counter.incrementAndGet());
							a.setConfiguration("type", new Value(
									ValueType.NUMBER.toJsonString()));
						}
						System.out.println(numberOfNodes
								+ " nodes are generated");
						if (time != null && time > 0) {
							startPool(pool, parent, time, TimeUnit.MILLISECONDS);
							System.out.println("timer changed to " + time);
						}

						long stopTime = System.currentTimeMillis();
						long elapsedTime = stopTime - startTime;
						System.out.println("Creation time " + elapsedTime);
					}
				});
		action.addParameter(new Parameter("numbers", ValueType.NUMBER, null));
		action.addParameter(new Parameter("time", ValueType.NUMBER, null));
		actionNode.setAction(action);

		// Create variable
		NodeStringTuple tuple = manager.getNode("test/random_generated_"
				+ counter, true);
		Node node = tuple.getNode();
		node.setConfiguration("type",
				new Value(ValueType.NUMBER.toJsonString()));

		startPool(pool, parent, 300, TimeUnit.MILLISECONDS);

		link.connect();
		link.sleep();
	}

	private static void startPool(ScheduledThreadPoolExecutor pool,
			final Node parent, int time, TimeUnit timeUnit) {
		pool.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {

				Map<String, Node> children = parent.getChildren();
				if (children != null) {
					for (Node node : children.values()) {
						Value value = node.getValue();
						if (value == null) {
							value = new Value(0);
						} else {
							value = new Value(random.nextInt());
						}
						node.setValue(value);
					}
				}

			}
		}, 0, time, timeUnit);
	}

	@Handler
	public void onConnected(ConnectedToServerEvent event) {
		System.out.println("RandomNumberGenerator is started");
	}
}

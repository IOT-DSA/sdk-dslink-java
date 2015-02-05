package org.dsa.iot.demo;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final EventBus master = new EventBus();
    private boolean running = true;
    private DSLink link;


    public static void main(String[] args) {
        Main m = new Main();
        m.main();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    private void main() {
        System.out.println("Initializing...");
        master.register(new Main());

        final String url = "http://localhost:8080/conn";
        final String endpoint = "ws://localhost:8080";

        DSLink.generate(master, url, endpoint,
                ConnectionType.WS, "test", new Handler<DSLink>() {
                    @Override
                    @SneakyThrows
                    public void handle(DSLink link) {
                        Main.this.link = link;
                        link.getResponder().createRoot("Demo");
                        link.getResponder().createRoot("Test");

                        System.out.println("Connecting...");
                        link.connect();
                        System.out.println("Connected");
                    }
                });

        while (running && (link == null || link.isConnected())) {
            Thread.sleep(500);
        }
        System.out.println("Disconnected");
    }

    @Subscribe
    public void errorHandler(AsyncExceptionEvent event) {
        System.err.println("A fatal error has occurred");
        event.getThrowable().printStackTrace();
        running = false;
    }
}

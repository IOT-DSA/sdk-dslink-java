package org.dsa.iot.demo;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final EventBus master = new EventBus();
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

        val url = "http://localhost:8080/conn";
        val endpoint = "ws://localhost:8080";

        link = DSLink.generate(master, url, endpoint,
                                ConnectionType.WS, "test");
        link.getResponder().createRoot("Demo");
        link.getResponder().createRoot("Test");

        System.out.println("Connecting...");
        link.connect();
        System.out.println("Connected");
        link.sleep();
        System.out.println("Disconnected");
    }

    @Subscribe
    public void errorHandler(AsyncExceptionEvent event) {
        System.err.println("A fatal error has occurred");
        event.getThrowable().printStackTrace();
    }
}

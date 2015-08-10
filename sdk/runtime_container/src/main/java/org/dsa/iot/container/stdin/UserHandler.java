package org.dsa.iot.container.stdin;

import org.dsa.iot.container.manager.DSLinkManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Samuel Grenier
 */
public class UserHandler {

    private final DSLinkManager manager;
    private final Path root;
    private final String brokerUrl;

    public UserHandler(DSLinkManager manager, Path root, String brokerUrl) {
        this.manager = manager;
        this.root = root;
        this.brokerUrl = brokerUrl;
    }

    public void start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                try {
                    int b;
                    while ((b = System.in.read()) != -1) {
                        if (b == '\n') {
                            byte[] bytes = baos.toByteArray();
                            try {
                                handleInput(new String(bytes, "UTF-8"));
                            } catch (Exception e) {
                                String err = "Failed to handle input: ";
                                err += new String(bytes, "UTF-8");
                                System.err.println(err);
                            }
                            baos.reset();
                        } else {
                            baos.write(b);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void handleInput(String input) throws Exception {
        input = input != null ? input.trim() : null;
        if (input == null || input.isEmpty()) {
            return;
        }
        String[] data = input.split(" ");
        if (data.length < 2) {
            System.err.println("Unknown command: " + input);
            return;
        }
        String command = data[0];
        String dslink = data[1];

        switch (command) {
            case "stop": {
                manager.stop(root.resolve(dslink));
                break;
            }
            case "start": {
                manager.load(root.resolve(dslink), brokerUrl);
                break;
            }
            default: {
                System.err.println("Unknown command: " + command);
            }
        }
    }

}

package org.dsa.iot.container.stdin;

import org.dsa.iot.container.manager.DSLinkManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Samuel Grenier
 */
public class UserHandler implements LinkHandler {

    private final DSLinkManager manager;
    private final Path root;
    private final String brokerUrl;

    public UserHandler(DSLinkManager manager, Path root, String brokerUrl) {
        this.manager = manager;
        this.root = root;
        this.brokerUrl = brokerUrl;
    }

    @Override
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
        String command = data[0];
        switch (command) {
            case "stop": {
                if (data.length < 2) {
                    System.err.println("Usage: stop <link>");
                    break;
                }
                Path path = root.resolve(data[1]);
                manager.stop(path);
                break;
            }
            case "start": {
                if (data.length < 2) {
                    System.err.println("Usage: start <link>");
                    break;
                }
                Path path = root.resolve(data[1]);
                manager.load(path, brokerUrl);
                break;
            }
            case "startAll": {
                manager.loadDirectory(root, brokerUrl);
                break;
            }
            case "stopAll": {
                manager.stopAll();
                break;
            }
            default: {
                System.err.println("Unknown command: " + command);
            }
        }
    }

}

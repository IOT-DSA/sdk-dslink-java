package org.dsa.iot.container.stdin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dsa.iot.container.manager.DSLinkInfo;
import org.dsa.iot.container.manager.DSLinkManager;
import org.dsa.iot.container.utils.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class StdinHandler implements LinkHandler {

    private final DSLinkManager manager;

    public StdinHandler(DSLinkManager manager) {
        this.manager = manager;
    }

    @Override
    public void start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                try {
                    int b;
                    boolean write = false;
                    while ((b = System.in.read()) != -1) {
                        if (b == '\u0002') {
                            write = true;
                        } else if (b == '\u0003') {
                            byte[] bytes = baos.toByteArray();
                            try {
                                handleInput(bytes);
                            } catch (Exception e) {
                                String err = "Failed to handle input: ";
                                err += new String(bytes, "UTF-8");
                                System.err.println(err);
                            }
                            write = false;
                            baos.reset();
                        } else if (write) {
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

    private void handleInput(byte[] json) throws Exception {
        ObjectMapper mapper = Json.getMapper();
        ObjectNode node = mapper.readValue(json, ObjectNode.class);

        String eventType = node.get("event").asText();
        switch (eventType) {
            case "start": {
                start(node);
                break;
            }
            case "stop": {
                stop(node);
                break;
            }
            default: {
                throw new RuntimeException("Unknown event: " + eventType);
            }
        }
    }

    private void start(ObjectNode node) {
        Path path = Paths.get(node.get("path").asText());
        try {
            JsonNode config = node.path("configs");
            if (config.isMissingNode()) {
                outputFail(path, "Missing configs", null);
                return;
            }
            String name = config.get("name").asText();
            String log = config.get("log").asText();
            String broker = config.get("broker").asText();
            String clazz = config.get("handler_class").asText();
            DSLinkInfo info = new DSLinkInfo(path, name, log, clazz, broker);
            try {
                manager.start(path, info);

                Map<String, Object> data = new HashMap<>();
                data.put("event", "started");
                data.put("path", path.toString());
                output(data);
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));

                String err = "Exception was thrown during DSLink starting";
                outputFail(path, err, writer.toString());
            }
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));

            String err = "Exception was thrown during JSON processing";
            outputFail(path, err, writer.toString());
        }
    }

    private void stop(ObjectNode node) {
        Path path = Paths.get(node.get("path").asText());
        try {
            manager.stop(path);
        } catch (Exception ignored) {
        } finally {
            Map<String, Object> data = new HashMap<>();
            data.put("event", "stopped");
            data.put("path", path.toString());
            output(data);
        }
    }

    private void outputFail(Path path, String summary, String details) {
        if (details == null) {
            details = "";
        }
        if (summary == null) {
            summary = "";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event", "fail");
        data.put("path", path.toString());
        data.put("summary", summary);
        data.put("details", details);
        output(data);
    }

    private void output(Map<String, Object> data) {
        try {
            ObjectMapper mapper = Json.getMapper();
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, data);
            String s = "\u0002" + writer.toString() + "\u0003";
            System.out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

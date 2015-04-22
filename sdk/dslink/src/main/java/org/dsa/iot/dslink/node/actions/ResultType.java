package org.dsa.iot.dslink.node.actions;

/**
 * Handles invocation result types on a node.
 *
 * @author Samuel Grenier
 */
public enum ResultType {

    /**
     * Default invocation type. Infers static results of a single row
     * populated with the desired values
     */
    VALUES("values"),

    /**
     * Streams results continuously. Infers highly dynamic results.
     */
    STREAM("stream"),

    /**
     * Streams results continuously similarly to the {@link #STREAM} type.
     * While the results are dynamic, they differ in such that the response
     * stream ends when there is no more data to be read.
     */
    TABLE("table");

    private final String jsonName;

    ResultType(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return The JSON name of the type.
     */
    public String getJsonName() {
        return jsonName;
    }

    public static ResultType toEnum(String type) {
        switch (type) {
            case "values":
                return VALUES;
            case "stream":
                return STREAM;
            case "table":
                return TABLE;
            default:
                throw new RuntimeException("Unsupported type: " + type);
        }
    }
}

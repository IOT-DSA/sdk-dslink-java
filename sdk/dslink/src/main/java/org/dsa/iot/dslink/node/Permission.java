package org.dsa.iot.dslink.node;

/**
 * Handles various permission levels
 * @author Samuel Grenier
 */
public enum Permission {
    NONE("none"),
    READ("read"),
    WRITE("write"),
    CONFIG("config"),
    NEVER("never");

    private final String jsonName;

    Permission(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return JSON ready name of the permission
     */
    public String getJsonName() {
        return jsonName;
    }

    /**
     * Converts a string permission received from an endpoint back into a
     * permission enumeration.
     *
     * @param perm Permission string to convert.
     * @return Converted string into an enumeration.
     */
    public static Permission toEnum(String perm) {
        switch (perm) {
            case "none":
                return NONE;
            case "read":
                return READ;
            case "write":
                return WRITE;
            case "config":
                return CONFIG;
            case "never":
                return NEVER;
            default:
                throw new RuntimeException("Unhandled type");
        }
    }
}

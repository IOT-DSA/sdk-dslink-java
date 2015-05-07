package org.dsa.iot.dslink.node;

/**
 * @author Samuel Grenier
 */
public enum Writable {

    /**
     * Whether anyone can write to the value.
     */
    WRITE,

    /**
     * Whether configuration permissions are required to write to the value.
     */
    CONFIG,

    /**
     * Can never write to the value.
     */
    NEVER;

    public String toJsonName() {
        return this.toString().toLowerCase();
    }

    public static Writable toEnum(String writable) {
        if (writable == null) {
            return NEVER;
        }
        switch (writable) {
            case "write":
                return WRITE;
            case "config":
                return CONFIG;
            case "never":
                return NEVER;
            default:
                throw new RuntimeException("Unhandled writable permission: " + writable);
        }
    }
}

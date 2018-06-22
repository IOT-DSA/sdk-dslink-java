package org.dsa.iot.dslink.util;

/**
 * Global string references for the system properties in use.
 *
 * @author Samuel Grenier
 * @see System#getProperty(String)
 */
public class PropertyReference {

    public static final String NAMESPACE = "dslink";

    /**
     * A string property of CSV values for the supported encoding formats
     * that can be used over the network for communication. If the property
     * is not specified then the SDK will allow using any supported format.
     */
    public static final String FORMATS = NAMESPACE + ".formats";

    /**
     * An integer property that determines the dispatch delay when a client
     * is unable to write to the network. During this delay, messages will be
     * merged until they can be sent over the network.
     *
     * Default value is 75.
     */
    public static final String DISPATCH_DELAY = NAMESPACE + ".dispatchDelay";

    /**
     * An integer property that determines the QOS queue size.  A value of 0 or less means an
     * unlimited queue.
     *
     * Default value is 0.
     */
    public static final String QOS_QUEUE_SIZE = NAMESPACE + ".qosQueueSize";

    /**
     * A boolean property that determines the sdk should perform any
     * validations. Currently only the dslink.json is validated.
     *
     * Default value is true.
     */
    public static final String VALIDATE = NAMESPACE + ".validate";

    /**
     * A boolean property that determines whether to validate the
     * dslink.json configuration fields.
     *
     * Default value is true.
     */
    public static final String VALIDATE_JSON = VALIDATE + ".json";

    /**
     * A boolean property that determines whether to validate the handler
     * class or not. The field must still exist at a minimum.
     *
     * Default value is true.
     */
    public static final String VALIDATE_HANDLER = VALIDATE + ".handler_class";

}

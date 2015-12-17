package org.dsa.iot.dslink.node.value;

/**
 * @author Samuel Grenier
 */
public class SubscriptionValue {

    private final String path;
    private final Value value;
    private final Number count;
    private final Number sum;
    private final Number min;
    private final Number max;

    public SubscriptionValue(String path, Value value,
                                Number count, Number sum,
                                Number min, Number max) {
        this.path = path;
        this.value = value;
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    public String getPath() {
        return path;
    }

    /**
     * @return Timestamp of the value
     * @see Value#getTimeStamp()
     * @see Value#getTime()
     */
    @Deprecated
    public String getTimestamp() {
        if (value == null) {
            return null;
        }
        return value.getTimeStamp();
    }

    public Value getValue() {
        return value;
    }

    public Number getCount() {
        return count;
    }

    public Number getSum() {
        return sum;
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }
}

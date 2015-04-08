package org.dsa.iot.dslink.node.value;

/**
 * @author Samuel Grenier
 */
public class SubscriptionValue {

    private final String path;
    private final String time;
    private final Value value;
    private final Integer count;
    private final Integer sum;
    private final Integer min;
    private final Integer max;

    public SubscriptionValue(String path, Value value, String time) {
        this(path, value, time, null, null, null, null);
    }

    public SubscriptionValue(String path, Value value, String time,
                                Integer count, Integer sum,
                                Integer min, Integer max) {
        this.path = path;
        this.time = time;
        this.value = value;
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    public String getPath() {
        return path;
    }

    public String getTimestamp() {
        return time;
    }

    public Value getValue() {
        return value;
    }

    public Integer getCount() {
        return count;
    }

    public Integer getSum() {
        return sum;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }
}

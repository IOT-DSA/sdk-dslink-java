package org.dsa.iot.core;

import lombok.Getter;

/**
 * @author Samuel Grenier
 */
@Getter
public class Pair<K, V> {
    
    private final K key;
    private final V value;
    
    public Pair(K key, V value) {
        this(key, value, true);
    }
    
    public Pair(K key, V value, boolean allowNullKey) {
        this(key, value, allowNullKey, true);
    }
    
    public Pair(K key, V value, boolean allowNullKey,
                                boolean allowNullValue) {
        if (!allowNullKey && key == null)
            throw new IllegalArgumentException("key");
        else if (!allowNullValue && value == null)
            throw new IllegalArgumentException("value");
        this.key = key;
        this.value = value;
    }
}

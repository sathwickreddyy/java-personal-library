package com.java.oops.cache.strategy;

/**
 * Interface for cache strategy
 * @param <K> Key of type K
 * @param <V> Value of type V
 */
public interface CachingStrategy<K, V> {
    /**
     * Reads the value from the cache with given Strategy
     * @param key Key of type K
     * @return Value of type V
     */
    V read(K key);

    /**
     * Writes the value to the cache with given Strategy
     * @param key Key of type K
     * @param value Value of type V
     */
    void write(K key, V value);
}

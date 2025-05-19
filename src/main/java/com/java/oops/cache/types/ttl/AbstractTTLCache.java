package com.java.oops.cache.types.ttl;

import com.java.oops.cache.types.AbstractCache;

import java.time.Duration;

/**
 * TTL Support Cache interface
 * @param <K> of type K
 * @param <V> of type V
 *
 * @author sathwick
 */
public interface AbstractTTLCache<K, V> extends AbstractCache<K, V> {
    /**
     * Updates the cache with key, value and ttl in milliseconds
     *
     * @param key Of type K
     * @param value Of type V
     * @param ttl of type Duration
     */
    void put(K key, V value, Duration ttl);
}

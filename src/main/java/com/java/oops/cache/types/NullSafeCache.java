package com.java.oops.cache.types;

import java.util.Optional;

/**
 * NullSafeCache with Null Object Pattern
 * @param <K> Key of type K
 * @param <V> Value of type V
 */
public class NullSafeCache<K,V> implements AbstractCache<K, V> {
    private final AbstractCache<K, V> delegateCache;

    /**
     * Null Safe Cache constructor
     * @param delegateCache Of type AbstractCache
     */
    public NullSafeCache(AbstractCache<K, V> delegateCache) {
        this.delegateCache = delegateCache;
    }

    /**
     * Updates the cache with key and value
     *
     * @param key   Of type K
     * @param value Of type V
     */
    @Override
    public void put(K key, V value) {
        if(value != null) {
            delegateCache.put(key, value);
        }
    }

    /**
     * Returns the value for the given key
     *
     * @param key Of type K
     * @return Optional of type V
     */
    @Override
    public Optional<V> get(K key) {
        return delegateCache.get(key);
    }

    /**
     * Evicts the key from the cache based on eviction policy
     *
     * @param key Of type K
     */
    @Override
    public void evict(K key) {
        delegateCache.evict(key);
    }
}

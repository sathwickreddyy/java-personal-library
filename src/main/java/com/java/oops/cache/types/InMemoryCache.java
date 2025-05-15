package com.java.oops.cache.types;

import com.java.oops.cache.eviction.EvictionPolicy;
import com.java.oops.cache.eviction.LRUEvictionPolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In Memory Cache - Generic
 * @param <K> Key of type K
 * @param <V> Value of type V
 * @author sathwick
 */
@Slf4j
@Getter
public class InMemoryCache<K, V> implements AbstractCache<K, V> {
    private final Map<K, V> cache;
    private final EvictionPolicy<K> evictionPolicy;
    private final Integer capacity;
    private int hitCount = 0;
    private int missCount = 0;

    /**
     * Initializes the cache
     * @param evictionPolicy EvictionPolicy to be used
     * @param capacity Capacity of the cache
     */
    public InMemoryCache(EvictionPolicy<K> evictionPolicy, Integer capacity) {
        this.cache = new HashMap<>();
        this.evictionPolicy = evictionPolicy;
        this.capacity = capacity;
    }

    /**
     * Initializes the cache with LRUEvictionPolicy
     * @param capacity Capacity of the cache
     */
    public InMemoryCache(Integer capacity) {
        this(new LRUEvictionPolicy<>(capacity), capacity);
    }

    /**
     * Updates the cache with key and value
     *
     * @param key   Of type K
     * @param value Of type V
     */
    @Override
    public void put(K key, V value) {
        if(!cache.containsKey(key) && cache.size() == capacity) {
            log.debug("Cache is full, evicting key according to eviction policy");
            K evictedKey = evictionPolicy.evict();
            cache.remove(evictedKey);
        }

        log.debug("Updating the cache with given key {} and value {}", key, value);
        evictionPolicy.recordAccess(key);
        cache.put(key, value);
    }

    /**
     * Returns the value for the given key
     *
     * @param key Of type K
     * @return Optional of type V
     */
    @Override
    public Optional<V> get(K key) {
        if(cache.containsKey(key)) {
            hitCount++;
            log.debug("Key Hit in the cache for key: {}", key);
            evictionPolicy.recordAccess(key);
            return Optional.ofNullable(cache.get(key));
        }
        missCount++;
        log.debug("Key miss in the cache for key: {}", key);
        return Optional.empty();
    }

    /**
     * Evicts the key from the cache based on eviction policy
     *
     * @param key Of type K
     */
    @Override
    public void evict(K key) {
        log.debug("Evicting the key from the cache");
        cache.remove(key);
        evictionPolicy.evict(key);
    }
}

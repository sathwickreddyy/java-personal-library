package com.java.oops.cache.types;

import com.java.oops.cache.eviction.EvictionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory cache implementation supporting TTL (Time-To-Live) for entries and pluggable eviction policy.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author sathwick
 */
@Slf4j
@RequiredArgsConstructor
public class InMemoryTTLCache<K, V> implements AbstractTTLCache<K, V> {
    // Underlying cache storage
    private final Map<K, CacheEntry<V>> cache = new HashMap<>();
    // Eviction policy to use when cache is full
    private final EvictionPolicy<K> evictionPolicy;
    // Maximum number of entries in cache
    private final Long capacity;
    // Special value for entries with no expiry
    private static final Long NO_EXPIRY = -1L;

    /**
     * Puts a key-value pair into the cache with a specified TTL.
     * If cache is at capacity and key is new, evicts an entry based on the eviction policy.
     *
     * @param key   the cache key
     * @param value the cache value
     * @param ttl   time-to-live duration; negative duration means no expiry
     */
    @Override
    public void put(K key, V value, Duration ttl) {
        boolean isNewKey = !cache.containsKey(key);
        if (isNewKey && cache.size() == capacity) {
            K toBeEvicted = evictionPolicy.evict();
            cache.remove(toBeEvicted);
            log.info("Evicted key '{}' due to capacity limit", toBeEvicted);
        }
        CacheEntry<V> cacheEntry = ttl.isNegative()
                ? new CacheEntry<>(value)
                : new CacheEntry<>(value, System.currentTimeMillis() + ttl.toMillis());
        cache.put(key, cacheEntry);
        evictionPolicy.recordAccess(key);
        log.info("Put key '{}' with TTL {} ms (expiry at {})", key, ttl.toMillis(),
                ttl.isNegative() ? "NO_EXPIRY" : (System.currentTimeMillis() + ttl.toMillis()));
    }

    /**
     * Puts a key-value pair with no expiry.
     *
     * @param key   the cache key
     * @param value the cache value
     */
    @Override
    public void put(K key, V value) {
        put(key, value, Duration.ofMillis(NO_EXPIRY));
    }

    /**
     * Retrieves the value for the given key, if present and not expired.
     * Removes the entry if expired.
     *
     * @param key the cache key
     * @return Optional containing the value if present and not expired, otherwise empty
     */
    @Override
    public Optional<V> get(K key) {
        CacheEntry<V> cacheEntry = cache.get(key);
        if (cacheEntry == null) {
            log.debug("Cache miss for key '{}'", key);
            return Optional.empty();
        }
        if (cacheEntry.isExpired()) {
            log.info("Cache entry for key '{}' expired, evicting", key);
            evict(key);
            return Optional.empty();
        }
        evictionPolicy.recordAccess(key);
        log.debug("Cache hit for key '{}'", key);
        return Optional.of(cacheEntry.getValue());
    }

    /**
     * Evicts the specified key from the cache and notifies the eviction policy.
     *
     * @param key the cache key to evict
     */
    @Override
    public void evict(K key) {
        cache.remove(key);
        evictionPolicy.evict(key);
        log.info("Manually evicted key '{}'", key);
    }
}

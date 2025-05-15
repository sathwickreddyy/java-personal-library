package com.java.oops.cache.types;

import com.java.oops.cache.eviction.EvictionPolicy;
import com.java.oops.cache.eviction.LRUEvictionPolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory cache implementation supporting TTL (Time-To-Live) for entries and pluggable eviction policy.
 * <pre>
 * Features:
 * - Stores key-value pairs with optional expiry (TTL).
 * - Integrates with a pluggable eviction policy for capacity management.
 * - Provides methods for insertion, retrieval, eviction, and cleanup of expired entries.
 *
 * Note: It includes a background thread for periodic cleanup of expired entries (every 2 minutes).
 * </pre>
 * @param <K> Key type
 * @param <V> Value type
 * @author sathwick
 */
@Slf4j
@Getter
public class InMemoryTTLCache<K, V> implements AbstractTTLCache<K, V>, AutoCloseable {
    private final Map<K, CacheEntry<V>> cache = new HashMap<>();
    private final EvictionPolicy<K> evictionPolicy;
    private final int capacity;
    private static final Duration NO_EXPIRY = Duration.ZERO;

    // Cleaner thread fields
    private final Thread cleanerThread;
    private volatile boolean running = true;

    /**
     * Creates an InMemoryTTLCache with the specified eviction policy and capacity.
     *
     * @param evictionPolicy the eviction policy to use for capacity management
     * @param capacity       the maximum number of entries that can be stored in the cache
     */
    public InMemoryTTLCache(EvictionPolicy<K> evictionPolicy, int capacity) {
        this(evictionPolicy, capacity, Duration.ofSeconds(120));
    }

    /**
     * Creates an InMemoryTTLCache with a default LRU eviction policy and the specified capacity.
     *
     * @param capacity the maximum number of entries that can be stored in the cache
     */
    public InMemoryTTLCache(int capacity) {
        this(new LRUEvictionPolicy<>(capacity), capacity);
    }

    /**
     * Creates an InMemoryTTLCache with the specified eviction policy, capacity, and cleanup interval.
     *
     * @param evictionPolicy   the eviction policy to use for capacity management
     * @param capacity         the maximum number of entries that can be stored in the cache
     * @param cleanupInterval  the interval at which expired entries should be cleaned up
     */
    public InMemoryTTLCache(EvictionPolicy<K> evictionPolicy, int capacity, Duration cleanupInterval) {
        this.evictionPolicy = evictionPolicy;
        this.capacity = capacity;
        // Start the cleaner thread
        this.cleanerThread = new Thread(() -> cleanerLoop(cleanupInterval.toMillis()), "InMemoryTTLCache-Cleaner");
        this.cleanerThread.setDaemon(true);
        this.cleanerThread.start();
        log.info("Started cache cleaner thread with interval {} seconds", cleanupInterval.toSeconds());
    }

    /**
     * Inserts a key-value pair into the cache with a specified TTL.
     * If the cache is at capacity and the key is new, evicts an entry based on the eviction policy.
     *
     * @param key   the cache key
     * @param value the cache value
     * @param ttl   time-to-live duration; Duration.ZERO means no expiry
     */
    @Override
    public void put(K key, V value, Duration ttl) {
        boolean isNewKey = !cache.containsKey(key);
        if (isNewKey && cache.size() == capacity) {
            K toBeEvicted = evictionPolicy.evict();
            cache.remove(toBeEvicted);
            log.info("Evicted key '{}' due to capacity limit", toBeEvicted);
        }
        long expiryTime = ttl.isZero() ? -1L : System.currentTimeMillis() + ttl.toMillis();
        CacheEntry<V> cacheEntry = ttl.isZero()
                ? new CacheEntry<>(value)
                : new CacheEntry<>(value, expiryTime);
        cache.put(key, cacheEntry);
        evictionPolicy.recordAccess(key);
        log.info("Put key '{}' with TTL {} ms (expiry at {})", key, ttl.toMillis(),
                ttl.isZero() ? "NO_EXPIRY" : expiryTime);
    }

    /**
     * Inserts a key-value pair into the cache with no expiry.
     *
     * @param key   the cache key
     * @param value the cache value
     */
    @Override
    public void put(K key, V value) {
        put(key, value, NO_EXPIRY);
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

    /**
     * Removes all expired entries from the cache.
     * For each expired entry, logs the removal and notifies the eviction policy.
     */
    private void cleanUpExpiredEntries() {
        log.debug("Starting cleanup of expired entries");
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                K key = entry.getKey();
                log.info("Cleaner thread: Removing expired key '{}'", key);
                evictionPolicy.evict(key);
                return true;
            }
            return false;
        });
        log.debug("Completed cleanup of expired entries");
    }

    /**
     * Periodically runs cleanup of expired entries until stopped.
     */
    private void cleanerLoop(Long cleanupIntervalMillis) {
        while (running) {
            try {
                Thread.sleep(cleanupIntervalMillis);
                cleanUpExpiredEntries();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Cleaner thread interrupted, shutting down.");
                break;
            } catch (Exception e) {
                log.error("Exception in cleaner thread: ", e);
            }
        }
    }


    /**
     * Stops the cleaner thread and performs any necessary cleanup.
     */
    @Override
    public void close() {
        running = false;
        cleanerThread.interrupt();
        log.info("Cache cleaner thread stopped.");
    }
}

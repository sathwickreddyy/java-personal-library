package com.java.oops.cache.strategy;

import com.java.oops.cache.database.DatabaseService;
import com.java.oops.cache.types.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Implements the Cache-Aside (Lazy Loading) caching strategy.
 *
 * <p>
 * In Cache-Aside strategy, the application manages cache interactions explicitly:
 * <ul>
 *   <li>On read: Check cache first. If data is not present (cache miss), retrieve from database and populate cache.</li>
 *   <li>On write: Write directly to database and invalidate or update cache accordingly.</li>
 * </ul>
 *
 * This strategy is suitable for:
 * <ul>
 *   <li>Read-heavy workloads (e.g., e-commerce product details pages)</li>
 *   <li>Unpredictable resource demand scenarios</li>
 *   <li>Situations where caching logic needs explicit control</li>
 * </ul>
 *
 * @param <K> Type of cache key
 * @param <V> Type of cache value
 */
@Slf4j
public class CacheAsideStrategy<K, V> implements CachingStrategy<K, V> {

    private final AbstractCache<K, V> cache;
    private final DatabaseService<K, V> databaseService;

    /**
     * Initializes CacheAsideStrategy with specified cache and database service.
     *
     * @param cache AbstractCache implementation used for caching operations
     * @param databaseService DatabaseService implementation used for persistent storage
     */
    public CacheAsideStrategy(AbstractCache<K, V> cache, DatabaseService<K, V> databaseService) {
        this.cache = cache;
        this.databaseService = databaseService;
    }

    /**
     * Reads data using Cache-Aside pattern.
     *
     * @param key Key to retrieve from cache or database
     * @return Cached value if present; otherwise data loaded from DB (or null if not found)
     */
    @Override
    public V read(K key) {
        try {
            Optional<V> cachedValue = cache.get(key);
            if (cachedValue.isPresent()) {
                log.debug("Cache hit for key: {}", key);
                return cachedValue.get();
            }

            log.debug("Cache miss for key: {}. Fetching from database...", key);
            V dbValue = databaseService.load(key);

            if (dbValue != null) {
                cache.put(key, dbValue);
                log.debug("Loaded data from DB and updated cache for key: {}", key);
            } else {
                log.warn("No data found in DB for key: {}", key);
            }
            return dbValue;
        } catch (Exception e) {
            log.error("Exception during read operation for key: {}", key, e);
            return null;
        }
    }

    /**
     * Writes data to the database first and then invalidates the corresponding cache entry.
     *
     * @param key Key to be updated
     * @param value Value to persist in DB and invalidate in cache
     */
    @Override
    public void write(K key, V value) {
        try {
            databaseService.save(key, value);
            log.debug("Successfully saved data to DB for key: {}", key);

            // Invalidate the cache entry after DB update to maintain consistency
            cache.evict(key);
            log.debug("Successfully invalidated cache entry for key: {}", key);
        } catch (Exception e) {
            log.error("Exception during write operation for key: {}", key, e);
        }
    }
}

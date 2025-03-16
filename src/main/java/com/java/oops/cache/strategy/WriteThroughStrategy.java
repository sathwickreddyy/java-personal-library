package com.java.oops.cache.strategy;

import com.java.oops.cache.database.DatabaseService;
import com.java.oops.cache.types.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Implements the Write-Through caching strategy.
 *
 * <p>
 * In Write-Through caching:
 * <ul>
 *   <li>On writes, data is written synchronously to both cache and database simultaneously.</li>
 *   <li>On reads, data is fetched directly from cache if available; otherwise loaded from database.</li>
 * </ul>
 *
 * <p>
 * Suitable for scenarios:
 * <ul>
 *   <li>When data consistency between cache and database is critical.</li>
 *   <li>Moderate write frequency with immediate consistency requirements.</li>
 * </ul>
 *
 * @param <K> Type of cache key
 * @param <V> Type of cache value
 */
@Slf4j
public class WriteThroughStrategy<K, V> implements CachingStrategy<K, V> {

    private final AbstractCache<K, V> cache;
    private final DatabaseService<K, V> databaseService;

    /**
     * Constructs a WriteThroughStrategy instance.
     *
     * @param cache           Cache implementation used for caching operations
     * @param databaseService Database service implementation for persistent storage
     */
    public WriteThroughStrategy(AbstractCache<K, V> cache, DatabaseService<K, V> databaseService) {
        this.cache = cache;
        this.databaseService = databaseService;
    }

    /**
     * Reads data directly from the cache. If data is not found in the cache,
     * retrieves it from the database (without populating the cache automatically).
     *
     * @param key Key to read from cache/database
     * @return Cached value if present; otherwise loads from DB without caching it automatically (can be adjusted as per use-case)
     */
    @Override
    public V read(K key) {
        try {
            Optional<V> cachedValue = cache.get(key);
            if (cachedValue.isPresent()) {
                log.debug("Cache hit for key: {}", key);
                return cachedValue.get();
            }

            log.debug("Cache miss for key: {}. Retrieving from database...", key);
            V dbValue = databaseService.load(key);

            if (dbValue != null) {
                // Optionally populate the cache after DB retrieval to improve subsequent reads
                cache.put(key, dbValue);
                log.debug("Loaded and cached data after DB retrieval for key: {}", key);
                return dbValue;
            } else {
                log.warn("No data found in DB for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Error during read operation for key: {}", key, e);
            return null;
        }
    }

    /**
     * Writes data synchronously to both the database and the cache.
     *
     * @param key   Key to write/update
     * @param value Value to persist into DB and update in cache
     */
    @Override
    public void write(K key, V value) {
        try {
            // First write to the database
            databaseService.save(key, value);
            log.debug("Successfully persisted data into DB for key: {}", key);

            // Immediately update the cache after successful DB write
            cache.put(key, value);
            log.debug("Successfully updated cache entry after DB write for key: {}", key);
        } catch (Exception e) {
            log.error("Error during write-through operation for key: {}", key, e);
        }
    }
}

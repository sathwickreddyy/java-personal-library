package com.java.oops.cache.strategy;

import com.java.oops.cache.database.DatabaseService;
import com.java.oops.cache.types.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Implements the Read-Through caching strategy.
 *
 * <p>
 * In Read-Through caching:
 * <ul>
 *   <li>On reads, the application always interacts directly with the cache.</li>
 *   <li>If data is missing in the cache (cache miss), the cache itself fetches data from the database transparently,
 *       populates itself, and returns the value to the caller.</li>
 *   <li>On writes, data is written directly to both the cache and database simultaneously (synchronous update).</li>
 * </ul>
 *
 * <p>
 * Use Read-Through caching when:
 * <ul>
 *   <li>Your application has a high read-to-write ratio.</li>
 *   <li>You want to simplify application code by delegating cache population logic to caching layer.</li>
 *   <li>You prefer strong consistency between cache and database.</li>
 * </ul>
 *
 * @param <K> Type of cache key
 * @param <V> Type of cache value
 */
@Slf4j
public class ReadThroughStrategy<K, V> implements CachingStrategy<K, V> {

    private final AbstractCache<K, V> cache;
    private final DatabaseService<K, V> databaseService;

    /**
     * Constructs a ReadThroughStrategy instance.
     *
     * @param cache           Cache implementation used for caching operations
     * @param databaseService Database service used for persistent storage operations
     */
    public ReadThroughStrategy(AbstractCache<K, V> cache, DatabaseService<K, V> databaseService) {
        this.cache = cache;
        this.databaseService = databaseService;
    }

    /**
     * Reads data using Read-Through strategy.
     *
     * @param key Cache key
     * @return Cached value if present; otherwise loads from DB, caches it, and returns it. Returns null if not found.
     */
    @Override
    public V read(K key) {
        try {
            Optional<V> cachedValue = cache.get(key);
            if (cachedValue.isPresent()) {
                log.debug("Cache hit for key: {}", key);
                return cachedValue.get();
            }

            log.debug("Cache miss for key: {}. Loading from DB.", key);
            V dbValue = databaseService.load(key);

            if (dbValueExists(dbValue)) {
                cache.put(key, dbValue);
                log.debug("Loaded data from DB and cached successfully for key: {}", key);
                return dbValue;
            } else {
                log.warn("No data found in DB for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Exception during read operation for key: {}", key, e);
            return null;
        }
    }

    /**
     * Writes data synchronously into both DB and Cache.
     *
     * @param key   Cache key
     * @param value Value to write
     */
    @Override
    public void write(K key, V value) {
        try {
            databaseService.save(key, value);
            log.debug("Saved data into DB successfully for key: {}", key);

            // Update cache synchronously after DB write
            cache.put(key, value);
            log.debug("Updated Redis cache successfully after DB write for key: {}", key);

        } catch (Exception e) {
            log.error("Error during write operation for key: {}", key, e);
        }
    }

    /**
     * Helper method to check if DB returned valid data (non-null).
     *
     * @param dbValue Value fetched from DB
     * @return true if dbValue is not null; false otherwise
     */
    private boolean dbValueExists(V dbValue) {
        return dbValue != null;
    }
}

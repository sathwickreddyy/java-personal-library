package com.java.oops.cache.strategy;

import com.java.oops.cache.database.DatabaseService;
import com.java.oops.cache.types.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements the Write-Behind caching strategy.
 *
 * <p>
 * In Write-Behind caching:
 * <ul>
 *   <li>On writes, data is immediately updated in cache and asynchronously persisted to database.</li>
 *   <li>On reads, data is fetched directly from cache. If not present, loaded from DB and cached.</li>
 * </ul>
 *
 * <p>
 * Use Write-Behind caching when:
 * <ul>
 *   <li>You have write-heavy workloads requiring high throughput.</li>
 *   <li>You can tolerate eventual consistency between cache and persistent storage.</li>
 * </ul>
 *
 * @param <K> Type of cache key
 * @param <V> Type of cache value
 */
@Slf4j
public class WriteBehindStrategy<K, V> implements CachingStrategy<K, V> {

    private final AbstractCache<K, V> cache;
    private final DatabaseService<K, V> databaseService;
    private final ExecutorService executorService;

    /**
     * Constructs a WriteBehindStrategy instance with provided cache and database service.
     *
     * @param cache           Cache implementation used for caching operations
     * @param databaseService Database service implementation for persistent storage
     * @param executorService Executor service for asynchronous operations
     */
    public WriteBehindStrategy(AbstractCache<K, V> cache, DatabaseService<K, V> databaseService, ExecutorService executorService) {
        this.cache = cache;
        this.databaseService = databaseService;
        this.executorService = executorService;
    }

    /**
     * Reads data directly from the cache. On cache miss, loads from DB and caches it.
     *
     * @param key Key to read from cache/database
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

            log.debug("Cache miss for key: {}. Retrieving from database...", key);
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
     * Writes data immediately to the cache and asynchronously persists the data to the database.
     *
     * @param key   Cache key to write/update
     * @param value Value to persist asynchronously
     */
    @Override
    public void write(K key, V value) {
        try {
            // Immediately update cache first
            cache.put(key, value);
            log.debug("Successfully updated cache entry immediately for key: {}", key);

            // Asynchronously persist data into DB using executor service
            executorService.submit(() -> {
                try {
                    databaseService.save(key, value);
                    log.debug("Asynchronously persisted data into DB successfully for key: {}", key);
                } catch (Exception ex) {
                    log.error("Async write failed for key: {}", key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error during write operation (cache update) for key: {}", key, e);
        }
    }

    /**
     * Shuts down the executor service gracefully.
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Error shutting down WriteBehindStrategy executor", e);
            Thread.currentThread().interrupt();
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

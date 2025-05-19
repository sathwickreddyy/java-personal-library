package com.java.oops.cache.types;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a distributed cache abstraction with advanced features.
 * <p>
 * Extends {@link AbstractCache} and adds support for time-to-live (TTL), distributed locking,
 * bulk operations, and cache-wide clearing.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @author sathwick
 */
public interface AbstractDistributedCache<K, V> extends AbstractCache<K, V> {

    /**
     * Inserts or updates the value associated with the specified key in the cache,
     * with a given time-to-live (TTL) duration.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl   the duration after which the entry should expire and be evicted from the cache
     */
    void put(K key, V value, Duration ttl);

    /**
     * Attempts to acquire a distributed lock for the specified key, waiting up to the specified timeout.
     * <p>
     * This is useful for scenarios where concurrent modifications to the same key must be prevented.
     *
     * @param key     the key for which the lock is to be acquired
     * @param timeout the maximum time to wait for the lock to become available
     * @return {@code true} if the lock was acquired, {@code false} if the timeout elapsed before the lock could be acquired
     */
    Boolean acquireLock(K key, Duration timeout);

    /**
     * Releases a previously acquired distributed lock for the specified key.
     *
     * @param key the key for which the lock is to be released
     * @return {@code true} if the lock was successfully released, {@code false} if the lock was not held or could not be released
     */
    Boolean releaseLock(K key);

    /**
     * Removes all entries from the cache.
     * <p>
     * Use with caution in production environments, as this operation may be expensive and affect all clients.
     */
    void clear();

    /**
     * Inserts or updates multiple entries in the cache in a single bulk operation.
     *
     * @param entries a map of key-value pairs to be inserted or updated in the cache
     */
    void putAll(Map<K, V> entries);

    /**
     * Retrieves the values associated with the specified keys.
     *
     * @param keys a collection of keys whose associated values are to be returned
     * @return a map of keys to {@link Optional} values; if a key is not present, its value will be {@link Optional#empty()}
     */
    Map<K, Optional<V>> getAll(Collection<K> keys);
}
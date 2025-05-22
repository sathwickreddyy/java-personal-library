package com.java.oops.cache.types.distributed;

import com.java.oops.cache.types.AbstractCache;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
     * @throws Exception if an error occurs during the operation
     */
    void put(K key, V value, Duration ttl) throws Exception;

    /**
     * Attempts to acquire a distributed lock for the specified key, waiting up to the specified timeout.
     * <p>
     * This is useful for scenarios where concurrent modifications to the same key must be prevented.
     *
     * @param key     the key for which the lock is to be acquired
     * @param value   the value to be associated with the lock
     * @param timeout the maximum time to wait for the lock to become available
     * @return {@code true} if the lock was acquired, {@code false} if the timeout elapsed before the lock could be acquired
     * @throws Exception if an error occurs during the operation
     */
    Boolean acquireLock(String key, String value, Duration timeout) throws Exception;

    /**
     * Releases a previously acquired distributed lock for the specified key.
     *
     * @param key the key for which the lock is to be released
     * @return {@code true} if the lock was successfully released, {@code false} if the lock was not held or could not be released
     * @throws Exception if an error occurs during the operation
     */
    Boolean releaseLock(String key) throws Exception;

    /**
     * Returns the value associated with the specified key if a lock is held and not expired.
     * @param key key
     * @return Lock Value if lock is held and not expired
     * @throws Exception if an error occurs
     */
    String fetchLockValue(String key) throws Exception;

    /**
     * Returns default lock key
     * @param key key
     * @return lock prefixed key
     */
    default String getLockKey(String key) {
        return "lock:" + key;
    }

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
     * @throws Exception if an error occurs during the operation
     */
    void putAll(Map<K, CacheEntry<V>> entries) throws Exception;

    /**
     * Retrieves the values associated with the specified keys.
     *
     * @param keys a collection of keys whose associated values are to be returned
     * @return a map of keys to {@link Optional} values; if a key is not present, its value will be {@link Optional#empty()}
     * @throws Exception if an error occurs during the operation
     */
    Map<K, Optional<V>> getAll(Collection<K> keys) throws Exception;

    /***
     * Distributed cache entry
     * @param <V> of type V
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    class CacheEntry<V> {
        private final V value;
        private Duration ttl;
    }
}
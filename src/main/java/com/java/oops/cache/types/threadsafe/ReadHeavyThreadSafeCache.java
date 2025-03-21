package com.java.oops.cache.types.threadsafe;

import com.java.oops.cache.types.AbstractCache;
import com.java.oops.cache.types.NullSafeCache;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe cache implementation that combines null safety with read-write lock strategy.
 * This implementation extends NullSafeCache and adds thread safety for concurrent operations.
 * It is optimized for read-heavy workloads where reads significantly outnumber writes.
 *
 * <p>Multiple threads can read from the cache simultaneously, but writes are exclusive.</p>
 * <p>This implementation also ensures null values are not stored in the cache.</p>
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of values maintained by this cache
 */
public class ReadHeavyThreadSafeCache<K, V> extends NullSafeCache<K, V> {
    private final ReadWriteLock readWriteLock;

    /**
     * Constructs a new ThreadSafeNullSafeCache that decorates the specified cache implementation.
     *
     * @param delegateCache The underlying cache implementation to be made thread-safe and null-safe
     * @throws NullPointerException if the delegate cache is null
     */
    public ReadHeavyThreadSafeCache(AbstractCache<K, V> delegateCache) {
        super(delegateCache);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    /**
     * Stores a key-value pair in the cache if the value is not null.
     * This operation acquires a write lock to ensure thread safety.
     *
     * @param key   The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @throws NullPointerException if the key is null
     */
    @Override
    public void put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Cache key cannot be null");
        }

        readWriteLock.writeLock().lock();
        try {
            super.put(key, value);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the value associated with the specified key from the cache.
     * This operation acquires a read lock, allowing multiple concurrent reads.
     *
     * @param key The key whose associated value is to be returned
     * @return An Optional containing the value to which the specified key is mapped,
     *         or an empty Optional if the cache contains no mapping for the key
     * @throws NullPointerException if the key is null
     */
    @Override
    public Optional<V> get(K key) {
        if (key == null) {
            throw new NullPointerException("Cache key cannot be null");
        }

        readWriteLock.readLock().lock();
        try {
            return super.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * Removes the mapping for a key from the cache if it is present.
     * This operation acquires a write lock to ensure thread safety.
     *
     * @param key The key whose mapping is to be removed from the cache
     * @throws NullPointerException if the key is null
     */
    @Override
    public void evict(K key) {
        if (key == null) {
            throw new NullPointerException("Cache key cannot be null");
        }

        readWriteLock.writeLock().lock();
        try {
            super.evict(key);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}

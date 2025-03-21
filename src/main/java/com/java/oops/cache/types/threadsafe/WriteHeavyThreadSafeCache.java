package com.java.oops.cache.types.threadsafe;

import com.java.oops.cache.types.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe cache implementation optimized for write-heavy workloads.
 * This implementation uses a striped lock pattern to reduce contention when multiple threads
 * are writing to different parts of the cache simultaneously.
 *
 * <p>The striped lock approach divides the key space into multiple segments based on key hash codes,
 * allowing concurrent modifications to different segments of the cache. This significantly improves
 * performance in scenarios with frequent write operations across different keys.</p>
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of values maintained by this cache
 */
@Slf4j
public class WriteHeavyThreadSafeCache<K, V> implements AbstractCache<K, V> {
    private final AbstractCache<K, V> delegateCache;
    private final Lock[] locks;
    private final int concurrencyLevel;

    /**
     * Constructs a new WriteHeavyThreadSafeCache with the specified delegate cache and concurrency level.
     * The concurrency level determines the number of lock segments used to manage concurrent access.
     *
     * @param delegateCache The underlying cache implementation to be made thread-safe
     * @param concurrencyLevel The estimated number of concurrently updating threads (determines number of locks)
     * @throws NullPointerException if the delegate cache is null
     * @throws IllegalArgumentException if the concurrency level is not positive
     */
    public WriteHeavyThreadSafeCache(AbstractCache<K, V> delegateCache, int concurrencyLevel) {
        if(delegateCache == null) {
            throw new NullPointerException("Delegate cache cannot be null");
        }
        if(concurrencyLevel <= 0) {
            throw new IllegalArgumentException("Concurrency level must be positive");
        }
        this.delegateCache = delegateCache;
        this.concurrencyLevel = concurrencyLevel;
        this.locks = new ReentrantLock[concurrencyLevel];

        // Initialize all locks
        for (int i = 0; i < concurrencyLevel; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    /**
     * Constructs a new WriteHeavyThreadSafeCache with the specified delegate cache and a default
     * concurrency level equal to the number of available processors in the system.
     *
     * @param delegateCache The underlying cache implementation to be made thread-safe
     * @throws NullPointerException if the delegate cache is null
     */
    public WriteHeavyThreadSafeCache(AbstractCache<K, V> delegateCache) {
        this(delegateCache, Runtime.getRuntime().availableProcessors()*2);
        log.info("Creating cache with concurrency level: {}", Runtime.getRuntime().availableProcessors()*2);
    }

    /**
     * Returns the appropriate lock for the specified key based on its hash code.
     * This method distributes keys across the available locks to minimize contention.
     *
     * @param key The key to get the lock for
     * @return The lock associated with the key's hash segment
     */
    private Lock getLockForKey(K key) {
        int hashCode = key.hashCode();
        // Ensure non-negative hash code
        hashCode = hashCode < 0 ? -hashCode : hashCode;
        return locks[hashCode % concurrencyLevel];
    }

    /**
     * Stores a key-value pair in the cache.
     * This operation acquires a lock specific to the key's hash to reduce contention
     * when multiple threads are writing to different parts of the cache.
     *
     * @param key   The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @throws NullPointerException if the key is null
     */
    @Override
    public void put(K key, V value) {
        if(key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        Lock lock = getLockForKey(key);
        lock.lock();
        try {
            delegateCache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the value associated with the specified key from the cache.
     * This operation acquires a lock specific to the key's hash.
     *
     * @param key The key whose associated value is to be returned
     * @return An Optional containing the value to which the specified key is mapped,
     *         or an empty Optional if the cache contains no mapping for the key
     * @throws NullPointerException if the key is null
     */
    @Override
    public Optional<V> get(K key) {
        if(key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        Lock lock = getLockForKey(key);
        lock.lock();
        try {
            return delegateCache.get(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the mapping for a key from the cache if it is present.
     * This operation acquires a lock specific to the key's hash.
     *
     * @param key The key whose mapping is to be removed from the cache
     * @throws NullPointerException if the key is null
     */
    @Override
    public void evict(K key) {
        if(key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        Lock lock = getLockForKey(key);
        lock.lock();
        try {
            delegateCache.evict(key);
        } finally {
            lock.unlock();
        }
    }
}

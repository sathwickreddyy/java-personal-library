package com.java.oops.cache.types.distributed;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

import java.io.*;
import java.time.Duration;
import java.util.*;

/**
 * Redis-based implementation of AbstractDistributedCache.
 * Provides distributed caching capabilities using Redis.
 *
 * @param <K> Type of cache key (must be Serializable)
 * @param <V> Type of cache value (must be Serializable)
 * @author sathwick
 */
@Slf4j
public class RedisDistributedCache<K extends Serializable, V extends Serializable> implements AbstractDistributedCache<K, V> {

    private final Jedis redisClient;

    /**
     * Constructs a RedisDistributedCache instance with provided Jedis client.
     *
     * @param redisClient Jedis client instance
     */
    public RedisDistributedCache(Jedis redisClient) {
        this.redisClient = redisClient;
    }

    /**
     * Stores a key-value pair in Redis cache.
     *
     * @param key   Cache key
     * @param value Cache value
     */
    @Override
    public void put(K key, V value) {
        try {
            byte[] serializedKey = serialize(key);
            byte[] serializedValue = serialize(value);
            redisClient.set(serializedKey, serializedValue);
            log.debug("Successfully cached data for key: {}", key);
        } catch (IOException | JedisException e) {
            log.error("Failed to put data into Redis cache for key: {}", key, e);
        }
    }

    /**
     * Retrieves a value from Redis cache for the given key.
     *
     * @param key Cache key
     * @return Optional containing the cached value if present; otherwise Optional.empty()
     */
    @Override
    public Optional<V> get(K key) {
        try {
            byte[] serializedKey = serialize(key);
            byte[] result = redisClient.get(serializedKey);
            if (result == null) {
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
            V value = deserialize(result);
            log.debug("Cache hit for key: {}", key);
            return Optional.ofNullable(value);
        } catch (IOException | ClassNotFoundException | JedisException e) {
            log.error("Failed to retrieve data from Redis cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Removes a given key and its associated value from Redis cache.
     *
     * @param key Cache key to evict
     */
    @Override
    public void evict(K key) {
        try {
            byte[] serializedKey = serialize(key);
            redisClient.del(serializedKey);
            log.debug("Successfully evicted cache entry for key: {}", key);
        } catch (IOException | JedisException e) {
            log.error("Failed to evict data from Redis cache for key: {}", key, e);
        }
    }

    /**
     * Serializes an object into a byte array.
     *
     * @param obj Object to serialize
     * @return Serialized byte array representation of the object
     * @throws IOException If serialization fails
     */
    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    /**
     * Deserializes an object from a byte array.
     *
     * @param bytes Byte array to deserialize from
     * @return Deserialized object of type V
     * @throws IOException If deserialization fails due to I/O issues
     * @throws ClassNotFoundException If the class of the deserialized object cannot be found
     */
    @SuppressWarnings("unchecked")
    private V deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (V) in.readObject();
        }
    }

    /**
     * Stores a key-value pair in Redis cache with a specified TTL.
     *
     * @param key   Cache key
     * @param value Cache value
     * @param ttl   Time-to-live duration
     * @throws JedisException if Redis operation fails
     * @throws IOException    if serialization fails
     */
    @Override
    public void put(K key, V value, Duration ttl) throws JedisException, IOException {
        try {
            byte[] serializedKey = serialize(key);
            byte[] serializedValue = serialize(value);
            redisClient.psetex(serializedKey, ttl.toMillis(), serializedValue);
            log.debug("Successfully cached data for key {} with TTL: {}", key, ttl);
        } catch (IOException | JedisException e) {
            log.error("Failed to put data into Redis cache for key: {}", key, e);
            throw e;
        }
    }

    /**
     * Attempts to acquire a distributed lock for the specified key, waiting up to the specified timeout.
     *
     * @param key     the key for which the lock is to be acquired
     * @param timeout the maximum time to wait for the lock to become available
     * @return {@code true} if the lock was acquired, {@code false} if the timeout elapsed before the lock could be acquired
     * @throws JedisException if Redis operation fails
     */
    @Override
    public Boolean acquireLock(String key, String lockValue, Duration timeout) throws JedisException {
        try {
            String lockKey = "lock:" + key;
            String result = redisClient.set(lockKey, lockValue, SetParams.setParams().nx().px(timeout.toMillis()));
            if ("OK".equals(result)) {
                log.debug("Lock acquired for key: {}", key);
                return true;
            }
            log.warn("Failed to acquire lock for key: {} with timeout: {}", key, timeout);
            return false;
        }
        catch (JedisException e) {
            log.error("Failed to acquire lock for key: {} with timeout: {}", key, timeout, e);
            throw e;
        }
    }

    /**
     * Releases a previously acquired distributed lock for the specified key.
     *
     * @param key the key for which the lock is to be released
     * @return {@code true} if the lock was successfully released, {@code false} otherwise
     * @throws JedisException if Redis operation fails
     */
    @Override
    public Boolean releaseLock(String key) throws JedisException {
        String lockKey = "lock:" + key;
        try {
            Long result = redisClient.del(lockKey);
            if (result != null && result > 0) {
                log.debug("Lock released for key: {}", key);
                return true;
            } else {
                log.warn("No lock found to release for key: {}", key);
                return false;
            }
        } catch (JedisException e) {
            log.error("Failed to release lock for key: {}", key, e);
            throw e;
        }
    }

    /**
     * Removes all entries from the cache.
     * <p>
     * Use with caution in production environments, as this operation may be expensive and affect all clients.
     */
    @Override
    public void clear() {
        try {
            redisClient.flushDB();
            log.warn("All cache entries cleared from Redis.");
        } catch (JedisException e) {
            log.error("Failed to clear Redis cache.", e);
        }
    }

    /**
     * <p>Inserts or updates multiple entries in the cache in a single bulk operation.</p>
     *
     * <p>Bulk operations in Redis is efficient by using pipelining i.e., a feature that allows you to send
     * multiple commands to the server without waiting for individual responses.
     * This reduces network round-trips and can significantly improve performance for batch operations.</p>
     *
     * @param entries a map of key-value pairs to be inserted or updated in the cache
     */
    @Override
    public void putAll(Map<K, CacheEntry<V>> entries) {
        if (entries == null || entries.isEmpty()) {
            log.debug("No entries provided to putAll.");
            return;
        }
        try {
            Pipeline pipeline = redisClient.pipelined();
            for (Map.Entry<K, CacheEntry<V>> entry : entries.entrySet()) {
                K key = entry.getKey();
                CacheEntry<V> cacheEntry = entry.getValue();
                try {
                    byte[] serializedKey = serialize(key);
                    byte[] serializedValue = serialize(cacheEntry.getValue());
                    if (cacheEntry.getTtl() != null) {
                        pipeline.psetex(serializedKey, cacheEntry.getTtl().toMillis(), serializedValue);
                    } else {
                        pipeline.set(serializedKey, serializedValue);
                    }
                } catch (IOException e) {
                    log.error("Failed to serialize entry for key: {} in putAll", key, e);
                }
            }
            pipeline.sync(); // Executes all commands in the pipeline
            log.info("Bulk putAll operation completed for {} entries.", entries.size());
        } catch (JedisException e) {
            log.error("Redis error during putAll operation.", e);
        }
    }


    /**
     * Retrieves the values associated with the specified keys.
     *
     * @param keys a collection of keys whose associated values are to be returned
     * @return a map of keys to {@link Optional} values; if a key is not present, its value will be {@link Optional#empty()}
     */
    @Override
    public Map<K, Optional<V>> getAll(Collection<K> keys) {
        Map<K, Optional<V>> result = new HashMap<>();
        if (keys == null || keys.isEmpty()) {
            log.debug("No keys provided to getAll.");
            return result;
        }
        try {
            Pipeline pipeline = redisClient.pipelined();
            Map<K, Response<byte[]>> responses = new HashMap<>();
            for (K key : keys) {
                try {
                    byte[] serializedKey = serialize(key);
                    responses.put(key, pipeline.get(serializedKey));
                } catch (IOException e) {
                    log.error("Failed to serialize key: {} in getAll", key, e);
                    result.put(key, Optional.empty());
                }
            }
            pipeline.sync(); // Executes all commands in the pipeline

            for (Map.Entry<K, Response<byte[]>> entry : responses.entrySet()) {
                K key = entry.getKey();
                try {
                    byte[] valueBytes = entry.getValue().get();
                    if (valueBytes == null) {
                        result.put(key, Optional.empty());
                    } else {
                        V value = deserialize(valueBytes);
                        result.put(key, Optional.ofNullable(value));
                    }
                } catch (Exception e) {
                    log.error("Failed to deserialize value for key: {} in getAll", key, e);
                    result.put(key, Optional.empty());
                }
            }
            log.info("Bulk getAll operation completed for {} keys.", keys.size());
        } catch (JedisException e) {
            log.error("Redis error during getAll operation.", e);
        }
        return result;
    }


}

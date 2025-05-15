package com.java.oops.cache.types;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.util.Optional;

/**
 * Redis-based implementation of AbstractCache.
 * Provides distributed caching capabilities using Redis.
 *
 * @param <K> Type of cache key (must be Serializable)
 * @param <V> Type of cache value (must be Serializable)
 * @author sathwick
 */
@Slf4j
public class RedisDistributedCache<K extends Serializable, V extends Serializable> implements AbstractCache<K, V> {

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
}

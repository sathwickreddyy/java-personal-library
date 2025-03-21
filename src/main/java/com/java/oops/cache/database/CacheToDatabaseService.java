package com.java.oops.cache.database;

/**
 * Interface representing a basic persistent storage service.
 *
 * @param <K> Type of primary key used in storage operations
 * @param <V> Type of data stored/retrieved from storage
 */
public interface CacheToDatabaseService<K,V>{
    /**
     * Loads data from persistent storage based on provided key.
     *
     * @param key Key identifying the data to load
     * @return Data loaded from storage or null if not found
     */
    V load(K key);

    /**
     * Saves data into persistent storage with given key-value pair.
     *
     * @param key Primary identifier of the data
     * @param val Data value to save into persistent storage
     */
    void save(K key,V val);

    // @TODO

    /**
     * Bulk save data into persistent storage.
     */
    void bulkSave();
}

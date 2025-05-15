package com.java.oops.cache.types;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a single cache entry, holding the value and its expiry time (if any).
 *
 * @param <V> the type of value stored in the cache entry
 */
@Data
@Slf4j
public class CacheEntry<V> {
    /**
     * Special constant indicating this entry does not expire.
     */
    public static final long NO_EXPIRY = -1L;

    private V value;
    private long expiryInMillis;

    /**
     * Constructs a cache entry with a value and an explicit expiry time.
     *
     * @param value           the value to cache
     * @param expiryInMillis  the absolute expiry time in milliseconds since epoch,
     *                        or {@link #NO_EXPIRY} for no expiry
     */
    public CacheEntry(V value, long expiryInMillis) {
        this.value = value;
        this.expiryInMillis = expiryInMillis;
    }

    /**
     * Constructs a cache entry with no expiry.
     *
     * @param value the value to cache
     */
    public CacheEntry(V value) {
        this(value, NO_EXPIRY);
    }

    /**
     * Checks if this cache entry is expired.
     *
     * @return {@code true} if expired, {@code false} otherwise
     */
    public boolean isExpired() {
        boolean expired = expiryInMillis > 0 && System.currentTimeMillis() > expiryInMillis;
        log.debug("Checked expiry for value '{}': expiryInMillis={}, expired={}", value, expiryInMillis, expired);
        return expired;
    }
}

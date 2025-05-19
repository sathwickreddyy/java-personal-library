package com.java.oops.cache.types;

import com.java.oops.cache.eviction.LRUEvictionPolicy;
import com.java.oops.cache.types.ttl.CacheEntry;
import com.java.oops.cache.types.ttl.InMemoryTTLCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class InMemoryTTLCacheTest {

    private InMemoryTTLCache<String, String> cache;

    @BeforeEach
    public void setUp() {
        cache = new InMemoryTTLCache<>(new LRUEvictionPolicy<>(3), 3, Duration.ofSeconds(5));
    }

    @AfterEach
    public void tearDown() {
        cache.close();
    }

    @Test
    public void testPutAndGetNoExpiry() {
        cache.put("key1", "value1");
        Optional<String> result = cache.get("key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    public void testPutAndGetWithExpiry() throws InterruptedException {
        cache.put("key2", "value2", Duration.ofMillis(500));
        Optional<String> result = cache.get("key2");
        assertTrue(result.isPresent());
        assertEquals("value2", result.get());
        Thread.sleep(600); // Wait for expiry
        result = cache.get("key2");
        assertFalse(result.isPresent());
    }

    @Test
    public void testExpiryRemovesEntry() throws InterruptedException {
        cache.put("key3", "value3", Duration.ofMillis(100));
        Thread.sleep(150); // Wait for expiry
        Optional<String> result = cache.get("key3");
        assertFalse(result.isPresent());
    }

    @Test
    public void testAutoExpiry() throws InterruptedException {
        cache.put("key4", "value4", Duration.ofSeconds(2));
        Map<String, CacheEntry<String>> entries = cache.getCache(); // get all entries>
        assertEquals(1, entries.size());
        assertTrue(entries.containsKey("key4"));
        Thread.sleep(Duration.ofSeconds(6).toMillis()); // every 5 seconds thread will check for expired entries
        assertFalse(entries.containsKey("key4"));
    }

    @Test
    public void testEvictionPolicyEviction() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        // Cache is full now, next put should evict least recently used (key1)
        cache.put("key4", "value4");
        assertFalse(cache.get("key1").isPresent());
        assertTrue(cache.get("key2").isPresent());
        assertTrue(cache.get("key3").isPresent());
        assertTrue(cache.get("key4").isPresent());
    }

    @Test
    public void testManualEvict() {
        cache.put("key1", "value1");
        cache.evict("key1");
        assertFalse(cache.get("key1").isPresent());
    }
}

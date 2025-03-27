package com.java.oops.cache.client;

import com.java.oops.cache.eviction.EvictionPolicy;
import com.java.oops.cache.eviction.FIFOEvictionPolicy;
import com.java.oops.cache.eviction.LFUEvictionPolicy;
import com.java.oops.cache.eviction.LRUEvictionPolicy;
import com.java.oops.cache.types.InMemoryCache;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

/**
 * Test Class
 */
class InMemoryCachePerformanceTest {
    /**
     * Test Runner
     */
    private static final Logger logger = LoggerFactory.getLogger(InMemoryCachePerformanceTest.class);

    /**
     * Run the cache performance test
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        int capacity = 300;
        int numOfRequests = 50000;

        logger.info("\nInitializing cache performance test with capacity: {} and number of requests: {}\n", capacity, numOfRequests);

        int[] requests = randomRequestsGenerator(numOfRequests, 900);
//        int[] requests = skewedRequestsGenerator(numOfRequests, 450, 1500);
//        int[] requests = zipFlanRequestsGenerator(numOfRequests,  1500);


        logger.info("Generated {} random requests with keys ranging from 0 to 7.", numOfRequests);
//        logger.info("Requests are: {}\n\n", requests);

        // Test with LRU, LFU, FIFO policies
        testCachePerformance(new LRUEvictionPolicy<>(capacity), capacity, requests, "LRU");
        testCachePerformance(new LFUEvictionPolicy<>(), capacity, requests, "LFU");
        testCachePerformance(new FIFOEvictionPolicy<>(), capacity, requests, "FIFO");
    }

    private static void testCachePerformance(EvictionPolicy<Integer> policy, int capacity, int[] requests, String policyName) {
        InMemoryCache<Integer, String> cache = new InMemoryCache<>(policy, capacity);

        logger.info("Testing cache performance with {} eviction policy.", policyName);

        for (int key : requests) {
            // All requests are get requests
            Optional<String> result = cache.get(key);
            if (result.isEmpty()) {
                cache.put(key, "Value_" + key);
//                logger.info("Cache Miss:- PUT {} ; Cache Map {}", key, cache.getCache().keySet());
            }
//            else {
//                logger.info("Cache Hit:- GET {} ; Cache Map {}", key, cache.getCache().keySet());
//            }
        }

        double hitRate = ((double) cache.getHitCount() / requests.length) * 100;
        double missRate = ((double) cache.getMissCount() / requests.length) * 100;

        logger.info("Results for {} eviction policy:", policyName);
        logger.info("Hit Rate: {}", hitRate);
        logger.info("Miss Rate: {}\n", missRate);
    }

    private static int[] randomRequestsGenerator(int numOfRequests, int keySpace) {
        Random random = new SecureRandom();
        int[] requests = new int[numOfRequests];

        for (int i = 0; i < numOfRequests; i++) {
            int key = random.nextInt(keySpace)+1; // Simulate a larger keyspace than cache capacity
            requests[i] = key;
        }

        return requests;
    }

    private static int[] skewedRequestsGenerator(int numOfRequests, int smallKeySpace, int largeKeySpace) {
        Random random = new Random();
        int[] requests = new int[numOfRequests];

        for (int i = 0; i < numOfRequests; i++) {
            if (random.nextInt(100) < 83) {
                // 83% of the time, pick a key from a small "hot" set (0 to 300)
                requests[i] = random.nextInt(smallKeySpace) + 1;
            } else {
                // 17% of the time, pick a key from the full range (0 to 1500)
                requests[i] = random.nextInt(largeKeySpace) + 1;
            }
        }

        return requests;
    }

    private static int[] zipFlanRequestsGenerator(int numOfRequests, int keySpace) {
        int[] requests = new int[numOfRequests];
        ZipfDistribution zipf = new ZipfDistribution(keySpace, 1.1); // 1.2 is the skew factor
        for (int i = 0; i < numOfRequests; i++) {
            requests[i] = zipf.sample();
        }
        return requests;
    }
}


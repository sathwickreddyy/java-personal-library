package com.java.oops.cache.types.threadsafe;

import com.java.oops.cache.types.AbstractCache;
import com.java.oops.cache.types.InMemoryCache;
import com.java.oops.cache.eviction.LRUEvictionPolicy;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test runner to benchmark performance of different cache implementations
 * with a focus on write-heavy workloads.
 */
public class ThreadSafeCacheBenchmarkRunner {

    private static final int NUM_THREADS = 32;
    private static final int OPERATIONS_PER_THREAD = 100000;
    private static final int WRITE_PERCENTAGE = 80; // 80% writes for write-heavy workload
    private static final int CACHE_SIZE = 10000;

    public static void main(String[] args) throws InterruptedException {
        // Create different thread-safe wrappers
        AbstractCache<String, String> readWriteCache = new ReadHeavyThreadSafeCache<>(new InMemoryCache<>(new LRUEvictionPolicy<>(CACHE_SIZE), CACHE_SIZE));
        AbstractCache<String, String> writeHeavyCache = new WriteHeavyThreadSafeCache<>(new InMemoryCache<>(new LRUEvictionPolicy<>(CACHE_SIZE), CACHE_SIZE), 1024);

        System.out.println("Starting cache benchmark with " + NUM_THREADS +
                " threads, " + OPERATIONS_PER_THREAD + " operations per thread, " +
                WRITE_PERCENTAGE + "% writes");

        // Benchmark each cache implementation
        System.out.println("\nTesting ReadWriteCache (optimized for read-heavy workloads):");
        runBenchmark(readWriteCache, NUM_THREADS, OPERATIONS_PER_THREAD, WRITE_PERCENTAGE);

        System.out.println("\nTesting WriteHeavyCache (optimized for write-heavy workloads):");
        runBenchmark(writeHeavyCache, NUM_THREADS, OPERATIONS_PER_THREAD, WRITE_PERCENTAGE);
    }

    private static void runBenchmark(AbstractCache<String, String> cache,
                                     int numThreads,
                                     int operationsPerThread,
                                     int writePercentage) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        Random random = new Random();

        // Prefill cache with some data
        for (int i = 0; i < CACHE_SIZE / 2; i++) {
            cache.put("key" + i, "value" + i);
        }

        long startTime = System.currentTimeMillis();

        // Start worker threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            String key = "key" + (random.nextInt(CACHE_SIZE * 2));

                            // Determine if this operation should be a write
                            if (random.nextInt(100) < writePercentage) {
                                // Write operation
                                try {
                                    cache.put(key, "value-" + threadId + "-" + j);
                                } catch (NullPointerException npe) {
                                    System.err.println("NPE during put operation - Thread ID: " + threadId +
                                            ", Key: " + key + ", Iteration: " + j);
                                    System.err.println("Cache instance: " + (cache == null ? "null" : cache.getClass().getName()));
                                    npe.printStackTrace();
                                }
                            } else {
                                // Read operation
                                try {
                                    Optional<String> result = cache.get(key);
                                    if (result.isPresent()) {
                                        // Value found
                                    }
                                } catch (NullPointerException npe) {
                                    System.err.println("NPE during get operation - Thread ID: " + threadId +
                                            ", Key: " + key + ", Iteration: " + j);
                                    System.err.println("Cache instance: " + (cache == null ? "null" : cache.getClass().getName()));
                                    npe.printStackTrace();
                                }
                            }
                            successfulOperations.incrementAndGet();
                        } catch (Exception e) {
                            System.err.println("Error in thread " + threadId + " at iteration " + j + ": " + e.getMessage());
                            System.err.println("Error class: " + e.getClass().getName());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Critical error in thread " + threadId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }


        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Completed " + successfulOperations.get() + " operations in " + duration + " ms");
        System.out.println("Throughput: " + (successfulOperations.get() * 1000.0 / duration) + " operations/second");
    }

    /**
     * Runs a more detailed analysis comparing different concurrency levels for WriteHeavyCache
     */
    public static void concurrencyLevelAnalysis() throws InterruptedException {
        AbstractCache<String, String> baseCache = new InMemoryCache<>(new LRUEvictionPolicy<>(CACHE_SIZE), CACHE_SIZE);
        List<Integer> concurrencyLevels = List.of(1, 4, 8, 16, 32, 64, 128);

        System.out.println("Analyzing WriteHeavyCache performance with different concurrency levels");
        System.out.println("Write percentage: " + WRITE_PERCENTAGE + "%");
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Operations per thread: " + OPERATIONS_PER_THREAD);
        System.out.println("-------------------------------------------------------");
        System.out.println("Concurrency Level | Throughput (ops/sec) | Duration (ms)");
        System.out.println("-------------------------------------------------------");

        for (int level : concurrencyLevels) {
            AbstractCache<String, String> writeHeavyCache = new WriteHeavyThreadSafeCache<>(baseCache, level);

            // Run benchmark and collect results
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            CountDownLatch latch = new CountDownLatch(NUM_THREADS);
            AtomicInteger successfulOperations = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            // Start worker threads (simplified for brevity)
            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        Random random = new Random();
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            String key = "key" + (random.nextInt(CACHE_SIZE * 2));
                            if (random.nextInt(100) < WRITE_PERCENTAGE) {
                                writeHeavyCache.put(key, "value-" + j);
                            } else {
                                writeHeavyCache.get(key);
                            }
                            successfulOperations.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double throughput = successfulOperations.get() * 1000.0 / duration;

            System.out.printf("%-17d | %-21.2f | %-12d%n", level, throughput, duration);
        }
    }
}

/*
-----------------------------------------------------------------------------------
Starting cache benchmark with 32 threads, 100000 operations per thread, 80% writes

Testing ReadWriteCache (optimized for read-heavy workloads):
Completed 3200000 operations in 1021 ms
Throughput: 3134182.1743388833 operations/second

Testing WriteHeavyCache (optimized for write-heavy workloads):
Completed 3200000 operations in 692 ms
Throughput: 4624277.456647399 operations/second

BUILD SUCCESSFUL in 2s
3 actionable tasks: 2 executed, 1 up-to-date
-----------------------------------------------------------------------------------


Below is the detailed analysis of the performance comparison between ReadWriteCache and WriteHeavyCache implementations:

## Performance Analysis

The benchmark was conducted with:
- 32 concurrent threads
- 100,000 operations per thread (3.2 million total operations)
- 80% write operations (write-heavy workload)

### Key Results:
1. **ReadWriteCache Performance**:
   - Completed 3.2 million operations in 1021 ms
   - Throughput: ~3.13 million operations/second

2. **WriteHeavyCache Performance**:
   - Completed 3.2 million operations in 692 ms
   - Throughput: ~4.62 million operations/second

### Comparative Analysis:
- **WriteHeavyCache outperformed ReadWriteCache by approximately 47.5%** in this write-heavy scenario
- The WriteHeavyCache completed the same workload 329 ms faster (1021 ms vs 692 ms)
- The throughput difference was about 1.49 million operations/second in favor of WriteHeavyCache

### Technical Explanation:
The significant performance advantage of WriteHeavyCache can be attributed to its striped locking strategy,
which divides the key space into multiple segments with separate locks. This approach:

1. Reduces lock contention by allowing concurrent writes to different segments
2. Minimizes thread blocking since only operations on the same key segment compete for locks
3. Scales better with increased thread count in write-heavy scenarios

In contrast, ReadWriteCache uses a single ReadWriteLock for the entire cache, which:
1. Allows multiple concurrent reads (good for read-heavy workloads)
2. But forces all write operations to be serialized (problematic for write-heavy workloads)

This benchmark clearly demonstrates that the architectural choice of using fine-grained locking in WriteHeavyCache
provides substantial benefits for write-intensive workloads, validating the design decision to create specialized
cache implementations for different access patterns.
*/
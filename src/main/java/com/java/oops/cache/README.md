Here's an end-to-end implementation of the provided caching architecture, including proper design patterns and comprehensive documentation:

<img src="https://github.com/sathwickreddyy/java-personal-library/blob/main/src/main/java/com/java/oops/cache/architecture.png" alt="Cache Implementation"/>

## Implementation Overview

This implementation demonstrates a modular caching system using Java, leveraging design patterns to ensure flexibility, scalability, and maintainability. The architecture includes:

- **Caching Strategies**: Cache-Aside, Read-Through, Write-Through, Write-Behind
- **Cache Abstraction**: Abstract Cache with implementations (In-Memory, Distributed, Null-Safe)
- **Eviction Policies**: LRU, LFU, FIFO
- **Distributed Cache Providers**: Redis (example)
- **Null-Safe Cache**: Optional Wrapper and Null Object Pattern


## Project Structure

```
src/
├── cache
│   ├── AbstractCache.java
│   ├── InMemoryCache.java
│   ├── DistributedCache.java
│   └── NullSafeCache.java
├── strategy
│   ├── CacheStrategy.java
│   ├── CacheAsideStrategy.java
│   ├── ReadThroughStrategy.java
│   ├── WriteThroughStrategy.java
│   └── WriteBehindStrategy.java
├── eviction
│   ├── EvictionPolicy.java
│   ├── LRUEvictionPolicy.java
│   ├── LFUEvictionPolicy.java
│   └── FIFOEvictionPolicy.java
└── client
    └── CacheClient.java
```

---

## Core Design Patterns Used:

- **Strategy Pattern**: For switching between caching strategies dynamically.
- **Factory Method Pattern**: To instantiate different cache implementations.
- **Decorator Pattern**: For adding null-safe behavior to caches.
- **Null Object Pattern**: To handle null values gracefully.

---

## Implementation Details:

### 1. Abstract Cache Interface (`AbstractCache`):

```java
public interface AbstractCache<K, V> {
    void put(K key, V value);
    Optional<V> get(K key);
    void evict(K key);
}
```

---

### In-Memory Cache Implementation (with Eviction Policy):

```java
public class InMemoryCache<K,V> implements AbstractCache<K,V> {
    private final Map<K,V> cache;
    private final EvictionPolicy<K> evictionPolicy;

    public InMemoryCache(EvictionPolicy<K> evictionPolicy) {
        this.cache = new ConcurrentHashMap<>();
        this.evictionPolicy = evictionPolicy;
    }
    
    @Override
    public void put(K key, V value) {
        if(cache.size() >= MAX_SIZE) {
            K evictKey = evictionPolicy.evict();
            cache.remove(evictKey);
        }
        cache.put(key, value);
        evictionPolicy.recordAccess(key);
    }
    
    @Override
    public Optional<V> get(K key) {
        if(cache.containsKey(key)) {
            evictionPolicy.recordAccess(key);
            return Optional.ofNullable(cache.get(key));
        }
        return Optional.empty();
    }
}
```

---

### Distributed Cache Implementation (Redis Example):

```java
public class RedisDistributedCache<K extends Serializable, V extends Serializable> implements AbstractCache<K, V> {

    private final Jedis redisClient;

    public RedisDistributedCache(Jedis redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public void put(K key, V value) {
        try {
            byte[] serializedKey = serialize(key);
            byte[] serializedValue = serialize(value);
            redisClient.set(serializedKey, serializedValue);
        } catch (IOException | JedisException e) {
            // handle exception
        }
    }

    @Override
    public Optional<V> get(K key) {
        try {
            byte[] serializedKey = serialize(key);
            byte[] result = redisClient.get(serializedKey);
            if (result == null) {
                return Optional.empty();
            }
            V value = deserialize(result);
            return Optional.ofNullable(value);
        } catch (IOException | ClassNotFoundException | JedisException e) {
            return Optional.empty();
        }
    }

    @Override
    public void evict(K key) {
        try {
            byte[] serializedKey = serialize(key);
            redisClient.del(serializedKey);
        } catch (IOException | JedisException e) {
            // handle exception
        }
    }
    
    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    private V deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (V) in.readObject();
        }
    }
}
```

### Null-Safe Cache (using Null Object Pattern):

```java
public class NullSafeCache<K,V> implements AbstractCache<K,V> {
    private final AbstractCache<K,V> delegate;
    
    public NullSafeCache(AbstractCache<K,V> delegate){
        this.delegate = delegate;
    }
    
    @Override
    public void put(K key, V value){
        if(value != null){
            delegate.put(key,value);
        }
    }
    
    @Override
    public Optional<V> get(K key){
        return delegate.get(key); // returns Optional.empty if not found or null-safe wrapper 
    }

    @Override
    public void evict(K key) {
        delegate.evict(key);
    }
}
```

---

## Caching Strategies (Strategy Pattern):

```java
public interface CacheStrategy<K,V>{
    V read(K key);
    void write(K key, V value);
}

// Example: Cache Aside Strategy Implementation:
public class CacheAsideStrategy<K,V> implements CacheStrategy<K,V>{

    private final AbstractCache<K,V> cache;
    private final DatabaseService<K,V> db;

    public CacheAsideStrategy(AbstractCache<K,V> cache, DatabaseService<K,V> db){
        this.cache = cache;
        this.db = db;
    }

    @Override
    public V read(K key){
        return cache.get(key).orElseGet(() -> {
            V data = db.load(key);
            cache.put(key,data);
            return data;
        });
    }

    @Override
    public void write(K key, V value){
        db.save(key,value);
        cache.evict(key); // Invalidate cache entry after write.
    }
}
```

## Eviction Policies:

Implemented using Strategy pattern to allow dynamic policy selection:
-	Least Recently Used (LRU)
-	Least Frequently Used (LFU)
-	First-In First-Out (FIFO)

Example LRU implementation:
```java
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashSet<K> accessOrder;

    public LRUEvictionPolicy() {
        this.accessOrder = new LinkedHashSet<>();
    }

    @Override
    public void recordAccess(K key) {
        accessOrder.remove(key); // Remove if already exists
        accessOrder.add(key);    // Reinsert at the end
    }

    @Override
    public K evict() {
        if (accessOrder.isEmpty()) {
            throw new IllegalStateException("Cache is empty, cannot evict");
        }
        K firstKey = accessOrder.iterator().next();
        accessOrder.remove(firstKey);
        return firstKey;
    }
}
```
---

## Example Client Usage:

```java
public class CacheClient {
    public static void main(String[] args){
        AbstractCache<String,String> inMemory = new InMemoryCache<>(new LRUEvictionPolicy<>(100));
        AbstractCache<String,String> distributed = new DistributedCache<>(new RedisClient("localhost"));
        AbstractCache<String,String> nullSafe = new NullSafeCache<>(distributed);
    
        CacheStrategy<String,String> strategy = new CacheAsideStrategy<>(distributed, new DatabaseService<>());
    
        String data = strategy.read("user123");
        strategy.write("user123", "John Doe");
    
        System.out.println(data); // Output retrieved data.
    }
}
```

---

## Caching Strategies Explained:

| Strategy       | Description                                    | Use Case              |
|---------------|--------------------------------|-----------------------|
| Cache Aside    | Lazy loading; fetches data on-demand            | Simple apps           |
| Read Through   | Reads always via cache (cache populates itself) | High read throughput  |
| Write Through  | Writes immediately to DB via the cache          | Consistency critical apps |
| Write Behind   | Async writes to DB; high throughput             | High-throughput apps  |

---

This comprehensive implementation provides a clear example of applying design patterns effectively to build a scalable and maintainable caching system.

## References

- Arch Diagram as a Code - [Mermaid Live Editor](https://mermaid.live/)
```
graph TD
    C[Cache LLD] --> D{Caching Strategy}
    D --> E[Cache-Aside]
    D --> F[Read-Through]
    D --> G[Write-Through]
    D --> H[Write-Behind]
    
    C --> I[Abstract Cache]
    I --> J[In-Memory Cache]
    I --> K[Distributed Cache]
    I --> L[Null-Safe Cache]
    I --> I2[Abstract TTL Cache]
    I2 --> J2[In-Memory TTL Cache]
    
    J --> M[Eviction Policies]
    J2 --> M
    M --> N[LRU]
    M --> O[LFU]
    M --> P[FIFO]
    
    K --> Q[Hazelcast]
    K --> R[Redis]
    K --> S[Memcached]
    
    L --> T[Optional Wrapper]
    L --> U[Null Object Pattern]
```

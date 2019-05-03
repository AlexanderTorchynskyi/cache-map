import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheMap<K, V> implements Cache<K, V> {
    
    private static final long DEFAULT_EXPIRE_TIME = 1_000L;
    private static final int DEFAULT_CLEAN_UP_TIME = 5;
    
    private long expireTime;
    
    private final ConcurrentHashMap<K, SoftReference<CacheObject<V>>> cache = new ConcurrentHashMap<>();
    
    public CacheMap(long expireTime, int cleanUpTime) {
        this.expireTime = expireTime;
        postConstructMethod(cleanUpTime);
    }
    
    private void postConstructMethod(int cleanUpTime) {
        Thread cleanerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.SECONDS.sleep(cleanUpTime);
                    cache.entrySet().removeIf(entry -> Optional.ofNullable(entry.getValue()).map(SoftReference::get).map(CacheObject::isExpired).orElse(false));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }
    
    public CacheMap(long expireTime) {
        this(expireTime, DEFAULT_CLEAN_UP_TIME);
    }
    
    public CacheMap() {
        this(DEFAULT_EXPIRE_TIME, DEFAULT_CLEAN_UP_TIME);
    }
    
    @Override
    public void add(K key, V value) {
        this.add(key, value, expireTime);
    }
    
    @Override
    public void add(K key, V value, long periodInMillis) {
        if (Objects.isNull(key)) {
            return;
        }
        if (Objects.isNull(value)) {
            cache.remove(key);
        } else {
            long expiryTime = System.currentTimeMillis() + periodInMillis;
            cache.put(key, new SoftReference<>(new CacheObject<>(value, expiryTime)));
        }
    }
    
    @Override
    public void remove(K key) {
        cache.remove(key);
    }
    
    @Override
    public V get(K key) {
        return Optional.ofNullable(cache.get(key)).map(SoftReference::get).filter(cacheObject -> !cacheObject.isExpired()).map(CacheObject::getValue).orElse(null);
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public long size() {
        return cache.size();
    }
    
    private static class CacheObject<V> {
        
        private V value;
        private long expiryTime;
        
        CacheObject(V value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        V getValue() {
            return value;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
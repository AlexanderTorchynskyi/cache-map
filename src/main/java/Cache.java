public interface Cache<K, V> {
    
    void add(K key, V value);
    
    void add(K key, V value, long periodInMillis);
    
    void remove(K key);
    
    V get(K key);
    
    void clear();
    
    long size();
}

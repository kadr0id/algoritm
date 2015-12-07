/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 *
 * @author macbookpro
 */
public class ExtConcurrentHashMap<K1, K2, V> extends NonBlockingHashMap<CompositeHashKey<K1, K2>, V> {

    public ExtConcurrentHashMap() {
        super();
    }

    public ExtConcurrentHashMap(int initSize) {
        super(initSize);
    }

    public synchronized void put(K1 key1, K2 key2, V value) {
        CompositeHashKey<K1, K2> key = new CompositeHashKey<K1, K2>(key1, key2);
        this.put(key, value);
    }

    public V get(K1 key1, K2 key2) {
        CompositeHashKey<K1, K2> key = new CompositeHashKey<K1, K2>(key1, key2);
        return this.get(key);
    }

    public boolean containsKey(K1 key1, K2 key2) {
        CompositeHashKey<K1, K2> key = new CompositeHashKey<K1, K2>(key1, key2);
        return this.containsKey(key);
    }

    public void delete(K1 key1, K2 key2) {
        CompositeHashKey<K1, K2> key = new CompositeHashKey<K1, K2>(key1, key2);
        this.remove(key);
    }
}

package de.invesdwin.util.collections.loadingcache.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import de.invesdwin.util.collections.loadingcache.ILoadingCache;

@ThreadSafe
public abstract class ASynchronizedLoadingCache<K, V> implements ILoadingCache<K, V> {

    @GuardedBy("this")
    private final Map<K, V> map;
    private final Function<K, V> loadValue;

    public ASynchronizedLoadingCache(final Function<K, V> loadValue, final Map<K, V> map) {
        this.loadValue = loadValue;
        this.map = map;
    }

    @Override
    public V get(final K key) {
        V v;
        synchronized (this) {
            v = map.get(key);
        }
        if (v == null) {
            //bad idea to synchronize in apply, this might cause deadlocks
            v = loadValue.apply(key);
            synchronized (this) {
                map.put(key, v);
            }
        }
        return v;
    }

    @Override
    public synchronized void clear() {
        map.clear();
    }

    @Override
    public synchronized boolean containsKey(final K key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized void remove(final K key) {
        map.remove(key);
    }

    @Override
    public synchronized void put(final K key, final V value) {
        map.put(key, value);
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return map.values();
    }

}

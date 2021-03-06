package de.invesdwin.util.collections.loadingcache.internal;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.concurrent.ThreadSafe;

import de.invesdwin.util.collections.loadingcache.ILoadingCache;
import de.invesdwin.util.collections.loadingcache.guava.AGuavaLoadingCacheMap;
import de.invesdwin.util.collections.loadingcache.guava.GuavaLoadingCacheMapConfig;

@ThreadSafe
public class GuavaLoadingCache<K, V> implements ILoadingCache<K, V> {

    private final AGuavaLoadingCacheMap<K, V> delegate = new AGuavaLoadingCacheMap<K, V>() {
        @Override
        protected final V loadValue(final K key) {
            return loadValue.apply(key);
        }

        @Override
        protected GuavaLoadingCacheMapConfig getConfig() {
            return GuavaLoadingCache.this.getConfig();
        }
    };
    private final Function<K, V> loadValue;
    private final Integer maximumSize;

    public GuavaLoadingCache(final Function<K, V> loadValue, final Integer maximumSize) {
        this.loadValue = loadValue;
        this.maximumSize = maximumSize;
    }

    protected GuavaLoadingCacheMapConfig getConfig() {
        return new GuavaLoadingCacheMapConfig().withMaximumSize(maximumSize);
    }

    @Override
    public V get(final K key) {
        return delegate.get(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(final K key) {
        return delegate.containsKey(key);
    }

    @Override
    public void remove(final K key) {
        delegate.remove(key);
    }

    @Override
    public void put(final K key, final V value) {
        delegate.put(key, value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

}

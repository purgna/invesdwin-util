package de.invesdwin.util.collections.loadingcache.historical;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import javax.annotation.concurrent.ThreadSafe;

import org.assertj.core.description.TextDescription;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.collections.loadingcache.ADelegateLoadingCache;
import de.invesdwin.util.collections.loadingcache.ALoadingCache;
import de.invesdwin.util.collections.loadingcache.ILoadingCache;
import de.invesdwin.util.collections.loadingcache.historical.interceptor.HistoricalCacheQueryInterceptorSupport;
import de.invesdwin.util.collections.loadingcache.historical.interceptor.IHistoricalCacheQueryInterceptor;
import de.invesdwin.util.time.fdate.FDate;

@ThreadSafe
public abstract class AHistoricalCache<V> {

    /**
     * This is normally sufficient for daily bars of stocks and also fast enough for intraday ticks to load.
     */
    public static final int DEFAULT_MAXIMUM_SIZE = 10000;

    private final Set<IHistoricalCacheListener<V>> listeners = new CopyOnWriteArraySet<IHistoricalCacheListener<V>>();

    private boolean isPutDisabled = getMaximumSize() != null && getMaximumSize() == 0;
    private AHistoricalCache<Object> shiftKeysDelegate;
    private AHistoricalCache<Object> extractKeysDelegate;
    private final ILoadingCache<FDate, V> valuesMap = new ADelegateLoadingCache<FDate, V>() {

        @Override
        public V get(final FDate key) {
            onGet();
            return super.get(key);
        }

        @Override
        protected ILoadingCache<FDate, V> createDelegate() {
            return newProvider(new Function<FDate, V>() {

                @Override
                public V apply(final FDate key) {
                    final V value = AHistoricalCache.this.loadValue(key);
                    if (value != null && !listeners.isEmpty()) {
                        for (final IHistoricalCacheListener<V> l : listeners) {
                            l.onValueLoaded(key, value);
                        }
                    }
                    return value;
                }

                private void onValueLoaded(final FDate key, final V value) {}

            }, getMaximumSize());
        }

    };

    private final ILoadingCache<FDate, FDate> previousKeysCache = new ADelegateLoadingCache<FDate, FDate>() {

        @Override
        public FDate get(final FDate key) {
            onGet();
            return super.get(key);
        }

        @Override
        public void put(final FDate key, final FDate value) {
            //don't cache null values to prevent moving time issues of the underlying source (e.g. JForexTickCache getNextValue)
            if (value != null && !key.equals(value)) {
                super.put(key, value);
            }
        }

        @Override
        protected ILoadingCache<FDate, FDate> createDelegate() {
            return newProvider(new Function<FDate, FDate>() {
                @Override
                public FDate apply(final FDate key) {
                    return innerCalculatePreviousKey(key);
                }
            }, getMaximumSize());
        }

    };

    private final ILoadingCache<FDate, FDate> nextKeysCache = new ADelegateLoadingCache<FDate, FDate>() {
        @Override
        public void put(final FDate key, final FDate value) {
            //don't cache null values to prevent moving time issues of the underlying source (e.g. JForexTickCache getNextValue)
            if (value != null && !key.equals(value)) {
                super.put(key, value);
            }
        }

        @Override
        public FDate get(final FDate key) {
            onGet();
            return super.get(key);
        }

        @Override
        protected ILoadingCache<FDate, FDate> createDelegate() {
            return newProvider(new Function<FDate, FDate>() {
                @Override
                public FDate apply(final FDate key) {
                    return innerCalculateNextKey(key);
                }
            }, getMaximumSize());
        }
    };

    /**
     * null means unlimited and 0 means no caching at all.
     */
    protected Integer getMaximumSize() {
        return DEFAULT_MAXIMUM_SIZE;
    }

    @SuppressWarnings("unchecked")
    protected void setShiftKeysDelegate(final AHistoricalCache<?> shiftKeysDelegate, final boolean extractKeys) {
        Assertions.assertThat(shiftKeysDelegate).as("Use null instead of this").isNotSameAs(this);
        Assertions.assertThat(this.shiftKeysDelegate).isNull();
        this.shiftKeysDelegate = (AHistoricalCache<Object>) shiftKeysDelegate;
        if (extractKeys) {
            this.extractKeysDelegate = this.shiftKeysDelegate;
        } else {
            this.extractKeysDelegate = null;
        }
        isPutDisabled = false;
    }

    protected void onGet() {}

    protected abstract V loadValue(FDate key);

    protected <T> ILoadingCache<FDate, T> newProvider(final Function<FDate, T> loadValue, final Integer maximumSize) {
        return new ALoadingCache<FDate, T>() {

            @Override
            protected Integer getMaximumSize() {
                return maximumSize;
            }

            @Override
            protected T loadValue(final FDate key) {
                return loadValue.apply(key);
            }

        };
    }

    /**
     * Should return the key if the value does not contain a key itself.
     */
    public final FDate extractKey(final FDate key, final V value) {
        if (extractKeysDelegate != null) {
            final Object shiftKeysValue = extractKeysDelegate.query().withFuture().getValue(key);
            return extractKeysDelegate.extractKey(key, shiftKeysValue);
        } else {
            return innerExtractKey(key, value);
        }
    }

    /**
     * This is only for internal purposes, use extractKey instead.
     */
    protected FDate innerExtractKey(final FDate key, final V value) {
        throw new UnsupportedOperationException();
    }

    protected final FDate calculatePreviousKey(final FDate key) {
        if (key == null) {
            throw new IllegalArgumentException("key should not be null!");
        }
        if (shiftKeysDelegate != null) {
            return shiftKeysDelegate.calculatePreviousKey(key);
        } else {
            return previousKeysCache.get(key);
        }
    }

    protected final FDate calculateNextKey(final FDate key) {
        if (key == null) {
            throw new IllegalArgumentException("key should not be null!");
        }
        if (shiftKeysDelegate != null) {
            return shiftKeysDelegate.calculateNextKey(key);
        } else {
            return nextKeysCache.get(key);
        }
    }

    /**
     * This is only for internal purposes, use calculatePreviousKey instead.
     */
    protected FDate innerCalculatePreviousKey(final FDate key) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is only for internal purposes, use calculateNextKey instead.
     */
    protected FDate innerCalculateNextKey(final FDate key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Does not allow values from future per default.
     */
    public final HistoricalCacheQuery<V> query() {
        return new HistoricalCacheQuery<V>(this, shiftKeysDelegate);
    }

    public final boolean containsKey(final FDate key) {
        return getValuesMap().containsKey(key);
    }

    public final void remove(final FDate key) {
        getValuesMap().remove(key);
        if (getPreviousKeysCache().containsKey(key)) {
            final FDate previousKey = getPreviousKeysCache().get(key);
            getPreviousKeysCache().remove(previousKey);
        }
        getPreviousKeysCache().remove(key);
        getNextKeysCache().remove(key);
    }

    private void putPrevAndNext(final FDate nextKey, final FDate valueKey, final V value, final FDate previousKey) {
        if (previousKey != null && nextKey != null) {
            if (!(previousKey.compareTo(nextKey) <= 0)) {
                throw new IllegalArgumentException(new TextDescription("previousKey [%s] <= nextKey [%s] not matched",
                        previousKey, nextKey).toString());
            }
        }
        getValuesMap().put(valueKey, value);
        if (previousKey != null) {
            putPrevious(previousKey, value, valueKey);
        }
        if (nextKey != null) {
            putNext(nextKey, value, valueKey);
        }
    }

    public final void put(final FDate newKey, final V newValue, final FDate prevKey, final V prevValue) {
        if (isPutDisabled) {
            return;
        }
        if (newValue != null) {
            if (prevValue != null) {
                putPrevAndNext(newKey, prevKey, prevValue, null);
                putPrevAndNext(null, newKey, newValue, prevKey);
            } else {
                putPrevAndNext(null, newKey, newValue, null);
            }
        }
    }

    public final void put(final V newValue, final V prevValue) {
        if (isPutDisabled) {
            return;
        }
        if (newValue != null) {
            final FDate newKey = extractKey(null, newValue);
            if (prevValue != null) {
                final FDate prevKey = extractKey(null, prevValue);
                putPrevAndNext(newKey, prevKey, prevValue, null);
                putPrevAndNext(null, newKey, newValue, prevKey);
            } else {
                putPrevAndNext(null, newKey, newValue, null);
            }
        }
    }

    public final void put(final Entry<FDate, V> newEntry, final Entry<FDate, V> prevEntry) {
        if (isPutDisabled) {
            return;
        }
        if (newEntry != null) {
            final V newValue = newEntry.getValue();
            if (newValue != null) {
                final FDate newKey = newEntry.getKey();
                if (prevEntry != null) {
                    final FDate prevKey = prevEntry.getKey();
                    final V prevValue = prevEntry.getValue();
                    putPrevAndNext(newKey, prevKey, prevValue, null);
                    putPrevAndNext(null, newKey, newValue, prevKey);
                } else {
                    putPrevAndNext(null, newKey, newValue, null);
                }
            }
        }
    }

    private void putPrevious(final FDate previousKey, final V value, final FDate valueKey) {
        final int compare = previousKey.compareTo(valueKey);
        if (!(compare <= 0)) {
            throw new IllegalArgumentException(new TextDescription("previousKey [%s] <= value [%s] not matched",
                    previousKey, valueKey).toString());
        }
        if (compare != 0) {
            getPreviousKeysCache().put(valueKey, previousKey);
            getNextKeysCache().put(previousKey, valueKey);
        }
    }

    private void putNext(final FDate nextKey, final V value, final FDate valueKey) {
        final int compare = nextKey.compareTo(valueKey);
        if (!(compare >= 0)) {
            throw new IllegalArgumentException(new TextDescription("nextKey [%s] >= value [%s] not matched", nextKey,
                    valueKey).toString());
        }
        if (compare != 0) {
            getNextKeysCache().put(valueKey, nextKey);
            getPreviousKeysCache().put(nextKey, valueKey);
        }
    }

    public void clear() {
        valuesMap.clear();
        previousKeysCache.clear();
        nextKeysCache.clear();
        if (shiftKeysDelegate != null) {
            shiftKeysDelegate.clear();
        }
    }

    protected IHistoricalCacheQueryInterceptor<V> getQueryInterceptor() {
        return new HistoricalCacheQueryInterceptorSupport<V>();
    }

    public Set<IHistoricalCacheListener<V>> getListeners() {
        return listeners;
    }

    private ILoadingCache<FDate, FDate> getPreviousKeysCache() {
        if (shiftKeysDelegate != null) {
            return shiftKeysDelegate.getPreviousKeysCache();
        } else {
            return previousKeysCache;
        }
    }

    private ILoadingCache<FDate, FDate> getNextKeysCache() {
        if (shiftKeysDelegate != null) {
            return shiftKeysDelegate.getNextKeysCache();
        } else {
            return nextKeysCache;
        }
    }

    ILoadingCache<FDate, V> getValuesMap() {
        return valuesMap;
    }

    protected final FDate minKey() {
        return FDate.MIN_DATE;
    }

    protected final FDate maxKey() {
        return FDate.MAX_DATE;
    }

}

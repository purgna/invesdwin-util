package de.invesdwin.util.collections.loadingcache.historical;

import java.util.Map.Entry;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.collections.ImmutableEntry;
import de.invesdwin.util.time.fdate.FDate;

@Immutable
enum HistoricalCacheAssertValue {
    ASSERT_VALUE_WITH_FUTURE() {
        @Override
        public <V> Entry<FDate, V> internalAssertValue(final AHistoricalCache<V> parent, final FDate key,
                final FDate valueKey, final V value) {
            FDate assertedValueKey = null;
            if (value != null) {
                assertedValueKey = parent.extractKey(valueKey, value);
            }
            if (assertedValueKey == null) {
                assertedValueKey = valueKey;
            }
            if (assertedValueKey == null) {
                assertedValueKey = key;
            }
            return ImmutableEntry.of(assertedValueKey, value);
        }

    },
    ASSERT_VALUE_WITH_FUTURE_NULL() {
        @Override
        public <V> Entry<FDate, V> internalAssertValue(final AHistoricalCache<V> parent, final FDate key,
                final FDate valueKey, final V value) {
            FDate assertedValueKey = null;
            if (value != null) {
                assertedValueKey = parent.extractKey(valueKey, value);
                if (assertedValueKey.compareTo(key) >= 1) {
                    return null;
                }
            }
            if (assertedValueKey == null) {
                assertedValueKey = valueKey;
            }
            if (assertedValueKey == null) {
                assertedValueKey = key;
            }
            return ImmutableEntry.of(assertedValueKey, value);
        }
    },
    ASSERT_VALUE_WITHOUT_FUTURE() {
        @Override
        public <V> Entry<FDate, V> internalAssertValue(final AHistoricalCache<V> parent, final FDate key,
                final FDate valueKey, final V value) {
            FDate assertedValueKey = null;
            if (value != null) {
                assertedValueKey = parent.extractKey(valueKey, value);
                if (assertedValueKey.compareTo(key) >= 1) {
                    throw new IllegalArgumentException("Value key [" + assertedValueKey + "] is after requested key ["
                            + key + "]. Thus it comes from the future, which is not allowed!");
                }
            }
            if (assertedValueKey == null) {
                assertedValueKey = valueKey;
            }
            if (assertedValueKey == null) {
                assertedValueKey = key;
            }
            return ImmutableEntry.of(assertedValueKey, value);
        }
    };

    protected abstract <V> Entry<FDate, V> internalAssertValue(AHistoricalCache<V> parent, FDate key, FDate valueKey,
            V value);

    public final <V> Entry<FDate, V> assertValue(final AHistoricalCache<V> parent, final FDate key,
            final FDate valueKey, final V value) {
        final Entry<FDate, V> assertedValue = internalAssertValue(parent, key, valueKey, value);
        if (assertedValue == null || assertedValue.getValue() == null) {
            return null;
        } else {
            return assertedValue;
        }
    }

    public static final <V> V unwrapEntry(final Entry<FDate, V> entry) {
        if (entry == null) {
            return (V) null;
        } else {
            return entry.getValue();
        }
    }
}

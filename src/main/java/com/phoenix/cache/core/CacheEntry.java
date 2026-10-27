package com.phoenix.cache.core;

import java.time.Instant;

/**
 * Represents a cache entry stored in {@link CacheEngine}. Each entry contains the cached value,
 * its absolute expiration time (epoch millis) and a version that can be used for future
 * conflict‑resolution logic.
 *
 * <p>Day 2 only requires basic TTL handling, so the version field is initialized to {@code 0}
 * and incremented on each {@link CacheEngine#put(Object, Object, long)} operation.
 */
public class CacheEntry<V> {
    private final V value;
    private final long expiryEpochMilli;
    private final long version;

    public CacheEntry(V value, long expiryEpochMilli, long version) {
        this.value = value;
        this.expiryEpochMilli = expiryEpochMilli;
        this.version = version;
    }

    public V getValue() {
        return value;
    }

    public long getExpiryEpochMilli() {
        return expiryEpochMilli;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Returns {@code true} if the entry is considered expired at the supplied reference time.
     */
    public boolean isExpired(long nowEpochMilli) {
        return nowEpochMilli >= expiryEpochMilli;
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "value=" + value +
                ", expiry=" + Instant.ofEpochMilli(expiryEpochMilli) +
                ", version=" + version +
                '}';
    }
}

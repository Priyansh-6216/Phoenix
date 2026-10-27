package com.phoenix.cache.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread‑safe in‑memory cache engine used for Day 2.
 *
 * <p>Features:
 * <ul>
 *   <li>CRUD operations with optional TTL (time‑to‑live) in milliseconds.</li>
 *   <li>Lazy expiration – entries are checked for expiry on {@code get}/{@code exists} and removed.
 *   <li>Scheduled background cleanup that removes a bounded number of expired entries each run.
 * </ul>
 *
 * <p>The engine stores {@link CacheEntry} objects keyed by {@code String}. Versions are simple
 * monotonically increasing counters per entry; they are useful for future replication logic.
 */
@Component
public class CacheEngine {

    /** Internal storage mapping keys to cache entries. */
    private final ConcurrentHashMap<String, CacheEntry<Object>> store = new ConcurrentHashMap<>();

    /** Global version counter – increments for each {@code put}. */
    private final AtomicLong versionCounter = new AtomicLong();

    /** Maximum number of expired entries to remove per cleanup run. */
    @Value("${cache.cleanup.max-per-run:100}")
    private int maxCleanupPerRun;

    /**
     * Allows tests or runtime configuration to adjust the maximum number of entries cleaned per run.
     */
    public void setMaxCleanupPerRun(int maxCleanupPerRun) {
        this.maxCleanupPerRun = maxCleanupPerRun;
    }

    /** Interval (in milliseconds) between cleanup runs – configured via Spring property. */
    // The @Scheduled annotation below uses a fixed delay defined in properties.

    /**
     * Stores a value under the given key.
     *
     * @param key   cache key – must not be {@code null}
     * @param value value to store – must not be {@code null}
     * @param ttlMillis time‑to‑live in milliseconds. If {@code <= 0} the entry never expires.
     * @return the previous value associated with the key, or {@code null} if none.
     */
    public Object put(String key, Object value, long ttlMillis) {
        long now = Instant.now().toEpochMilli();
        long expiry = ttlMillis > 0 ? now + ttlMillis : Long.MAX_VALUE;
        long version = versionCounter.incrementAndGet();
        CacheEntry<Object> entry = new CacheEntry<>(value, expiry, version);
        CacheEntry<Object> previous = store.put(key, entry);
        return previous != null && !previous.isExpired(now) ? previous.getValue() : null;
    }

    /**
     * Retrieves a value if present and not expired.
     *
     * @param key cache key
     * @return the cached value or {@code null} if missing or expired.
     */
    public Object get(String key) {
        long now = Instant.now().toEpochMilli();
        CacheEntry<Object> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(now)) {
            store.remove(key, entry);
            return null;
        }
        return entry.getValue();
    }

    /**
     * Deletes an entry.
     *
     * @param key cache key
     * @return {@code true} if an entry was removed.
     */
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    /**
     * Checks whether a non‑expired entry exists for the given key.
     */
    public boolean exists(String key) {
        return get(key) != null;
    }

    /**
     * Clears the entire cache.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Background task that removes expired entries.
     *
     * The method runs with a fixed delay (default 5 seconds) and removes up to
     * {@code maxCleanupPerRun} entries per invocation to keep work bounded.
     */
    @Scheduled(fixedDelayString = "${cache.cleanup.interval:5000}")
    public void cleanupExpired() {
        long now = Instant.now().toEpochMilli();
        int removed = 0;
        for (Map.Entry<String, CacheEntry<Object>> e : store.entrySet()) {
            if (removed >= maxCleanupPerRun) {
                break;
            }
            if (e.getValue().isExpired(now)) {
                if (store.remove(e.getKey(), e.getValue())) {
                    removed++;
                }
            }
        }
        // No explicit logging – Micrometer metrics can be added later.
    }
}

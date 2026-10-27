package com.phoenix.cache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CacheEngine} covering CRUD, TTL, overwrites, and basic concurrency.
 */
@Execution(ExecutionMode.CONCURRENT)
class CacheEngineTest {

    private CacheEngine cacheEngine;

    @BeforeEach
    void setUp() {
        cacheEngine = new CacheEngine();
        // Configure small cleanup limits for fast tests.
        cacheEngine.setMaxCleanupPerRun(1000);
    }

    @Test
    void putAndGetBasic() {
        String key = "key1";
        String value = "value1";
        assertNull(cacheEngine.get(key));
        cacheEngine.put(key, value, 0);
        assertEquals(value, cacheEngine.get(key));
    }

    @Test
    void putOverwriteReturnsPrevious() {
        String key = "k";
        cacheEngine.put(key, "old", 0);
        Object prev = cacheEngine.put(key, "new", 0);
        assertEquals("old", prev);
        assertEquals("new", cacheEngine.get(key));
    }

    @Test
    void ttlExpiration() throws InterruptedException {
        String key = "temp";
        cacheEngine.put(key, "data", 100); // 100 ms TTL
        assertEquals("data", cacheEngine.get(key));
        Thread.sleep(150);
        assertNull(cacheEngine.get(key), "Entry should be expired after TTL");
    }

    @Test
    void ttlInvalidNegativeMeansNoExpiry() throws InterruptedException {
        String key = "forever";
        cacheEngine.put(key, "persistent", -1);
        Thread.sleep(200);
        assertEquals("persistent", cacheEngine.get(key), "Negative TTL should disable expiration");
    }

    @Test
    void deleteRemovesEntry() {
        String key = "toDelete";
        cacheEngine.put(key, "val", 0);
        assertTrue(cacheEngine.delete(key));
        assertNull(cacheEngine.get(key));
        assertFalse(cacheEngine.delete(key), "Second delete should report false");
    }

    @Test
    void existsReflectsPresenceAndExpiry() throws InterruptedException {
        String key = "existKey";
        cacheEngine.put(key, "v", 50);
        assertTrue(cacheEngine.exists(key));
        Thread.sleep(70);
        assertFalse(cacheEngine.exists(key), "Expired entry should not be considered existing");
    }

    @Test
    void clearEmptiesCache() {
        cacheEngine.put("a", 1, 0);
        cacheEngine.put("b", 2, 0);
        cacheEngine.clear();
        assertNull(cacheEngine.get("a"));
        assertNull(cacheEngine.get("b"));
    }

    @Test
    void backgroundCleanupRemovesExpired() throws InterruptedException {
        cacheEngine.put("exp1", "v", 10);
        cacheEngine.put("exp2", "v", 10);
        Thread.sleep(20);
        cacheEngine.cleanupExpired();
        assertNull(cacheEngine.get("exp1"));
        assertNull(cacheEngine.get("exp2"));
    }

    @Test
    void concurrentPutsAndGets() throws Exception {
        int threadCount = 10;
        int opsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            tasks.add(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    String key = "k" + (j % 50);
                    cacheEngine.put(key, "v" + id, 0);
                    Object v = cacheEngine.get(key);
                    assertNotNull(v);
                }
                return null;
            });
        }
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdownNow();
        assertNotNull(cacheEngine.get("k0"));
    }
}

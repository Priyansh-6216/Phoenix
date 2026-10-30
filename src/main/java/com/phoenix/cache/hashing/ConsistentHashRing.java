package com.phoenix.cache.hashing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Simple consistent hashing implementation using a sorted {@link TreeMap}.
 *
 * <p>Each physical node can be represented by multiple virtual nodes (replicas) to improve key distribution.
 * The ring uses 64‑bit hash values derived from {@link java.security.MessageDigest SHA‑256} for stability.
 *
 * <p>Only the core logic needed for Day 4 is provided – adding/removing nodes and locating the primary
 * node for a given key. Replica selection (next N nodes clockwise) is also supported.
 */
public class ConsistentHashRing {
    private final NavigableMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodesPerPhysical;

    /**
     * Creates a new hash ring.
     *
     * @param virtualNodesPerPhysical number of virtual nodes to create for each physical node (must be > 0)
     */
    public ConsistentHashRing(int virtualNodesPerPhysical) {
        if (virtualNodesPerPhysical <= 0) {
            throw new IllegalArgumentException("virtualNodesPerPhysical must be > 0");
        }
        this.virtualNodesPerPhysical = virtualNodesPerPhysical;
    }

    /** Adds a physical node to the ring, creating the configured number of virtual nodes. */
    public void addNode(String nodeId) {
        for (int i = 0; i < virtualNodesPerPhysical; i++) {
            long hash = hash(nodeId + "#" + i);
            ring.put(hash, nodeId);
        }
    }

    /** Removes a physical node and all of its virtual nodes from the ring. */
    public void removeNode(String nodeId) {
        List<Long> toRemove = ring.entrySet().stream()
                .filter(e -> e.getValue().equals(nodeId))
                .map(e -> e.getKey())
                .collect(Collectors.toList());
        toRemove.forEach(ring::remove);
    }

    /** Returns the primary node responsible for the given key. */
    public String getPrimaryNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty – no nodes added.");
        }
        long keyHash = hash(key);
        // ceilingEntry returns the first entry >= keyHash, or null if none.
        return ring.ceilingEntry(keyHash) != null
                ? ring.ceilingEntry(keyHash).getValue()
                : ring.firstEntry().getValue(); // wrap around
    }

    /** Returns up to {@code replicaCount} distinct nodes clockwise from the primary node. */
    public List<String> getReplicaNodes(String key, int replicaCount) {
        if (replicaCount <= 0) {
            throw new IllegalArgumentException("replicaCount must be > 0");
        }
        List<String> replicas = new ArrayList<>();
        if (ring.isEmpty()) {
            return replicas;
        }
        long hash = hash(key);
        var tail = ring.tailMap(hash, true);
        var iterator = tail.values().iterator();
        while (replicas.size() < replicaCount && iterator.hasNext()) {
            String node = iterator.next();
            if (!replicas.contains(node)) {
                replicas.add(node);
            }
        }
        // continue from beginning if needed
        if (replicas.size() < replicaCount) {
            for (String node : ring.values()) {
                if (!replicas.contains(node)) {
                    replicas.add(node);
                }
                if (replicas.size() >= replicaCount) {
                    break;
                }
            }
        }
        return replicas;
    }

    /** Returns a snapshot of current node IDs (unique physical nodes). */
    public List<String> getAllNodes() {
        return ring.values().stream().distinct().collect(Collectors.toList());
    }

    /** Simple 64‑bit hash based on SHA‑256; deterministic across runs. */
    private static long hash(String data) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            // Use the first 8 bytes to compose a long (big‑endian)
            long h = 0L;
            for (int i = 0; i < Long.BYTES; i++) {
                h = (h << 8) | (bytes[i] & 0xffL);
            }
            return h;
        } catch (Exception e) {
            // Fallback to default String hash code extended to long
            return data.hashCode();
        }
    }
}

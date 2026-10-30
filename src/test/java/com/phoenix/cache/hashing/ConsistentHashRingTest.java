package com.phoenix.cache.hashing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;

/**
 * Basic unit tests for {@link ConsistentHashRing}.
 */
class ConsistentHashRingTest {

    @Test
    void basicAddAndPrimaryNode() {
        ConsistentHashRing ring = new ConsistentHashRing(3);
        ring.addNode("nodeA");
        ring.addNode("nodeB");
        // With two nodes, any key should map to either nodeA or nodeB
        String primary = ring.getPrimaryNode("myKey");
        assertTrue(primary.equals("nodeA") || primary.equals("nodeB"));
    }

    @Test
    void replicaSelectionDistinct() {
        ConsistentHashRing ring = new ConsistentHashRing(2);
        ring.addNode("node1");
        ring.addNode("node2");
        ring.addNode("node3");
        List<String> replicas = ring.getReplicaNodes("someKey", 2);
        assertEquals(2, replicas.size());
        assertNotEquals(replicas.get(0), replicas.get(1));
    }

    @Test
    void removalWorks() {
        ConsistentHashRing ring = new ConsistentHashRing(1);
        ring.addNode("A");
        ring.addNode("B");
        ring.removeNode("A");
        // After removal, only B should be returned
        assertEquals("B", ring.getPrimaryNode("anyKey"));
        assertTrue(ring.getAllNodes().contains("B"));
        assertFalse(ring.getAllNodes().contains("A"));
    }
}

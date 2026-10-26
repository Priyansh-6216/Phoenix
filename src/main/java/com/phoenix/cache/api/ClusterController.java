package com.phoenix.cache.api;

import com.phoenix.cache.config.NodeProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cluster status endpoint.
 *
 * <p>Day 1 stub: returns the local node's identity and a placeholder topology.
 * Will be replaced in Day 5/6 with real membership state.
 */
@RestController
@RequestMapping("/cluster")
public class ClusterController {

    private final NodeProperties nodeProperties;

    /**
     * Constructs the controller with node configuration.
     *
     * @param nodeProperties externalized node identity settings
     */
    public ClusterController(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    /**
     * Returns cluster status including local node identity and topology version.
     *
     * <p>Day 1: single-node stub. Multi-node membership is added in Day 5.
     *
     * @return 200 with cluster status payload
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nodeId", nodeProperties.getId());
        payload.put("advertisedAddress", nodeProperties.getAdvertisedAddress());
        payload.put("topologyVersion", 0);
        payload.put("nodes", List.of(
            Map.of(
                "nodeId", nodeProperties.getId(),
                "address", nodeProperties.getAdvertisedAddress(),
                "state", "ACTIVE"
            )
        ));
        payload.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(payload);
    }
}

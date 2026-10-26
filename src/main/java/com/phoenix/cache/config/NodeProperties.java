package com.phoenix.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for node identity.
 *
 * <p>Properties are read from {@code application.yml} (or environment overrides) under
 * the {@code cache.node} prefix. This allows the same compiled artifact to run as
 * node-1, node-2, or node-3 by changing environment variables in Docker Compose.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * cache:
 *   node:
 *     id: node-1
 *     advertised-address: "cache-node-1:8080"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "cache.node")
public class NodeProperties {

    /**
     * Unique identifier for this node within the cluster.
     * Defaults to "node-1" for local single-node development.
     */
    private String id = "node-1";

    /**
     * Host:port advertised to peer nodes for inter-node communication.
     * Defaults to localhost:8080.
     */
    private String advertisedAddress = "localhost:8080";

    /**
     * Returns the node ID.
     *
     * @return node identifier string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the node ID.
     *
     * @param id unique node identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the advertised address.
     *
     * @return host:port string
     */
    public String getAdvertisedAddress() {
        return advertisedAddress;
    }

    /**
     * Sets the advertised address.
     *
     * @param advertisedAddress host:port string visible to peer nodes
     */
    public void setAdvertisedAddress(String advertisedAddress) {
        this.advertisedAddress = advertisedAddress;
    }
}

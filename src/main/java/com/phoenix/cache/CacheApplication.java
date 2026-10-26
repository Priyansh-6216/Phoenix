package com.phoenix.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Self-Healing Distributed Cache node.
 *
 * <p>Each running instance of this application represents one cache node in the cluster.
 * Node identity (ID, advertised address, port) is read from external configuration so that
 * the same artifact can be started as node-1, node-2, or node-3 without recompilation.
 *
 * <p>Day 1 establishes the build baseline. Distributed-systems components are added
 * incrementally in subsequent days following the project plan.
 */
@SpringBootApplication
@EnableScheduling
public class CacheApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments (passed through to Spring)
     */
    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }
}

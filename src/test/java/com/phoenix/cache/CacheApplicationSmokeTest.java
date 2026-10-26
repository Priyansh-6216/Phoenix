package com.phoenix.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 1 smoke tests.
 *
 * <p>These tests verify the build baseline:
 * <ul>
 *   <li>The application context loads without errors.</li>
 *   <li>The liveness probe responds 200 with status UP.</li>
 *   <li>The readiness probe responds 200 with status UP.</li>
 *   <li>The cluster status endpoint responds with expected fields.</li>
 *   <li>The Prometheus metrics endpoint is accessible.</li>
 * </ul>
 *
 * <p>More focused unit and integration tests are added per day as components are built.
 *
 * <p>Note: Spring Boot Actuator health endpoints return {@code application/vnd.spring-boot.actuator.v3+json},
 * which is not a sub-type of {@code application/json}. Assertions use {@code content().string()} instead
 * of {@code contentTypeCompatibleWith(MediaType.APPLICATION_JSON)} to avoid false failures.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CacheApplicationSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * The Spring application context must load without errors.
     * If this test fails the entire build must stop.
     */
    @Test
    void contextLoads() {
        // If the context fails to start, Spring throws before this line.
    }

    /**
     * Liveness probe must return HTTP 200 with status=UP.
     *
     * <p>Actuator endpoints use {@code application/vnd.spring-boot.actuator.v3+json}.
     * We assert on the response body string to avoid media-type compatibility issues.
     */
    @Test
    void livenessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    /**
     * Readiness probe must return HTTP 200 when the node is ready to serve.
     */
    @Test
    void readinessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    /**
     * Cluster status endpoint must return the local node's identity.
     * Day 1: single-node stub; real membership is introduced in Day 5.
     */
    @Test
    void clusterStatusContainsNodeId() throws Exception {
        mockMvc.perform(get("/cluster/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.nodeId").value("node-1"))
            .andExpect(jsonPath("$.topologyVersion").value(0))
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.nodes[0].state").value("ACTIVE"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * Prometheus metrics endpoint must be accessible and return non-empty content.
     * Verifies Micrometer and the Prometheus registry are correctly wired.
     * The endpoint responds with plain text in Prometheus exposition format.
     */
    @Test
    void prometheusMetricsEndpointIsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(emptyString())));
    }
}

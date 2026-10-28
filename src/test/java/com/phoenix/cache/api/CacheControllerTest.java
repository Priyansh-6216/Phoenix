package com.phoenix.cache.api;

import com.phoenix.cache.core.CacheEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link CacheController} using {@link MockMvc}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheEngine cacheEngine;

    @BeforeEach
    void setUp() {
        cacheEngine.clear();
    }

    @Test
    void testPutGetDeleteFlow() throws Exception {
        // PUT a value
        mockMvc.perform(put("/cache/foo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"bar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous").doesNotExist());

        // GET the value
        mockMvc.perform(get("/cache/foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is("bar")));

        // DELETE the value
        mockMvc.perform(delete("/cache/foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", is(true)));

        // GET after delete -> 404
        mockMvc.perform(get("/cache/foo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Key not found or expired")));
    }

    @Test
    void testPutWithTtlExpires() throws Exception {
        // Put with TTL = 100 ms
        mockMvc.perform(put("/cache/ttlKey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"temp\", \"ttl\": 100}"))
                .andExpect(status().isOk());

        // Immediately GET should succeed
        mockMvc.perform(get("/cache/ttlKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is("temp")));

        // Wait for expiration
        Thread.sleep(150);

        // GET after expiration -> 404
        mockMvc.perform(get("/cache/ttlKey"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Key not found or expired")));
    }
}

package com.phoenix.cache.api;

import com.phoenix.cache.core.CacheEngine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for cache operations.
 *
 * <p>Provides CRUD endpoints with optional TTL handling. Errors are returned as a JSON
 * payload containing an {@code error} message and a {@code status} code.
 */
@RestController
@RequestMapping("/cache")
@Validated
public class CacheController {

    private final CacheEngine cacheEngine;

    public CacheController(CacheEngine cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    /**
     * Store or update a value.
     *
     * @param key   cache key (must not be blank)
     * @param request body containing the value and optional ttl in milliseconds
     * @return previous value if present, otherwise {@code null}
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> put(
            @PathVariable @NotBlank String key,
            @Valid @RequestBody CacheRequest request) {
        long ttl = request.getTtl() != null ? request.getTtl() : 0L;
        Object previous = cacheEngine.put(key, request.getValue(), ttl);
        Map<String, Object> body = new HashMap<>();
        body.put("previous", previous);
        return ResponseEntity.ok(body);
    }

    /**
     * Retrieve a value.
     */
    @GetMapping("/{key}")
    public ResponseEntity<Object> get(@PathVariable @NotBlank String key) {
        Object value = cacheEngine.get(key);
        if (value == null) {
            return errorResponse("Key not found or expired", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(value);
    }

    /**
     * Delete a value.
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable @NotBlank String key) {
        boolean removed = cacheEngine.delete(key);
        if (!removed) {
            return errorResponse("Key not found", HttpStatus.NOT_FOUND);
        }
        Map<String, Object> body = Map.of("deleted", true);
        return ResponseEntity.ok(body);
    }

    private <T> ResponseEntity<T> errorResponse(String message, HttpStatus status) {
        Map<String, Object> err = Map.of(
                "error", message,
                "status", status.value()
        );
        @SuppressWarnings("unchecked")
        ResponseEntity<T> resp = (ResponseEntity<T>) ResponseEntity.status(status).body(err);
        return resp;
    }
}

/**
 * Request payload for {@link CacheController#put(String, CacheRequest)}.
 */
class CacheRequest {
    @NotNull
    private Object value;

    @PositiveOrZero(message = "TTL must be non‑negative")
    private Long ttl; // milliseconds, null means no TTL

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}

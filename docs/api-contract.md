# API Contract — Self-Healing Distributed Cache

**Status:** Day 1 initial specification. Request/response schemas will be finalized
alongside implementation in Day 3.

---

## Base URL

Each cache node exposes its own API. Example: `http://localhost:8080`.

---

## Cache Operations

### PUT /cache/{key}

Store or update a value. Optionally set a TTL.

**Request**

```
PUT /cache/{key}
Content-Type: application/json

{
  "value": "<string | base64-encoded bytes>",
  "ttlSeconds": 300        // optional; omit for no expiration
}
```

**Path parameters**

| Parameter | Type | Constraints |
|-----------|------|-------------|
| `key` | string | 1–512 characters; printable ASCII |

**Body fields**

| Field | Type | Constraints |
|-------|------|-------------|
| `value` | string | 1 byte – 1 MB |
| `ttlSeconds` | integer | 1 – 86400 (optional) |

**Responses**

| Status | Meaning |
|--------|---------|
| 200 | Key stored or updated |
| 400 | Invalid key, value, or TTL |
| 503 | Node unavailable or quorum not achievable |

**Success body**

```json
{
  "key": "user:42",
  "version": 1,
  "expiresAt": "2026-10-26T17:00:00Z"  // null if no TTL
}
```

---

### GET /cache/{key}

Retrieve a stored value.

**Request**

```
GET /cache/{key}
```

**Responses**

| Status | Meaning |
|--------|---------|
| 200 | Key found and not expired |
| 404 | Key not found (cache miss) |
| 410 | Key found but entry has expired (returned on lazy expiration) |
| 503 | Node unavailable |

**Success body**

```json
{
  "key": "user:42",
  "value": "...",
  "version": 1,
  "createdAt": "2026-10-26T16:00:00Z",
  "expiresAt": "2026-10-26T17:00:00Z"   // null if no TTL
}
```

**Miss body (404)**

```json
{
  "error": "CACHE_MISS",
  "key": "user:42",
  "message": "Key not found"
}
```

---

### DELETE /cache/{key}

Delete and invalidate a value. Propagated to replicas.

**Request**

```
DELETE /cache/{key}
```

**Responses**

| Status | Meaning |
|--------|---------|
| 204 | Key deleted (or was already absent) |
| 503 | Node unavailable or quorum not achievable |

---

## Health and Readiness

### GET /actuator/health/liveness

Reports whether the process is alive. A non-200 response causes orchestrators to restart the pod.

**Response 200**

```json
{ "status": "UP" }
```

---

### GET /actuator/health/readiness

Reports whether the node can serve its intended role (cache reads/writes). Returns 200 only when
the node is `ACTIVE` and has completed any recovery synchronization.

**Response 200 (ready)**

```json
{ "status": "UP" }
```

**Response 503 (not ready)**

```json
{
  "status": "OUT_OF_SERVICE",
  "components": {
    "node": {
      "status": "OUT_OF_SERVICE",
      "details": { "state": "RECOVERING" }
    }
  }
}
```

---

## Cluster Status

### GET /cluster/status

Returns the current membership view from this node's perspective.

**Day 1:** Single-node stub. Multi-node membership added in Day 5.

**Response 200**

```json
{
  "nodeId": "node-1",
  "advertisedAddress": "localhost:8080",
  "topologyVersion": 0,
  "nodes": [
    {
      "nodeId": "node-1",
      "address": "localhost:8080",
      "state": "ACTIVE"
    }
  ],
  "timestamp": "2026-10-26T16:00:00Z"
}
```

**Node state values:** `JOINING`, `ACTIVE`, `SUSPECT`, `UNAVAILABLE`, `RECOVERING`, `LEFT`

---

## Metrics

### GET /actuator/prometheus

Prometheus-format metrics. Scraped by the Prometheus server.

---

## Error Format

All error responses use the following JSON structure:

```json
{
  "error": "<ERROR_CODE>",
  "message": "<human-readable description>",
  "key": "<affected key, if applicable>",
  "timestamp": "<ISO-8601>"
}
```

**Error codes**

| Code | Meaning |
|------|---------|
| `INVALID_KEY` | Key violates length or character constraints |
| `INVALID_VALUE` | Value exceeds size limit |
| `INVALID_TTL` | TTL is out of range |
| `CACHE_MISS` | Key not found |
| `ENTRY_EXPIRED` | Key found but expired |
| `QUORUM_UNAVAILABLE` | Insufficient healthy nodes for configured R/W |
| `INTERNAL_ERROR` | Unexpected server error (details withheld from clients) |

Internal stack traces and configuration details are never returned to clients.

---

## Idempotency and Retry Semantics

- `PUT` is idempotent when the same `{key, value, ttlSeconds}` is sent repeatedly.
- Concurrent `PUT` operations on the same key are resolved using the version field; the
  higher version wins. Ties are broken by wall-clock timestamp, which is an approximation
  and can produce stale reads under concurrent writes — see `docs/consistency-model.md`.
- `DELETE` is idempotent: deleting an absent key returns 204.
- Clients should retry `503` responses with exponential back-off. Re-sending a `PUT`
  after a `503` may result in a duplicate write if the first attempt partially succeeded.

---

## Bounded Request Constraints

| Constraint | Limit |
|------------|-------|
| Maximum key length | 512 bytes |
| Maximum value size | 1 MB |
| Maximum TTL | 86 400 seconds (24 hours) |
| Request timeout (server-side) | 5 seconds |
| Connection timeout | 5 seconds |

These limits prevent unbounded memory use and are enforced by the API layer.

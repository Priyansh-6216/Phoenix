# Assumptions and Non-Functional Goals

**Status:** Day 1 initial record. Update as implementation progresses or decisions change.
Conflicting decisions must be explained before architecture changes are made.

---

## 1. Deployment Assumptions

| Assumption | Value / Policy |
|------------|----------------|
| Minimum cluster size | 3 independent nodes (processes or containers) |
| Maximum cluster size | Not yet defined; prototype targets ≤ 10 nodes |
| Node colocation | Nodes may run on the same physical host in development; production should use distinct hosts |
| Network | Local LAN with < 5 ms RTT; wide-area scenarios are out of scope |
| Clock synchronization | Clocks are approximately synchronized (NTP); not treated as a strongly reliable ordering mechanism |
| Persistence medium | Local filesystem for the journal; no distributed storage assumed |

---

## 2. Capacity Assumptions

| Parameter | Initial assumption | Adjustable? |
|-----------|-------------------|-------------|
| Maximum entries per node | 1 000 000 | Yes — via config |
| Maximum value size | 1 MB | Yes — enforced by API |
| Maximum key length | 512 bytes | Yes — enforced by API |
| Target memory per node | ≤ 4 GB heap | Documented in JVM flags |
| Entry count at which eviction is considered | TBD (Day 2) | Yes |

---

## 3. Replication Assumptions

| Parameter | Initial assumption |
|-----------|--------------------|
| Replication factor (N) | 3 |
| Write quorum (W) | 2 |
| Read quorum (R) | 2 |
| Replica placement | Distinct physical nodes; no two replicas on the same node |
| Replication transport | HTTP over the inter-node API; gRPC or custom protocol may be considered later |

**Warning:** N=3, R=2, W=2 is a starting experiment. It does not guarantee linearizability.
Concurrent-write ordering, stale reads, membership disagreement, and split-brain risks are
documented in `docs/consistency-model.md` (Day 11).

---

## 4. Failure Detection Assumptions

| Parameter | Initial value | Configurable? |
|-----------|--------------|---------------|
| Heartbeat interval | 2 seconds | Yes |
| Suspicion threshold (missed heartbeats before SUSPECT) | 3 consecutive misses | Yes |
| Failure threshold (time in SUSPECT before UNAVAILABLE) | 10 seconds | Yes |
| False-positive risk | Acknowledged — network delays can mimic failures | N/A |
| Permanent-failure assumption | Never assumed without explicit operator intervention; nodes may rejoin | N/A |

**Limitation:** Heartbeats alone do not prevent split-brain. Two network partitions may each
believe the other side is unavailable and accept conflicting writes. This is documented as a
known risk.

---

## 5. Consistency Policy

| Property | Claim |
|----------|-------|
| Read-your-writes | **Not guaranteed** across different coordinating nodes in this prototype |
| Monotonic reads | **Not guaranteed** |
| Linearizability | **Not claimed** — quorum intersection alone does not provide this |
| Eventual consistency | **Intended** — replicas converge when network is stable and no conflicts exist |
| Conflict resolution | Last-write-wins by version, tie-broken by timestamp (clock-skew caveat applies) |
| Stale reads | Possible when R quorum is achieved via a lagging replica |

These constraints will be formalized and tested in Day 11.

---

## 6. Durability Assumptions

| Scenario | Expected outcome |
|----------|-----------------|
| Node crash after write acknowledged by W replicas | Write survives if at least one of the W-acknowledging replicas remains available |
| Node crash before W acknowledgements | Write may be lost (acknowledged only locally or not at all) |
| Disk failure on a single node | Surviving replicas serve requests; journal on that node is lost |
| All nodes crash simultaneously | Data loss; journal allows partial recovery from last checkpoint |

**Important:** "Zero data loss" is never claimed. The journal and replication design are
scoped to the prototype failure scenarios tested in Day 9 and Day 14.

---

## 7. TTL and Invalidation Assumptions

| Property | Policy |
|----------|--------|
| TTL resolution | 1-second granularity |
| Expired entry behavior | Not returned on `GET`; cleaned up lazily on access and by scheduled sweeper |
| Invalidation propagation | `DELETE` propagated to all replicas; tombstone prevents stale resurrection |
| Tombstone retention | Configurable; default 5 minutes |
| Cross-node TTL consistency | TTL is stored with the entry; clock skew may cause minor divergence |

---

## 8. Performance Goals (to be measured, not assumed)

These are targets for the benchmark, not guarantees.

| Metric | Target (single node, warm) | Notes |
|--------|---------------------------|-------|
| Throughput | ≥ 10 000 ops/s | Read-heavy workload; to be measured |
| p50 latency | < 5 ms | Local single-node only |
| p95 latency | < 20 ms | |
| p99 latency | < 50 ms | |
| Failure detection time | < 30 seconds | From process stop to UNAVAILABLE state |
| Rebalancing convergence | < 60 seconds | For 10 000 keys; to be measured |

All numbers must be replaced with measured values from Day 13. Hardware, JVM flags, workload
parameters, and test duration must be reported alongside results.

---

## 9. Operational Assumptions

- The system is operated by a knowledgeable operator, not a fully automated orchestrator.
- No automatic scaling is assumed; node join/leave is operator-initiated.
- Metrics and logs are the primary operational interface (Prometheus + Grafana, Day 12).
- No secrets or credentials are stored in source control.

---

## 10. Out of Scope

- Wide-area / multi-datacenter replication.
- Strong consistency or consensus protocols (Raft, Paxos).
- Auto-scaling or container orchestration beyond Docker Compose.
- TLS between nodes (acceptable for local prototype; must be noted as a production gap).
- Authentication or authorization.
- Persistent storage beyond the append-only journal.

# Architecture — Self-Healing Distributed Cache

**Status:** Day 1 baseline. Updated incrementally as components are built.

---

## High-Level Request Flow

```
Client
  │
  ▼
┌─────────────────────┐
│    API Layer        │  REST endpoints, validation, error format
│  (Spring Boot)      │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│   Routing Layer     │  Consistent hash ring → select owner/replica
│                     │  Membership view (topology version)
└────────┬────────────┘
         │
    ┌────┴────────┐
    │             │
    ▼             ▼
┌────────┐   ┌─────────┐
│Primary │   │Replica  │  Per-physical node
│  Node  │   │  Nodes  │
│        │   │         │
│ Cache  │   │ Cache   │  ConcurrentHashMap + entry metadata
│ Store  │   │ Store   │
└────────┘   └─────────┘
    │
    ▼
┌─────────────────────┐
│  Membership Monitor │  Heartbeats, timeouts, suspicion, state machine
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  Rebalancing /      │  Ownership recalculation, batched key transfer
│  Recovery Manager   │  Journal replay, synchronization, reconciliation
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  Observability      │  Micrometer metrics, structured logs, Prometheus
└─────────────────────┘
```

---

## Component Map

| Component | Package | Day introduced | Status |
|---|---|---|---|
| API layer | `api/` | Day 3 | ⬜ Stub only |
| Cache store | `cache/` | Day 2 | ⬜ Not started |
| TTL manager | `cache/` | Day 2 | ⬜ Not started |
| Consistent hash ring | `hashing/` | Day 4 | ⬜ Not started |
| Membership manager | `cluster/` | Day 6 | ⬜ Not started |
| Health monitor | `cluster/` | Day 6 | ⬜ Not started |
| Replication manager | `replication/` | Day 7 | ⬜ Not started |
| Routing layer | `cluster/` | Day 5 | ⬜ Not started |
| Rebalancing planner | `rebalancing/` | Day 8 | ⬜ Not started |
| Transfer worker | `rebalancing/` | Day 8 | ⬜ Not started |
| Recovery manager | `recovery/` | Day 9 | ⬜ Not started |
| Invalidation coordinator | `invalidation/` | Day 10 | ⬜ Not started |
| Single-flight coordinator | `coordination/` | Day 10 | ⬜ Not started |
| Observability layer | `observability/` | Day 12 | ⬜ Not started |

---

## Repository Layout

```
Phoenix/
├── src/
│   ├── main/java/com/phoenix/cache/
│   │   ├── CacheApplication.java          # Spring Boot entry point
│   │   ├── api/                           # REST controllers
│   │   │   └── ClusterController.java     # /cluster/status (Day 1 stub)
│   │   ├── cache/                         # Cache store + TTL (Day 2)
│   │   ├── cluster/                       # Membership + health (Day 5/6)
│   │   ├── hashing/                       # Consistent hash ring (Day 4)
│   │   ├── replication/                   # Replication manager (Day 7)
│   │   ├── recovery/                      # Journal + recovery (Day 9)
│   │   ├── rebalancing/                   # Rebalancing (Day 8)
│   │   ├── invalidation/                  # Invalidation + tombstones (Day 10)
│   │   ├── coordination/                  # Single-flight (Day 10)
│   │   ├── observability/                 # Metrics + structured logs (Day 12)
│   │   └── config/
│   │       └── NodeProperties.java        # cache.node.* config binding
│   └── test/java/com/phoenix/cache/
│       ├── CacheApplicationSmokeTest.java # Day 1 smoke tests
│       ├── unit/                          # Day 2+ unit tests
│       ├── integration/                   # Day 3+ integration tests
│       ├── concurrency/                   # Day 11 concurrency tests
│       └── resilience/                    # Day 14 chaos tests
├── deployment/
│   ├── docker/                            # Dockerfile (Day 5)
│   ├── prometheus/                        # prometheus.yml (Day 12)
│   └── grafana/                           # Grafana dashboards (Day 12)
├── benchmarks/                            # k6/Gatling scripts (Day 13)
├── scripts/                               # Helper shell scripts
├── docs/
│   ├── architecture.md                    # This file
│   ├── api-contract.md                    # REST API specification
│   ├── assumptions.md                     # Capacity, replication, consistency
│   ├── consistency-model.md               # Consistency spec (Day 11)
│   ├── failure-matrix.md                  # Failure scenarios (Day 14)
│   └── benchmark-results.md               # Benchmark output (Day 13)
├── checkstyle.xml
├── pom.xml
├── Dockerfile                             # (Day 5)
├── docker-compose.yml                     # (Day 5)
└── README.md
```

---

## Node State Machine

Nodes transition through the following states (implemented Day 6):

```
                  ┌──────────┐
     startup      │          │   heartbeat OK
  ──────────────► │ JOINING  │──────────────────►┌────────┐
                  │          │                   │        │
                  └──────────┘                   │ ACTIVE │◄─────────────────┐
                                                 │        │                  │
                                                 └───┬────┘                  │
                                      missed hb  │                        recovery
                                                 ▼                        complete
                                            ┌─────────┐
                                            │ SUSPECT │
                                            └────┬────┘
                                 timeout     │
                                             ▼
                                       ┌───────────┐       restart
                                       │UNAVAILABLE│──────────────►┌───────────┐
                                       └───────────┘               │RECOVERING │
                                             │                     └───────────┘
                                   deliberate│
                                   shutdown  ▼
                                         ┌─────┐
                                         │ LEFT│
                                         └─────┘
```

---

## Consistent Hash Ring (Day 4)

- SHA-256 or MurmurHash3 maps node IDs to ring positions.
- Configurable virtual nodes per physical node (default: 150).
- Key maps to the first clockwise ring position.
- Replica selection walks clockwise, skipping duplicate physical nodes.
- Ring is protected by a `ReadWriteLock` to allow concurrent reads with exclusive writes.

---

## Replication Model (Day 7)

Configurable `N`, `R`, `W` policy (default experiment: N=3, R=2, W=2):

- `N` — number of replicas per key.
- `R` — read acknowledgements required.
- `W` — write acknowledgements required.

**Important:** This policy does not guarantee linearizability. Concurrent-write ordering,
stale reads, membership disagreement, and conflict resolution require explicit rules
(documented in `docs/consistency-model.md`, Day 11).

---

## Data Flow Diagrams

*Detailed sequence diagrams for write, read, failover, rebalancing, and recovery will be
added as each component is implemented (Days 5–9).*

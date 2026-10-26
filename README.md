# Self-Healing Distributed Cache

[![CI](https://github.com/priyanshsuthar/Phoenix/actions/workflows/ci.yml/badge.svg)](https://github.com/priyanshsuthar/Phoenix/actions/workflows/ci.yml)

> A fault-tolerant distributed in-memory key-value cache that distributes keys across nodes,
> detects node failures, reroutes requests, and restores data placement automatically.

## Status

| Day | Milestone | Status |
|-----|-----------|--------|
| 1 | Requirements, architecture, repository setup | ✅ Complete |
| 2 | Single-node cache engine | ⬜ Not started |
| 3 | REST API and configuration | ⬜ Not started |
| 4 | Consistent hashing | ⬜ Not started |
| 5 | Multi-node cluster and routing | ⬜ Not started |
| 6 | Membership and health monitoring | ⬜ Not started |
| 7 | Replication and failover | ⬜ Not started |
| 8 | Data rebalancing | ⬜ Not started |
| 9 | Journal and node recovery | ⬜ Not started |
| 10 | Invalidation and stampede protection | ⬜ Not started |
| 11 | Concurrency and consistency hardening | ⬜ Not started |
| 12 | Observability | ⬜ Not started |
| 13 | Benchmarking | ⬜ Not started |
| 14 | Chaos and resilience testing | ⬜ Not started |
| 15 | Portfolio packaging and final demo | ⬜ Not started |

## Problem

A single cache instance becomes a bottleneck and a single point of failure. Distributed caches
introduce additional challenges: data placement, replica consistency, node membership, failure
detection, recovery, invalidation, and operational visibility.

## Solution

A multi-node prototype that implements:
- **Consistent hashing** — deterministic key routing with minimal movement on topology change.
- **Heartbeat-based failure detection** — suspicion periods prevent false positives.
- **Configurable replication** — N replicas across distinct physical nodes.
- **Automatic failover** — reads reroute to healthy replicas when policy permits.
- **Incremental rebalancing** — only affected keyspace moves on membership change.
- **Journal-backed recovery** — nodes replay mutations before re-joining as active.
- **TTL and invalidation** — per-entry expiration with cross-replica propagation.
- **Single-flight coordination** — concurrent misses for the same key share one load.

## Technology Stack

| Area | Choice |
|------|--------|
| Language | Java 21 |
| Build | Maven 3.9+ |
| API | Spring Boot 3.3 + REST |
| Cache storage | `ConcurrentHashMap` + explicit entry metadata |
| Hash ring | Custom `TreeMap`-backed consistent hash |
| Membership | Custom state machine + heartbeat protocol |
| Recovery | Append-only journal + checkpoint |
| Testing | JUnit 5, Spring Boot Test |
| Infrastructure | Docker + Docker Compose |
| Metrics | Micrometer + Prometheus |
| Dashboards | Grafana |
| Load testing | k6 or Gatling |
| CI | GitHub Actions |

## Prerequisites

- Java 21 (verify: `java --version`)
- Maven 3.9+ (verify: `mvn --version`)
- Docker and Docker Compose (for multi-node cluster, added Day 5)

## Quick Start (Single Node)

```bash
# Clone and build
git clone <repo-url>
cd Phoenix

# Build and run all tests
mvn clean verify

# Start a single node on port 8080
mvn spring-boot:run

# Smoke-test the running node
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/cluster/status
curl http://localhost:8080/actuator/prometheus
```

## Multi-Node Cluster (Day 5+)

```bash
# Three-node cluster (added in Day 5)
docker compose up
```

## API Reference

See [docs/api-contract.md](docs/api-contract.md) for the full API specification.

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `PUT` | `/cache/{key}` | Store or update a value (optional TTL) |
| `GET` | `/cache/{key}` | Retrieve a value or documented miss |
| `DELETE` | `/cache/{key}` | Delete / invalidate a value |
| `GET` | `/actuator/health/liveness` | Is the process alive? |
| `GET` | `/actuator/health/readiness` | Can the node serve requests? |
| `GET` | `/cluster/status` | Node states and topology |
| `GET` | `/actuator/prometheus` | Prometheus metrics |

## Architecture

See [docs/architecture.md](docs/architecture.md) for the full architecture document and diagrams.

## Assumptions and Limitations

See [docs/assumptions.md](docs/assumptions.md) for capacity, replication, failure model,
and consistency assumptions.

## Known Limitations (Day 1)

- Single node only; cluster topology is a stub.
- No cache storage — `PUT`/`GET`/`DELETE` are not yet implemented (Day 2+).
- No persistent journal (Day 9).
- No Docker Compose multi-node setup (Day 5).

## Contributing

This is a solo learning project. See the
[project plan](SELF_HEALING_DISTRIBUTED_CACHE_PROJECT_PLAN.md) for the implementation roadmap.

## License

MIT

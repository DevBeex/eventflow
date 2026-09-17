# EventFlow Backend (MVP)

> Full product overview, architecture, and recruiter demo: see the root [`README.md`](../README.md).

Quarkus monolith implementing RF-001 through RF-013 from `EventFlow-Requerimientos-MVP.md`.

## Pinned versions

| Component | Version |
|---|---|
| Java | 21 |
| Quarkus platform BOM | 3.33.3 |
| PostgreSQL | 16.6 |
| Apache Kafka (KRaft) | 3.8.1 (`apache/kafka:3.8.1`) |
| Maven Wrapper | 3.9.x (via project wrapper) |

## API endpoints

| Method | Path | RF |
|---|---|---|
| POST | `/api/customers` | RF-001 |
| GET | `/api/customers` | RF-002 |
| POST | `/api/products` | RF-003 |
| GET | `/api/products` | RF-004 |
| GET | `/api/products/{id}` | RF-005 |
| POST | `/api/orders` | RF-006 |
| GET | `/api/orders` | RF-008 |
| GET | `/api/orders/{id}` | RF-009 |
| PATCH | `/api/orders/{id}/status` | RF-010 |
| GET | `/api/notifications` | RF-011 |
| GET | `/api/notifications/{id}` | RF-012 |

Background: outbox publisher scheduler (RF-013) and Kafka consumer on topic `order-created` (RF-007).

OpenAPI: `/q/openapi` · Swagger UI: `/q/swagger-ui` · Health: `/q/health`

## Quick start (Docker Compose)

From `backend/`:

```bash
cp .env.example .env
docker compose up --build
```

From repo root:

```bash
docker compose up --build
```

`app` does not start until the `tests` service finishes `mvn verify` successfully
(Testcontainers + PostgreSQL/Kafka). The image build itself only packages the app;
the gate is the Compose `tests` service (and CI `./mvnw verify`).

Stop (keeps volumes):

```bash
docker compose down
```

Destructive reset:

```bash
docker compose down -v
```

## Local development without Java on PATH

```powershell
docker run --rm -v "C:/Users/brod2/Documents/repositories/eventflow/backend:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-21 bash -lc "chmod +x mvnw && ./mvnw -B test"
```

Start dependencies only:

```bash
docker compose up postgres kafka -d
```

Run app with Quarkus dev mode inside Docker (after dependencies are up):

```powershell
docker run --rm -it -v "C:/Users/brod2/Documents/repositories/eventflow/backend:/workspace" -w /workspace -p 8080:8080 --network backend_default `
  -e DB_HOST=postgres -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 `
  maven:3.9.11-eclipse-temurin-21 bash -lc "chmod +x mvnw && ./mvnw -B quarkus:dev"
```

## Demo script

With stack running on port 8080:

```powershell
./scripts/demo.ps1
```

```bash
chmod +x scripts/demo.sh && ./scripts/demo.sh
```

## Tests

```bash
./mvnw -B test
./mvnw -B verify
```

Tests use Quarkus Dev Services: PostgreSQL **16.6** and Redpanda **v24.2.4** (Kafka-compatible). Docker Compose demo uses Apache Kafka **3.8.1**.

### Automated acceptance coverage

| AC | Status |
|---|---|
| AC-01..AC-11 | Automated in unit/integration tests |
| AC-14, AC-15, AC-18, AC-22, AC-23 | Automated (`NotificationFlowTest`) |
| AC-24 | Partial (sequential transition tests; full concurrent race is manual) |
| AC-25, AC-26 | Automated in `OrderResourceTest` |
| AC-28, AC-29, AC-33 | Automated in resource tests |
| AC-02 | Partial (duplicate email sequential; concurrent race manual) |
| AC-12, AC-16, AC-17, AC-19, AC-20, AC-21 | Manual / documented below |
| AC-31, AC-32 | Manual compose persistence checks |

## Consumer recovery (section 8.2)

If an invalid message stops the consumer channel:

1. Inspect logs for topic/partition/offset/eventId.
2. Stop the application.
3. For test injections only, advance the consumer group offset explicitly:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group eventflow-notifications-v1 \
  --topic order-created \
  --reset-offsets --to-offset <next-offset> --execute
```

Never skip offsets for legitimate events without diagnosis. Restart the app after fixing the root cause.

## MVP limits

- Single app instance, no auth, one event (`ORDER_CREATED`), one Kafka topic/partition.
- Invalid Kafka messages block the partition until manual intervention.
- Outbox relay retries indefinitely every 5 seconds.
- Kafka retention: 7 days (`604800000` ms).
- No HTTP idempotency on order creation.

## Environment variables

See `.env.example`.

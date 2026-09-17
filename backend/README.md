# EventFlow Backend

Quarkus service for customers, products, orders, transactional outbox publishing, and Kafka-driven notifications.

Overview and full-stack quick start: [`../README.md`](../README.md).

## Versions

| Component | Version |
|---|---|
| Java | 21 |
| Quarkus BOM | 3.33.3 |
| PostgreSQL | 16.6 |
| Apache Kafka (KRaft) | 3.8.1 (`apache/kafka:3.8.1`) |

## Endpoints

| Method | Path |
|---|---|
| POST / GET | `/api/customers` |
| POST / GET | `/api/products` |
| GET | `/api/products/{id}` |
| POST / GET | `/api/orders` |
| GET | `/api/orders/{id}` |
| PATCH | `/api/orders/{id}/status` |
| GET | `/api/notifications` |
| GET | `/api/notifications/{id}` |

Background work: outbox publisher (scheduled) and Kafka consumer on `order-created`.

OpenAPI: `/q/openapi` · Swagger UI: `/q/swagger-ui` · Health: `/q/health`

## Run with Docker Compose

From repo root (preferred):

```bash
docker compose up --build
```

Or from `backend/`:

```bash
cp .env.example .env
docker compose up --build
```

The Compose `tests` service runs `mvn verify` before `app` starts.

```bash
docker compose down       # keep volumes
docker compose down -v    # reset data
```

## Tests

```bash
./mvnw -B test
./mvnw -B verify
```

Dev Services / Testcontainers: PostgreSQL **16.6** and Redpanda **v24.2.4** (Kafka API). Compose demo uses Apache Kafka **3.8.1**.

Covered automatically: customer/product validation, order totals and status transitions, notification after publish/consume.

Manual checks worth doing once: concurrent duplicate email, Kafka stop/start while creating an order, compose restart without `-v`.

## Demo scripts

With the API on port 8080:

```powershell
./scripts/demo.ps1
```

```bash
chmod +x scripts/demo.sh && ./scripts/demo.sh
```

## Consumer recovery

If a bad message stops the consumer:

1. Check logs for topic / partition / offset / eventId.
2. Stop the app.
3. Only for deliberate test injections, advance the group offset:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group eventflow-notifications-v1 \
  --topic order-created \
  --reset-offsets --to-offset <next-offset> --execute
```

Do not skip offsets for real business events without diagnosis. Fix the cause, then restart.

## Limits

- One app instance, no auth, single event type and topic/partition.
- Invalid messages block the partition until handled.
- Outbox retries every 5 seconds while pending.
- Kafka retention: 7 days.
- Order creation is not HTTP-idempotent.

## Environment

See `.env.example`.

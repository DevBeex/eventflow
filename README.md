# EventFlow

Full-stack order flow with a transactional outbox: the API persists the order and a pending event in one PostgreSQL transaction, a scheduler publishes to Kafka, and a consumer records an internal notification — at most once per event.

| Layer | Stack |
|---|---|
| Backend | Java 21 · Quarkus 3.33.3 · PostgreSQL 16.6 · Apache Kafka 3.8.1 |
| Frontend | React 19 · TypeScript · Vite · Tailwind 4 · shadcn/ui · TanStack Query |
| Tooling | Docker Compose · Flyway · GitHub Actions · Testcontainers |

The UI shows confirmed totals and notifications from the API. It does not fake Kafka progress.

---

## How it works

1. Client sends `customerId` and line items (no prices).
2. Server loads catalog prices, computes subtotals/total in `BigDecimal`, and inserts `orders`, `order_items`, and `outbox_events` in **one transaction**.
3. A scheduled publisher sends `ORDER_CREATED` to topic `order-created`.
4. The consumer checks the outbox payload and inserts a notification (deduped by `eventId`).
5. If Kafka is unavailable, create-order still returns **201**; pending outbox rows are published when the broker recovers.

```text
React SPA  --REST /api-->  Quarkus (API + publisher + consumer)
                              |                    |
                         PostgreSQL            Kafka
                      (data + outbox)      (order-created)
```

One JVM process owns the API, outbox relay, and consumer. Shared database; no separate microservices for this MVP.

---

## Layout

```text
eventflow/
├── backend/             # Quarkus API, Flyway, outbox, Kafka, tests
├── frontend/            # React SPA (Vite proxies /api → backend)
├── docker-compose.yml
└── .github/workflows/
```

---

## Quick start

### Backend

```bash
docker compose up --build
```

Compose runs `mvn verify` before starting the app container.

| URL | |
|---|---|
| http://localhost:8080/api | REST API |
| http://localhost:8080/q/swagger-ui | OpenAPI UI |
| http://localhost:8080/q/health | Health |

```bash
docker compose down      # keep volumes
docker compose down -v   # wipe data
```

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

App: http://localhost:5173/demo (proxies `/api` to port 8080).

---

## Try the flow

1. Create a customer and two active products (`75.00` and `50.00`).
2. On **Demo**, pick the customer, add both products with quantities `2` and `1`.
3. Estimated total `$200.00` → create order → detail shows **server-confirmed** total.
4. Activity section polls until the notification appears.
5. Complete the order; cancel should return **409**.

Kafka recovery check:

```bash
docker compose stop kafka
# create an order → still 201
docker compose start kafka
# notification shows up without recreating the order
```

API demo scripts: [`backend/scripts/demo.ps1`](backend/scripts/demo.ps1) · [`backend/scripts/demo.sh`](backend/scripts/demo.sh).

---

## Backend

### API (`/api`)

| Method | Path |
|---|---|
| POST, GET | `/customers` |
| POST, GET | `/products` |
| GET | `/products/{id}` |
| POST, GET | `/orders` |
| GET | `/orders/{id}` |
| PATCH | `/orders/{id}/status` |
| GET | `/notifications` |
| GET | `/notifications/{id}` |

Status rules: `CREATED` → `COMPLETED` or `CANCELLED`. Terminal states stay terminal (same status is a no-op; other transitions → `409`).

### Design notes

- Money: `BigDecimal` / `NUMERIC`, scale 2, no float/double.
- Create-order body: only `customerId` and `items[{productId, quantity}]`.
- Outbox written in the same TX as the order; publish happens outside that TX.
- Consumer group `eventflow-notifications-v1`; topic `order-created` (1 partition, 7-day retention).
- Schema via Flyway; Hibernate validates and does not auto-update.

More detail: [`backend/README.md`](backend/README.md).

---

## Frontend

| Route | |
|---|---|
| `/demo` | Create order |
| `/orders`, `/orders/:id` | List, detail, status, notification activity |
| `/customers`, `/products` | Catalog create/list |
| `/notifications` | Notification list/detail |

- IDs handled with `lossless-json` (safe int64).
- Estimates in integer cents; confirmed total always from the API.
- No automatic retries on POST/PATCH.
- Notification polling every 2s for up to 30s (pauses when the tab is hidden).

More detail: [`frontend/README.md`](frontend/README.md).

---

## Tests

### Backend

```bash
cd backend
./mvnw -B verify
```

Integration tests use Testcontainers / Dev Services (PostgreSQL + Kafka-compatible broker). Compose also gates app startup on a green `verify`.

### Frontend

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run check:design
npm run test:run
npm run build
```

### CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs backend `verify` + image build, and frontend lint/typecheck/design/tests/build.

---

## Scope of this MVP

Single app instance, no auth, one business event (`ORDER_CREATED`), no payments/inventory/tax. Invalid Kafka messages stop the consumer channel until investigated (see backend README recovery notes).

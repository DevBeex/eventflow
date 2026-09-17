# EventFlow

**Orders that survive Kafka outages. Notifications that never duplicate.**

EventFlow is a full-stack MVP that shows how a real backend owns money, state, and async reliability — while a focused React demo makes that behavior visible without Swagger.

| Layer | Stack |
|---|---|
| **Backend** | Java 21 · Quarkus 3.33.3 · PostgreSQL 16.6 · Apache Kafka 3.8.1 |
| **Frontend** | React 19 · TypeScript · Vite · Tailwind 4 · shadcn/ui · TanStack Query |
| **Delivery** | Docker Compose · Flyway · GitHub Actions · Testcontainers |

> The UI never invents totals or “Kafka success” animations. It only shows what the API and the consumer actually produce.

---

## Why this project exists

Most portfolio CRUDs stop at “POST returns 201”. EventFlow goes one step further:

1. **Create an order** — only `customerId` + items. No prices from the client.
2. **Server calculates** unit prices, subtotals, and total (USD, exact decimals).
3. **Same PostgreSQL transaction** writes the order, line items, and a pending **outbox** event.
4. A **scheduler** publishes `ORDER_CREATED` to Kafka (`order-created`).
5. A **consumer** validates the canonical outbox payload and inserts **at most one** notification.
6. If Kafka is down, the API still returns **201**; when Kafka returns, the pending event is published and the notification appears — without recreating the order.

That is the story a recruiter can watch in the browser in about 90 seconds.

---

## Architecture (at a glance)

```text
┌─────────────┐     REST /api      ┌──────────────────────────────────────┐
│  React SPA  │ ─────────────────► │  Quarkus monolith                     │
│  /demo      │ ◄───────────────── │  customers · products · orders        │
└─────────────┘   JSON contracts   │  notifications · outbox               │
                                   └───────────┬──────────────┬────────────┘
                                               │              │
                                      PostgreSQL│              │Kafka
                                      (source of│              │order-created
                                       truth +  │              │
                                       outbox)  ▼              ▼
                                   ┌────────────┐      ┌─────────────┐
                                   │ 6 tables   │      │ 1 partition │
                                   │ Flyway V1  │      │ RF=1, 7d    │
                                   └────────────┘      └─────────────┘
```

**One deployable application.** Publisher and consumer live in the same process and share the database. No microservices theater, no fake Event Sourcing.

Specifications (normative):

- [`EventFlow-Requerimientos-MVP.md`](EventFlow-Requerimientos-MVP.md) — backend contracts RF-001…RF-013  
- [`EventFlow-Frontend-React-Shadcn.md`](EventFlow-Frontend-React-Shadcn.md) — frontend FE-001…FE-009  

---

## Repository layout

```text
eventflow/
├── backend/          # Quarkus API, Flyway, outbox, Kafka consumer, Compose, tests
├── frontend/         # React demo SPA (Vite proxy → backend)
├── docker-compose.yml
└── .github/workflows/ci.yml
```

| Path | Responsibility |
|---|---|
| [`backend/`](backend/) | REST API, schema, transactional outbox, Kafka publish/consume, integration tests |
| [`frontend/`](frontend/) | Recruiter demo UI: create order → confirmed total → live notification polling |
| [`docker-compose.yml`](docker-compose.yml) | Postgres + Kafka + app; **tests gate** before app starts |

---

## Quick start

### 1) Backend (API + Postgres + Kafka)

```bash
docker compose up --build
```

Compose runs `mvn verify` in a `tests` service first. **The API does not start until tests pass.**

| URL | Purpose |
|---|---|
| http://localhost:8080/api | Business API |
| http://localhost:8080/q/swagger-ui | Interactive OpenAPI |
| http://localhost:8080/q/health | Liveness / readiness |

Stop (keep data): `docker compose down`  
Reset volumes: `docker compose down -v`

### 2) Frontend (demo UI)

```bash
cd frontend
npm ci
npm run dev
```

| URL | Purpose |
|---|---|
| http://localhost:5173/demo | Main demo flow |
| Vite proxy `/api` → `http://localhost:8080` | Same-origin API in development |

---

## Recruiter demo (90 seconds)

1. Open **Clientes** → create a customer.  
2. Open **Productos** → create two active products: **75.00** and **50.00**.  
3. Open **Demo** → select customer, add both products, quantities **2** and **1**.  
4. Estimated total **$200.00** → **Crear orden**.  
5. Detail shows **Total confirmado: $200.00** from the server.  
6. **Actividad** polls notifications → **Notificación registrada** (real row, not a fake progress bar).  
7. **Completar** the order → try **Cancelar** → **409** (terminal state preserved).

Optional reliability demo:

```bash
docker compose stop kafka
# create another order in the UI → still 201
docker compose start kafka
# notification appears without recreating the order
```

Scripts that create data via API (capture real IDs): [`backend/scripts/demo.ps1`](backend/scripts/demo.ps1) / [`backend/scripts/demo.sh`](backend/scripts/demo.sh).

---

## Backend — what it actually does

### Domain & API

| Concern | Behavior |
|---|---|
| Customers | Create + list; email stripped/lowercased; unique → `409 DUPLICATE_CUSTOMER_EMAIL` |
| Products | Create + list + get; inactive products allowed for catalog, rejected on new orders (`409 PRODUCT_INACTIVE`) |
| Orders | Create with server-side pricing; list/filter; detail; status transitions with row lock |
| Notifications | List/get internal records only (no email/SMS) |

Endpoints (prefix `/api`):

| Method | Path | Notes |
|---|---|---|
| POST/GET | `/customers` | RF-001 / RF-002 |
| POST/GET | `/products` | RF-003 / RF-004 |
| GET | `/products/{id}` | RF-005 |
| POST/GET | `/orders` | RF-006 / RF-008 |
| GET | `/orders/{id}` | RF-009 |
| PATCH | `/orders/{id}/status` | RF-010 — `CREATED`→`COMPLETED`/`CANCELLED`; terminals are final |
| GET | `/notifications` | RF-011 |
| GET | `/notifications/{id}` | RF-012 |

### Money & validation

- `BigDecimal` / `NUMERIC` only — no float/double for money.  
- Prices must be exact to **two decimals** (no silent rounding).  
- Request bodies reject unknown fields; IDs and quantities are typed strictly.  
- Order payload from clients is **only** `customerId` + `items[{productId, quantity}]`.

### Async reliability (the core)

| Piece | Role |
|---|---|
| `outbox_events` | Pending `ORDER_CREATED` written in the **same TX** as the order |
| Scheduler (RF-013) | Every 1s, up to 20 pending; publish outside the DB TX; mark `PUBLISHED` after broker ack |
| Consumer (RF-007) | Group `eventflow-notifications-v1`; validates payload against outbox; `INSERT … ON CONFLICT` style dedupe |
| Topic | `order-created`, 1 partition, 7-day retention |

**Guarantees (honest):** at-least-once publish, at-most-one notification per event, no exactly-once end-to-end claim.

### Data model (Flyway)

`customers`, `products`, `orders`, `order_items`, `notifications`, `outbox_events` — constraints and indexes owned by Flyway; Hibernate validates, does not mutate schema.

---

## Frontend — what it actually does

| Screen | Purpose |
|---|---|
| `/demo` | Create order + “Cómo funciona” (Postgres → Kafka → notification) |
| `/orders`, `/orders/:id` | List/filter, confirmed totals, status actions, **activity polling** |
| `/customers`, `/products` | Create + list (paginated selectors for the demo form) |
| `/notifications` | List/detail with copyable `eventId` |

Technical choices that protect backend contracts:

- **lossless-json** for int64 IDs (no `Number(id)` truncation).  
- Money estimates in **integer cents**; confirmed total always from API.  
- POST/PATCH **retry = false**; timeouts treated as uncertain results.  
- Notification polling: every 2s for up to 30s, paused when the tab is hidden.  
- Design tokens centralized (`tokens.css`); `npm run check:design` fails on raw palette colors in features.

---

## Tests

### Backend (`backend/`)

```bash
cd backend
./mvnw -B verify
```

Uses Quarkus Dev Services / Testcontainers: **PostgreSQL 16.6** + Kafka-compatible broker for integration tests.

| Coverage (examples) | What it proves |
|---|---|
| Customers | Normalization, validation, duplicate email → 409 |
| Products | Price scale, `active` default/omit/null rules |
| Orders | Total `2×75 + 1×50 = 200.00`, inactive/missing refs, status transitions |
| Notifications | Order creation eventually yields one notification (publish + consume) |
| Compose gate | `tests` service must exit 0 before `app` starts |

Manual / operational cases (Kafka cutover, concurrent status races, invalid consumer messages) are documented in [`backend/README.md`](backend/README.md).

### Frontend (`frontend/`)

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run check:design
npm run test:run
npm run build
```

| Check | Purpose |
|---|---|
| Unit tests | ID/money codecs, order payload builder, error mapping, polling helpers |
| `check:design` | No hard-coded brand colors outside tokens |
| Playwright | Smoke E2E setup (`npx playwright install chromium` then `npm run test:e2e`) |

### CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- **backend:** `./mvnw -B verify` + Docker image build  
- **frontend:** `lint` · `typecheck` · `check:design` · `test:run` · `build`

---

## Explicit non-goals (MVP)

No auth, payments, inventory, tax, multi-tenant, GraphQL, Debezium, Redis, Kubernetes, or “admin reprocess” panels.  
Invalid Kafka messages **stop the consumer channel** by design (visible failure, documented recovery) — not silent skip.

---

## Learn more

| Doc | Content |
|---|---|
| [`backend/README.md`](backend/README.md) | Versions, endpoints, demo scripts, consumer recovery |
| [`frontend/README.md`](frontend/README.md) | Dev server, env, quality commands |
| [`frontend/docs/demo.md`](frontend/docs/demo.md) | Recruiter walkthrough |
| [`frontend/docs/architecture.md`](frontend/docs/architecture.md) | Feature boundaries |

---

## License / intent

Built as a **portfolio MVP**: small surface area, strong backend invariants, and a UI that makes those invariants observable.

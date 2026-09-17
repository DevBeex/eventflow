# EventFlow Frontend

React 19 SPA to create orders and watch the async notification produced by the backend.

Overview and full-stack quick start: [`../README.md`](../README.md).

## Setup

- Node.js 22 + npm
- Backend on `http://localhost:8080`

```bash
npm ci
copy .env.example .env
npm run dev
```

Vite proxies `/api` to the backend. `VITE_API_BASE_URL` is public config only (no secrets).

## Scripts

- `npm run dev` — development server
- `npm run build` / `npm run preview` — production build
- `npm run lint`, `npm run typecheck`, `npm run check:design` — quality
- `npm run test:run` — Vitest
- `npm run test:e2e` — Playwright (`npx playwright install chromium` first)

## Demo path

Create a customer and two active products (75.00 / 50.00), open `/demo`, quantities 2 and 1, create the order once. The detail page shows the confirmed total and polls for the notification (up to 30s). If it is late, use “Volver a consultar”; do not resubmit the order.

No login, catalog edit/delete, or payments in this UI.

## Docs

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/design-system.md`](docs/design-system.md)
- [`docs/demo.md`](docs/demo.md)
- [`CONTRIBUTING.md`](CONTRIBUTING.md)

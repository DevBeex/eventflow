# EventFlow Frontend

> Full product overview, architecture, and recruiter demo: see the root [`README.md`](../README.md).

SPA en React 19 para crear órdenes y observar la notificación asíncrona de EventFlow.

## Requisitos e instalación

- Node.js 22 y npm
- Backend EventFlow en `http://localhost:8080`

```bash
npm ci
copy .env.example .env
npm run dev
```

Vite abre la aplicación y redirige `/api` al backend. `VITE_API_BASE_URL` permite cambiar el prefijo público; no debe contener secretos.

## Comandos

- `npm run dev`: servidor de desarrollo
- `npm run build` / `npm run preview`: build y previsualización
- `npm run lint`, `npm run typecheck`, `npm run check:design`: calidad
- `npm run test:run`: Vitest y Testing Library/MSW
- `npm run test:e2e`: smoke de Playwright (antes ejecuta `npx playwright install chromium`)

## Demo

Prepara un cliente y dos productos activos (75.00 y 50.00), abre `/demo`, selecciona cantidades 2 y 1 y crea la orden. El detalle muestra el total confirmado y consulta la notificación por 30 segundos. Si tarda, usa «Volver a consultar»; no recrees la orden.

La UI usa datos reales. No incluye autenticación, edición de catálogo, pagos ni controles administrativos.

## Documentación

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/design-system.md`](docs/design-system.md)
- [`docs/demo.md`](docs/demo.md)
- [`CONTRIBUTING.md`](CONTRIBUTING.md)

# Arquitectura

`app` compone providers, rutas y layout. `pages/demo` coordina el recorrido principal usando las APIs públicas de `features`. Cada feature contiene consultas, validación y UI de su dominio; las dependencias entre features pasan por `public.tsx`.

`components/ui` contiene primitivas Radix/shadcn y `components/core` composiciones sin negocio. `lib` no conoce componentes ni features: concentra transporte, precisión numérica, formatos, rutas y contratos transversales. ESLint impide las dependencias inversas más peligrosas.

TanStack Query mantiene estado remoto; React Hook Form mantiene formularios; query params mantienen filtros y páginas. El cliente HTTP valida cada respuesta con Zod, conserva IDs int64 con `lossless-json` y no reintenta escrituras.

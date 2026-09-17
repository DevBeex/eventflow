# Sistema de diseño

`src/styles/tokens.css` es la única fuente de colores y medidas de marca. `theme.css` mapea los tokens a Tailwind 4 y `globals.css` carga las capas. `npm run check:design` rechaza colores literales y utilidades de paleta en `src`.

Las primitivas de `components/ui` ofrecen botones, campos, tarjetas, tablas, diálogos, alertas y estados visuales. Las composiciones de `components/core` unifican encabezados, vacíos, errores, carga, paginación, envío, confirmación, errores de campo y copiado.

Usa siempre nombres semánticos (`primary`, `success`, `destructive`). Los estados incluyen texto además de color y conservan foco visible. La interfaz responde desde 360 px y respeta movimiento reducido.

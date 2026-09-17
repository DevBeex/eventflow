# Contribuir

Usa Node 22 y `npm ci`. Antes de enviar cambios ejecuta:

```bash
npm run lint
npm run typecheck
npm run check:design
npm run test:run
npm run build
```

Mantén la UI en español y los nombres de código en inglés. No agregues colores fuera de `tokens.css`, imports internos entre features, conversiones numéricas de IDs, reintentos de escrituras ni datos ficticios en el build. Los contratos nuevos deben validarse con Zod en el límite HTTP y acompañarse de pruebas.

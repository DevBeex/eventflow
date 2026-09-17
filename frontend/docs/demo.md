# Guía de demo

1. Inicia PostgreSQL, Kafka y el backend en el puerto 8080.
2. Crea un cliente y dos productos activos con precios 75.00 y 50.00 desde la UI o el script del backend.
3. Abre `/demo`, selecciona los registros y usa cantidades 2 y 1.
4. Confirma el estimado de 200.00 y crea la orden una sola vez.
5. En el detalle, muestra el total confirmado y espera «Notificación registrada».
6. Abre la notificación, copia el eventId y vuelve a la orden.
7. Completa o cancela la orden con el diálogo de confirmación.

Si la notificación no aparece en 30 segundos, explica que la orden sí fue confirmada y pulsa «Volver a consultar» cuando Kafka esté disponible. Un timeout al crear deja el borrador y dirige a revisar órdenes antes de repetir el POST.

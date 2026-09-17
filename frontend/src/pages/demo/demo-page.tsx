import { Card, CardContent } from '@/components/ui'
import { PageHeader } from '@/components/core'
import { CreateOrderForm } from '@/features/orders/public'

export default function DemoPage() {
  return <div className="space-y-6"><PageHeader title="Crea una orden y observa cómo se registra su notificación automáticamente" description="Elige un cliente, agrega productos y sigue el resultado sin salir del detalle." /><CreateOrderForm /><Card><CardContent className="pt-6"><details><summary className="cursor-pointer font-semibold">Cómo funciona</summary><div className="mt-3 space-y-2 text-sm text-muted-foreground"><p>La API guarda la orden y registra un evento pendiente en PostgreSQL.</p><p>Un proceso de Java con Quarkus publica el evento mediante Kafka y otro registra la notificación.</p><p>La pantalla consulta esa notificación real; no muestra telemetría ni pasos inventados.</p></div></details></CardContent></Card></div>
}

import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { CopyValue, EmptyState, ErrorState, LoadingState, PageHeader, PaginationControls } from '@/components/core'
import { Alert, Button, Card, CardContent, CardHeader, CardTitle, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui'
import { routes } from '@/lib/config/routes'
import { formatDate } from '@/lib/format'
import { request, type ApiError } from '@/lib/http'
import { notificationSchema, pageSchema } from '@/lib/types/contracts'

export const notificationKeys = { all: ['notifications'] as const, list: (filters: object) => ['notifications', 'list', filters] as const, detail: (id: string) => ['notifications', 'detail', id] as const }
const listNotifications = (page: number, size: number, orderId?: string, signal?: AbortSignal) => request(`/notifications?page=${page}&size=${size}${orderId ? `&orderId=${orderId}` : ''}`, pageSchema(notificationSchema), { signal })
export const shouldContinuePolling = (deadline: number, hasNotification: boolean, hasError: boolean, hidden: boolean, now = Date.now()) => !hasNotification && !hasError && !hidden && now < deadline

export function OrderActivity({ orderId }: { orderId: string }) {
  const [windowKey, setWindowKey] = useState(0)
  const deadline = useRef(Date.now() + 30_000)
  const [expired, setExpired] = useState(false)
  const query = useQuery({
    queryKey: [...notificationKeys.list({ page: 0, size: 20, orderId }), windowKey],
    queryFn: ({ signal }) => listNotifications(0, 20, orderId, signal),
    retry: false,
    refetchInterval: (state) => {
      return shouldContinuePolling(deadline.current, Boolean(state.state.data?.items[0]), Boolean(state.state.error), document.hidden) ? 2_000 : false
    },
    refetchIntervalInBackground: false,
  })
  useEffect(() => {
    if (query.data?.items[0] || query.error) return
    const timer = window.setTimeout(() => setExpired(true), Math.max(0, deadline.current - Date.now()))
    return () => window.clearTimeout(timer)
  }, [query.data, query.error, windowKey])
  const restart = () => { deadline.current = Date.now() + 30_000; setExpired(false); setWindowKey((value) => value + 1) }
  const notification = query.data?.items[0]
  return <Card><CardHeader><CardTitle>Actividad</CardTitle></CardHeader><CardContent aria-live="polite">{query.isLoading ? <p>Consultando notificación…</p> : query.isError ? <ErrorState message="No pudimos consultar la notificación" onRetry={restart} /> : notification ? <Alert className="bg-success-surface text-success"><p className="font-semibold">Notificación registrada</p><p className="mt-1">Orden creada · {formatDate(notification.createdAt)}</p><Link className="mt-3 inline-block underline" to={routes.notification(notification.id)}>Ver notificación</Link></Alert> : expired ? <div><p>Aún no aparece la notificación. Puedes volver a consultar.</p><Button className="mt-3" variant="outline" onClick={restart}>Volver a consultar</Button></div> : <p>Notificación aún no disponible. Consultando notificación…</p>}</CardContent></Card>
}

export function NotificationsPage() {
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0), size = Number(params.get('size') ?? 20), orderId = params.get('orderId') ?? ''
  const query = useQuery({ queryKey: notificationKeys.list({ page, size, orderId }), queryFn: ({ signal }) => listNotifications(page, size, orderId, signal), staleTime: 15_000, placeholderData: keepPreviousData })
  const update = (changes: Record<string, string>) => setParams((current) => { const next = new URLSearchParams(current); Object.entries(changes).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key)); return next })
  return <div className="space-y-6"><PageHeader title="Notificaciones" description="Registros creados por el procesamiento asíncrono." /><Card><CardContent className="pt-6"><label className="mb-5 block text-sm font-medium">ID de orden<input className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3 sm:max-w-xs" inputMode="numeric" value={orderId} onChange={(event) => update({ page: '0', orderId: event.target.value.replace(/\D/g, '') })} /></label>{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title="Sin notificaciones" description={orderId ? 'No hay registros para esa orden.' : 'Aún no se registraron notificaciones.'} /> : <div className="space-y-4"><Table><TableHeader><TableRow><TableHead>Orden</TableHead><TableHead>Tipo</TableHead><TableHead>Mensaje</TableHead><TableHead>Fecha</TableHead></TableRow></TableHeader><TableBody>{query.data.items.map((notification) => <TableRow key={notification.id}><TableCell><Link className="underline" to={routes.order(notification.orderId)}>#{notification.orderId}</Link></TableCell><TableCell><Link className="font-medium underline" to={routes.notification(notification.id)}>Orden creada</Link></TableCell><TableCell>{notification.message}</TableCell><TableCell>{formatDate(notification.createdAt)}</TableCell></TableRow>)}</TableBody></Table><PaginationControls {...query.data} onPageChange={(next) => update({ page: String(next) })} onSizeChange={(next) => update({ page: '0', size: String(next) })} /></div>}</CardContent></Card></div>
}

export function NotificationDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({ queryKey: notificationKeys.detail(id), queryFn: ({ signal }) => request(`/notifications/${id}`, notificationSchema, { signal }), retry: false })
  if (query.isLoading) return <LoadingState label="Cargando notificación" />
  if (query.isError) return (query.error as ApiError).status === 404 ? <EmptyState title="Notificación no encontrada" description="El registro solicitado no existe." action={<Link to={routes.notifications}><Button>Ver notificaciones</Button></Link>} /> : <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} />
  const notification = query.data
  return <div className="space-y-6"><PageHeader title="Notificación" description="Detalle del registro asíncrono." /><Card><CardContent className="grid gap-5 pt-6 sm:grid-cols-2"><div><dt className="text-sm text-muted-foreground">Tipo</dt><dd className="font-medium">Orden creada</dd></div><div><dt className="text-sm text-muted-foreground">Fecha</dt><dd>{formatDate(notification.createdAt)}</dd></div><div><dt className="text-sm text-muted-foreground">Orden</dt><dd><Link className="underline" to={routes.order(notification.orderId)}>#{notification.orderId}</Link></dd></div><div className="sm:col-span-2"><dt className="text-sm text-muted-foreground">Event ID</dt><dd className="mt-1"><CopyValue value={notification.eventId} label="Copiar event ID" /></dd></div><div className="sm:col-span-2"><dt className="text-sm text-muted-foreground">Mensaje técnico</dt><dd>{notification.message}</dd></div></CardContent></Card></div>
}

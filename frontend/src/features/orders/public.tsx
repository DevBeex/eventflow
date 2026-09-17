import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { Minus } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { ConfirmActionDialog, EmptyState, ErrorState, LoadingState, PageHeader, PaginationControls, SubmitButton } from '@/components/core'
import { Alert, Badge, Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui'
import { CustomerSelector } from '@/features/customers/public'
import { OrderActivity } from '@/features/notifications/public'
import { ProductSelector } from '@/features/products/public'
import { formatDate, formatMoney } from '@/lib/format'
import { request, type ApiError } from '@/lib/http'
import { decimalToCents, idToJson } from '@/lib/http/codecs'
import { routes } from '@/lib/config/routes'
import { orderDetailSchema, orderStatusSchema, orderSummarySchema, pageSchema, type Customer, type OrderDetail, type Product } from '@/lib/types/contracts'

export const orderKeys = { all: ['orders'] as const, list: (filters: object) => ['orders', 'list', filters] as const, detail: (id: string) => ['orders', 'detail', id] as const }
export type OrderDraft = { customer?: Customer; items: Array<{ product: Product; quantity: number }> }
export function buildOrderPayload(draft: OrderDraft) {
  if (!draft.customer || draft.items.length < 1 || draft.items.length > 100) throw new Error('Orden inválida')
  return { customerId: idToJson(draft.customer.id), items: draft.items.map(({ product, quantity }) => ({ productId: idToJson(product.id), quantity })) }
}
export function estimateOrderCents(items: OrderDraft['items']) {
  return items.reduce((total, item) => total + decimalToCents(item.product.price) * item.quantity, 0)
}
export function OrderStatusBadge({ status }: { status: z.infer<typeof orderStatusSchema> }) {
  const labels = { CREATED: 'Creada', COMPLETED: 'Completada', CANCELLED: 'Cancelada' }
  return <Badge className={status === 'COMPLETED' ? 'bg-success-surface text-success' : status === 'CANCELLED' ? 'bg-danger-surface text-destructive' : 'bg-info-surface text-info'}>{labels[status]}</Badge>
}

export function CreateOrderForm() {
  const navigate = useNavigate(), client = useQueryClient()
  const [draft, setDraft] = useState<OrderDraft>({ items: [] })
  const [uncertain, setUncertain] = useState(false)
  const submitting = useRef(false)
  const mutation = useMutation({
    mutationFn: async () => {
      if (submitting.current) throw new Error('Solicitud en curso')
      submitting.current = true
      return request('/orders', orderDetailSchema, { method: 'POST', body: buildOrderPayload(draft) as unknown as BodyInit, uncertainOnFailure: true })
    },
    retry: false,
    onSuccess: (order) => { client.setQueryData(orderKeys.detail(order.id), order); client.invalidateQueries({ queryKey: orderKeys.all }); navigate(routes.order(order.id), { state: { created: true } }) },
    onError: (error: ApiError) => { if (error.uncertain) setUncertain(true) },
    onSettled: () => { submitting.current = false },
  })
  const add = (product: Product) => setDraft((current) => current.items.some((item) => item.product.id === product.id) ? current : { ...current, items: [...current.items, { product, quantity: 1 }] })
  const valid = Boolean(draft.customer && draft.items.length && draft.items.every((item) => Number.isInteger(item.quantity) && item.quantity >= 1 && item.quantity <= 999))
  const cents = estimateOrderCents(draft.items)
  return <div className="grid gap-6 lg:grid-cols-[1fr_22rem]"><Card><CardHeader><CardTitle>Datos de la orden</CardTitle></CardHeader><CardContent className="space-y-6"><CustomerSelector value={draft.customer} onSelect={(customer) => setDraft((current) => ({ ...current, customer }))} disabled={mutation.isPending} /><div className="space-y-3"><div className="flex items-center justify-between"><Label>Productos</Label><ProductSelector selectedIds={draft.items.map((item) => item.product.id)} onSelect={add} disabled={mutation.isPending || draft.items.length >= 100} /></div>{draft.items.length === 0 ? <p className="rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground">Agrega al menos un producto activo.</p> : draft.items.map((item) => <div key={item.product.id} className="grid grid-cols-[1fr_6rem_auto] items-center gap-3 rounded-md border p-3"><div><p className="font-medium">{item.product.name}</p><p className="text-sm text-muted-foreground">{formatMoney(item.product.price)} por unidad</p></div><Input aria-label={`Cantidad de ${item.product.name}`} type="number" min="1" max="999" value={item.quantity} disabled={mutation.isPending} onChange={(event) => setDraft((current) => ({ ...current, items: current.items.map((row) => row.product.id === item.product.id ? { ...row, quantity: Number(event.target.value) } : row) }))} /><Button type="button" variant="ghost" size="icon" aria-label={`Quitar ${item.product.name}`} disabled={mutation.isPending} onClick={() => setDraft((current) => ({ ...current, items: current.items.filter((row) => row.product.id !== item.product.id) }))}><Minus className="size-4" /></Button></div>)}</div>{mutation.isError && !uncertain && <ErrorState message={(mutation.error as ApiError).message ?? 'No se pudo crear la orden.'} />}{uncertain && <Alert className="bg-warning-surface text-warning"><p className="font-medium">No pudimos confirmar el resultado. Revisa las órdenes antes de intentar crear otra</p><div className="mt-3 flex gap-2"><Button variant="outline" onClick={() => navigate(routes.orders)}>Ver órdenes</Button><Button variant="outline" onClick={() => setUncertain(false)}>Volver al formulario</Button></div></Alert>}</CardContent></Card><Card className="h-fit"><CardHeader><CardTitle>Resumen</CardTitle></CardHeader><CardContent className="space-y-4"><div className="flex justify-between text-lg"><span>Total estimado</span><strong>{formatMoney(`${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`)}</strong></div><p className="text-sm text-muted-foreground">El backend confirmará precios y total al crear la orden.</p><SubmitButton className="w-full" pending={mutation.isPending} disabled={!valid || uncertain} onClick={() => mutation.mutate()}>Crear orden</SubmitButton></CardContent></Card></div>
}

export function OrdersPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0), size = Number(params.get('size') ?? 20), status = params.get('status') ?? '', customerId = params.get('customerId') ?? ''
  const queryString = new URLSearchParams({ page: String(page), size: String(size), ...(status && { status }), ...(customerId && { customerId }) })
  const query = useQuery({ queryKey: orderKeys.list({ page, size, status, customerId }), queryFn: ({ signal }) => request(`/orders?${queryString}`, pageSchema(orderSummarySchema), { signal }), placeholderData: keepPreviousData, staleTime: 15_000 })
  const update = (changes: Record<string, string>) => setParams((current) => { const next = new URLSearchParams(current); Object.entries(changes).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key)); return next })
  return <div className="space-y-6"><PageHeader title="Órdenes" description="Consulta órdenes y filtra sus estados." actions={<Link to={routes.demo}><Button>Nueva orden</Button></Link>} /><Card><CardContent className="pt-6"><div className="mb-5 grid gap-3 sm:grid-cols-2"><div><Label htmlFor="order-status">Estado</Label><select id="order-status" className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3" value={status} onChange={(event) => update({ page: '0', status: event.target.value })}><option value="">Todos</option><option value="CREATED">Creadas</option><option value="COMPLETED">Completadas</option><option value="CANCELLED">Canceladas</option></select></div><div><Label htmlFor="customer-filter">ID de cliente</Label><Input id="customer-filter" inputMode="numeric" value={customerId} onChange={(event) => update({ page: '0', customerId: event.target.value.replace(/\D/g, '') })} /></div></div>{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title={status || customerId ? 'Sin resultados' : 'No hay órdenes'} description={status || customerId ? 'Prueba otros filtros.' : 'Crea una orden desde la demo.'} /> : <div className="space-y-4" aria-busy={query.isFetching}><Table><TableHeader><TableRow><TableHead>Número</TableHead><TableHead>Cliente</TableHead><TableHead>Estado</TableHead><TableHead>Total</TableHead><TableHead>Fecha</TableHead></TableRow></TableHeader><TableBody>{query.data.items.map((order) => <TableRow key={order.id} className="cursor-pointer" tabIndex={0} onClick={() => navigateTo(order.id)} onKeyDown={(event) => event.key === 'Enter' && navigateTo(order.id)}><TableCell>#{order.id}</TableCell><TableCell>{order.customer.name}</TableCell><TableCell><OrderStatusBadge status={order.status} /></TableCell><TableCell>{formatMoney(order.total)}</TableCell><TableCell>{formatDate(order.createdAt)}</TableCell></TableRow>)}</TableBody></Table><PaginationControls {...query.data} onPageChange={(next) => update({ page: String(next) })} onSizeChange={(next) => update({ page: '0', size: String(next) })} /></div>}</CardContent></Card></div>
  function navigateTo(id: string) { navigate(routes.order(id)) }
}

export function OrderDetailPage() {
  const { id = '' } = useParams(), client = useQueryClient()
  const query = useQuery({ queryKey: orderKeys.detail(id), queryFn: ({ signal }) => request(`/orders/${id}`, orderDetailSchema, { signal }), retry: false, refetchOnMount: 'always' })
  const mutation = useMutation({
    mutationFn: (status: 'COMPLETED' | 'CANCELLED') => request(`/orders/${id}/status`, orderSummarySchema, { method: 'PATCH', body: { status } as unknown as BodyInit }),
    retry: false,
    onSuccess: (summary) => { client.setQueryData(orderKeys.detail(id), (old: OrderDetail | undefined) => old ? { ...old, ...summary } : old); client.invalidateQueries({ queryKey: orderKeys.all }) },
    onError: (error: ApiError) => { if (error.status === 409) query.refetch() },
  })
  if (query.isLoading) return <LoadingState label="Cargando orden" />
  if (query.isError) return query.error.status === 404 ? <EmptyState title="Orden no encontrada" description="La orden solicitada no existe." action={<Link to={routes.orders}><Button>Ver órdenes</Button></Link>} /> : <ErrorState message={query.error.message} onRetry={() => query.refetch()} />
  const order = query.data
  return <div className="space-y-6"><PageHeader title={`Orden #${order.id}`} description={`Creada ${formatDate(order.createdAt)}`} actions={<OrderStatusBadge status={order.status} />} />{mutation.error?.status === 409 && <Alert className="bg-warning-surface text-warning">El estado cambió en otra sesión. Actualizamos la orden.</Alert>}<div className="grid gap-6 lg:grid-cols-[1fr_22rem]"><Card><CardHeader><CardTitle>Artículos</CardTitle></CardHeader><CardContent><Table><TableHeader><TableRow><TableHead>Producto</TableHead><TableHead>Cantidad</TableHead><TableHead>Precio</TableHead><TableHead>Subtotal</TableHead></TableRow></TableHeader><TableBody>{order.items.map((item) => <TableRow key={item.productId}><TableCell>{item.productName}</TableCell><TableCell>{item.quantity}</TableCell><TableCell>{formatMoney(item.unitPrice)}</TableCell><TableCell>{formatMoney(item.subtotal)}</TableCell></TableRow>)}</TableBody></Table><div className="mt-4 flex justify-end text-lg"><strong>Total confirmado: {formatMoney(order.total)}</strong></div></CardContent></Card><div className="space-y-6"><Card><CardHeader><CardTitle>Cliente</CardTitle></CardHeader><CardContent><p className="font-medium">{order.customer.name}</p><p className="text-sm text-muted-foreground">{order.customer.email}</p></CardContent></Card>{order.status === 'CREATED' && <Card><CardHeader><CardTitle>Acciones</CardTitle></CardHeader><CardContent className="flex flex-col gap-2"><ConfirmActionDialog trigger={<Button>Completar</Button>} title="Completar orden" description="Esta acción lleva la orden a un estado terminal." actionText="Completar" pending={mutation.isPending} onConfirm={() => mutation.mutate('COMPLETED')} /><ConfirmActionDialog trigger={<Button variant="destructive">Cancelar</Button>} title="Cancelar orden" description="Esta acción no puede revertirse." actionText="Cancelar" destructive pending={mutation.isPending} onConfirm={() => mutation.mutate('CANCELLED')} /></CardContent></Card>}</div></div><OrderActivity orderId={order.id} /></div>
}

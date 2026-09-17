import { zodResolver } from '@hookform/resolvers/zod'
import { LosslessNumber } from 'lossless-json'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState, ErrorState, FormFieldError, LoadingState, PageHeader, PaginationControls, SubmitButton } from '@/components/core'
import { Badge, Button, Card, CardContent, Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger, Input, Label, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Textarea } from '@/components/ui'
import { formatDate, formatMoney } from '@/lib/format'
import { request, type ApiError } from '@/lib/http'
import { decimalToCents } from '@/lib/http/codecs'
import { pageSchema, productSchema, type Product } from '@/lib/types/contracts'

const priceSchema = z.string().trim().transform((value) => value.replace(',', '.')).refine((value) => /^\d+(?:\.\d{1,2})?$/.test(value), 'Usa hasta dos decimales').refine((value) => { try { const cents = decimalToCents(value); return cents >= 1 && cents <= 99_999_999 } catch { return false } }, 'El precio debe estar entre 0.01 y 999999.99')
const formSchema = z.object({ name: z.string().trim().min(1, 'Escribe un nombre').max(120), description: z.string().trim().max(1000), price: priceSchema, active: z.boolean() })
type FormValues = z.input<typeof formSchema>
export const productKeys = { all: ['products'] as const, list: (page: number, size: number, active?: boolean) => ['products', 'list', { page, size, active }] as const, detail: (id: string) => ['products', 'detail', id] as const }
export const useProducts = (page: number, size: number, active?: boolean) => useQuery({ queryKey: productKeys.list(page, size, active), queryFn: ({ signal }) => request(`/products?page=${page}&size=${size}${active === undefined ? '' : `&active=${active}`}`, pageSchema(productSchema), { signal }), staleTime: 15_000, placeholderData: keepPreviousData })

export function CreateProductDialog() {
  const [open, setOpen] = useState(false)
  const client = useQueryClient()
  const form = useForm<FormValues>({ resolver: zodResolver(formSchema), defaultValues: { name: '', description: '', price: '', active: true }, mode: 'onBlur' })
  const mutation = useMutation({
    mutationFn: (raw: FormValues) => { const values = formSchema.parse(raw); return request('/products', productSchema, { method: 'POST', body: { name: values.name.trim(), description: values.description.trim() || null, price: new LosslessNumber(values.price), active: values.active } as unknown as BodyInit }) },
    retry: false,
    onSuccess: () => { client.invalidateQueries({ queryKey: productKeys.all }); form.reset(); setOpen(false) },
  })
  const close = (next: boolean) => { if (!next && form.formState.isDirty && !mutation.isPending && !window.confirm('¿Descartar los cambios?')) return; setOpen(next) }
  return <Dialog open={open} onOpenChange={close}><DialogTrigger asChild><Button>Crear producto</Button></DialogTrigger><DialogContent><DialogHeader><DialogTitle>Crear producto</DialogTitle><DialogDescription>Define el precio que el backend guardará en nuevas órdenes.</DialogDescription></DialogHeader><form className="space-y-4" onSubmit={form.handleSubmit((value) => mutation.mutate(value))}><div className="space-y-1"><Label htmlFor="product-name">Nombre</Label><Input id="product-name" {...form.register('name')} disabled={mutation.isPending} /><FormFieldError id="product-name-error" message={form.formState.errors.name?.message} /></div><div className="space-y-1"><Label htmlFor="product-description">Descripción (opcional)</Label><Textarea id="product-description" {...form.register('description')} disabled={mutation.isPending} /><FormFieldError id="product-description-error" message={form.formState.errors.description?.message} /></div><div className="space-y-1"><Label htmlFor="product-price">Precio USD</Label><Input id="product-price" inputMode="decimal" placeholder="75.00" {...form.register('price')} disabled={mutation.isPending} /><FormFieldError id="product-price-error" message={form.formState.errors.price?.message} /></div><label className="flex items-center gap-2 text-sm"><input type="checkbox" {...form.register('active')} disabled={mutation.isPending} /> Activo para nuevas órdenes</label>{mutation.error && <ErrorState message={(mutation.error as ApiError).message} />}<div className="flex justify-end"><SubmitButton pending={mutation.isPending}>Guardar producto</SubmitButton></div></form></DialogContent></Dialog>
}

export function ProductDetailDialog({ id, children }: { id: string; children: React.ReactNode }) {
  const [open, setOpen] = useState(false)
  const query = useQuery({ queryKey: productKeys.detail(id), queryFn: ({ signal }) => request(`/products/${id}`, productSchema, { signal }), enabled: open, retry: false })
  return <Dialog open={open} onOpenChange={setOpen}><DialogTrigger asChild>{children}</DialogTrigger><DialogContent><DialogHeader><DialogTitle>Detalle de producto</DialogTitle><DialogDescription>Información actual del catálogo.</DialogDescription></DialogHeader>{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} /> : query.data && <dl className="grid gap-3 sm:grid-cols-2"><div><dt className="text-sm text-muted-foreground">Nombre</dt><dd className="font-medium">{query.data.name}</dd></div><div><dt className="text-sm text-muted-foreground">Precio</dt><dd>{formatMoney(query.data.price)}</dd></div><div className="sm:col-span-2"><dt className="text-sm text-muted-foreground">Descripción</dt><dd>{query.data.description || 'Sin descripción'}</dd></div><div><dt className="text-sm text-muted-foreground">Estado</dt><dd>{query.data.active ? 'Activo' : 'Inactivo'}</dd></div><div><dt className="text-sm text-muted-foreground">Creado</dt><dd>{formatDate(query.data.createdAt)}</dd></div></dl>}</DialogContent></Dialog>
}

export function ProductSelector({ selectedIds, onSelect, disabled }: { selectedIds: string[]; onSelect: (product: Product) => void; disabled?: boolean }) {
  const [open, setOpen] = useState(false)
  const [page, setPage] = useState(0)
  const query = useProducts(page, 10, true)
  return <><Button type="button" variant="outline" disabled={disabled} onClick={() => setOpen(true)}>Agregar producto</Button><Dialog open={open} onOpenChange={setOpen}><DialogContent><DialogHeader><DialogTitle>Agregar producto activo</DialogTitle><DialogDescription>La selección se conserva al cambiar de página.</DialogDescription></DialogHeader>{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title="No hay productos activos" description="Agrega un producto activo para continuar." /> : <div className="space-y-4"><div className="space-y-2">{query.data.items.map((product) => { const selected = selectedIds.includes(product.id); return <div key={product.id} className="flex items-center justify-between gap-3 rounded-md border p-3"><div><p className="font-medium">{product.name}</p><p className="text-sm text-muted-foreground">{formatMoney(product.price)}</p></div><Button size="sm" disabled={selected} onClick={() => onSelect(product)}>{selected ? 'Agregado' : 'Seleccionar'}</Button></div> })}</div><PaginationControls {...query.data} onPageChange={setPage} /></div>}</DialogContent></Dialog></>
}

export function ProductsPage() {
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0), size = Number(params.get('size') ?? 20), filter = params.get('active') ?? 'all'
  const query = useProducts(page, size, filter === 'all' ? undefined : filter === 'true')
  const update = (changes: Record<string, string>) => setParams((current) => { const next = new URLSearchParams(current); Object.entries(changes).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key)); return next })
  return <div className="space-y-6"><PageHeader title="Productos" description="Catálogo y precios disponibles." actions={<CreateProductDialog />} /><div className="flex gap-2" role="group" aria-label="Filtrar productos">{[['all','Todos'],['true','Activos'],['false','Inactivos']].map(([value,label]) => <Button key={value} size="sm" variant={filter === value ? 'default' : 'outline'} onClick={() => update({ page: '0', active: value === 'all' ? '' : value })}>{label}</Button>)}</div><Card><CardContent className="pt-6">{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={(query.error as ApiError).message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title="Sin resultados" description="No hay productos para este filtro." /> : <div className="space-y-4"><Table><TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Precio</TableHead><TableHead>Estado</TableHead><TableHead><span className="sr-only">Acciones</span></TableHead></TableRow></TableHeader><TableBody>{query.data.items.map((product) => <TableRow key={product.id}><TableCell className="font-medium">{product.name}</TableCell><TableCell>{formatMoney(product.price)}</TableCell><TableCell><Badge className={product.active ? 'bg-success-surface text-success' : ''}>{product.active ? 'Activo' : 'Inactivo'}</Badge></TableCell><TableCell><ProductDetailDialog id={product.id}><Button variant="ghost" size="sm">Ver detalle</Button></ProductDetailDialog></TableCell></TableRow>)}</TableBody></Table><PaginationControls {...query.data} onPageChange={(next) => update({ page: String(next) })} onSizeChange={(next) => update({ page: '0', size: String(next) })} /></div>}</CardContent></Card></div>
}

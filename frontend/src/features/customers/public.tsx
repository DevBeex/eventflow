import { zodResolver } from '@hookform/resolvers/zod'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState, ErrorState, FormFieldError, LoadingState, PageHeader, PaginationControls, SubmitButton } from '@/components/core'
import { Button, Card, CardContent, Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger, Input, Label, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui'
import { formatDate } from '@/lib/format'
import { request, type ApiError } from '@/lib/http'
import { idToJson } from '@/lib/http/codecs'
import { customerSchema, pageSchema, type Customer } from '@/lib/types/contracts'

const formSchema = z.object({ name: z.string().trim().min(1, 'Escribe un nombre').max(120), email: z.string().trim().max(254).email('Escribe un correo válido') })
type FormValues = z.infer<typeof formSchema>
export const customerKeys = { all: ['customers'] as const, list: (page: number, size: number) => ['customers', 'list', { page, size }] as const }
export const useCustomers = (page: number, size: number) => useQuery({ queryKey: customerKeys.list(page, size), queryFn: ({ signal }) => request(`/customers?page=${page}&size=${size}`, pageSchema(customerSchema), { signal }), staleTime: 15_000, placeholderData: keepPreviousData })

export function CreateCustomerDialog({ trigger }: { trigger?: React.ReactNode }) {
  const [open, setOpen] = useState(false)
  const queryClient = useQueryClient()
  const form = useForm<FormValues>({ resolver: zodResolver(formSchema), defaultValues: { name: '', email: '' }, mode: 'onBlur' })
  const mutation = useMutation({
    mutationFn: (values: FormValues) => request('/customers', customerSchema, { method: 'POST', body: { name: values.name.trim(), email: values.email.trim().toLowerCase() } as unknown as BodyInit }),
    retry: false,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: customerKeys.all }); form.reset(); setOpen(false) },
    onError: (error: ApiError) => { if (error.code === 'DUPLICATE_CUSTOMER_EMAIL') form.setError('email', { message: error.message }) },
  })
  const close = (next: boolean) => { if (!next && form.formState.isDirty && !mutation.isPending && !window.confirm('¿Descartar los cambios?')) return; setOpen(next) }
  return <Dialog open={open} onOpenChange={close}><DialogTrigger asChild>{trigger ?? <Button>Crear cliente</Button>}</DialogTrigger><DialogContent onEscapeKeyDown={(event) => mutation.isPending && event.preventDefault()}><DialogHeader><DialogTitle>Crear cliente</DialogTitle><DialogDescription>Registra el cliente que realizará órdenes.</DialogDescription></DialogHeader><form className="space-y-4" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}><div className="space-y-1.5"><Label htmlFor="customer-name">Nombre</Label><Input id="customer-name" autoFocus {...form.register('name')} aria-invalid={Boolean(form.formState.errors.name)} aria-describedby="customer-name-error" disabled={mutation.isPending} /><FormFieldError id="customer-name-error" message={form.formState.errors.name?.message} /></div><div className="space-y-1.5"><Label htmlFor="customer-email">Correo</Label><Input id="customer-email" type="email" {...form.register('email')} aria-invalid={Boolean(form.formState.errors.email)} aria-describedby="customer-email-error" disabled={mutation.isPending} /><FormFieldError id="customer-email-error" message={form.formState.errors.email?.message} /></div>{mutation.error && mutation.error.code !== 'DUPLICATE_CUSTOMER_EMAIL' && <ErrorState message={mutation.error.message} />}<div className="flex justify-end"><SubmitButton pending={mutation.isPending}>Guardar cliente</SubmitButton></div></form></DialogContent></Dialog>
}

export function CustomerSelector({ value, onSelect, disabled }: { value?: Customer; onSelect: (customer: Customer) => void; disabled?: boolean }) {
  const [open, setOpen] = useState(false)
  const [page, setPage] = useState(0)
  const query = useCustomers(page, 10)
  return <div className="space-y-2"><Label>Cliente</Label><Button type="button" variant="outline" className="w-full justify-start" disabled={disabled} onClick={() => setOpen(true)}>{value ? `${value.name} · ${value.email}` : 'Seleccionar cliente'}</Button><Dialog open={open} onOpenChange={setOpen}><DialogContent><DialogHeader><DialogTitle>Seleccionar cliente</DialogTitle><DialogDescription>Explora todas las páginas y elige un registro.</DialogDescription></DialogHeader>{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={query.error.message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title="No hay clientes" description="Crea un cliente para continuar." action={<CreateCustomerDialog trigger={<Button>Crear cliente</Button>} />} /> : <div className="space-y-4"><div className="space-y-2">{query.data.items.map((customer) => <div key={customer.id} className="flex items-center justify-between gap-3 rounded-md border p-3"><div><p className="font-medium">{customer.name}</p><p className="text-sm text-muted-foreground">{customer.email}</p></div><Button size="sm" variant={value?.id === customer.id ? 'secondary' : 'default'} onClick={() => { onSelect(customer); setOpen(false) }}>{value?.id === customer.id ? 'Seleccionado' : 'Seleccionar'}</Button></div>)}</div><PaginationControls {...query.data} onPageChange={setPage} /></div>}</DialogContent></Dialog></div>
}

export function CustomersPage() {
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0)
  const size = Number(params.get('size') ?? 20)
  const query = useCustomers(page, size)
  const update = (changes: Record<string, string>) => setParams((current) => { const next = new URLSearchParams(current); Object.entries(changes).forEach(([key, value]) => next.set(key, value)); return next })
  return <div className="space-y-6"><PageHeader title="Clientes" description="Clientes registrados para crear órdenes." actions={<CreateCustomerDialog />} /><Card><CardContent className="pt-6">{query.isLoading ? <LoadingState /> : query.isError ? <ErrorState message={query.error.message} onRetry={() => query.refetch()} /> : query.data.items.length === 0 ? <EmptyState title="No hay clientes" description="Crea el primero para comenzar." /> : <div className="space-y-4"><Table><TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Correo</TableHead><TableHead>Creado</TableHead></TableRow></TableHeader><TableBody>{query.data.items.map((customer) => <TableRow key={customer.id}><TableCell className="font-medium">{customer.name}</TableCell><TableCell>{customer.email}</TableCell><TableCell>{formatDate(customer.createdAt)}</TableCell></TableRow>)}</TableBody></Table><PaginationControls {...query.data} onPageChange={(next) => update({ page: String(next) })} onSizeChange={(next) => update({ page: '0', size: String(next) })} /></div>}</CardContent></Card><p className="text-xs text-muted-foreground">Las fechas se muestran en la zona horaria de tu navegador.</p></div>
}

export const customerIdBody = (id: string) => idToJson(id)

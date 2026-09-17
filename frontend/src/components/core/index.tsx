import { Check, Clipboard, Inbox, LoaderCircle, TriangleAlert } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Alert, AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogTitle, AlertDialogTrigger, Button, Skeleton } from '@/components/ui'

export function PageHeader({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) {
  return <header className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><h1 className="text-2xl font-bold tracking-tight">{title}</h1>{description && <p className="mt-1 text-muted-foreground">{description}</p>}</div>{actions}</header>
}
export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="flex flex-col items-center gap-3 py-12 text-center"><Inbox className="size-8 text-muted-foreground" /><div><h2 className="font-semibold">{title}</h2><p className="text-sm text-muted-foreground">{description}</p></div>{action}</div>
}
export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <Alert className="bg-danger-surface text-destructive"><div className="flex items-start gap-3"><TriangleAlert className="size-5 shrink-0" /><div><p>{message}</p>{onRetry && <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>Reintentar</Button>}</div></div></Alert>
}
export function LoadingState({ label = 'Cargando' }: { label?: string }) {
  return <div role="status" aria-label={label} className="space-y-3"><Skeleton className="h-12 w-full" /><Skeleton className="h-24 w-full" /><span className="sr-only">{label}</span></div>
}
export function PaginationControls({ page = 0, size = 20, totalItems = 0, totalPages = 0, onPageChange, onSizeChange }: { page?: number; size?: number; totalItems?: number; totalPages?: number; onPageChange: (page: number) => void; onSizeChange?: (size: number) => void }) {
  return <div className="flex flex-wrap items-center justify-between gap-3 text-sm"><span>{totalItems} registros · Página {page + 1} de {Math.max(totalPages, 1)}</span><div className="flex items-center gap-2">{onSizeChange && <select aria-label="Tamaño de página" className="h-9 rounded-md border border-input bg-background px-2" value={size} onChange={(event) => onSizeChange(Number(event.target.value))}><option value="10">10</option><option value="20">20</option><option value="50">50</option></select>}<Button size="sm" variant="outline" disabled={page === 0} onClick={() => onPageChange(page - 1)}>Anterior</Button><Button size="sm" variant="outline" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Siguiente</Button></div></div>
}
export function SubmitButton({ pending, children, ...props }: { pending: boolean; children: ReactNode } & React.ComponentProps<typeof Button>) {
  return <Button type="submit" disabled={pending || props.disabled} aria-busy={pending} {...props}>{pending && <LoaderCircle className="size-4 animate-spin" />}{pending ? 'Guardando…' : children}</Button>
}
export function ConfirmActionDialog({ trigger, title, description, actionText, pending, destructive, onConfirm }: { trigger: ReactNode; title: string; description: string; actionText: string; pending: boolean; destructive?: boolean; onConfirm: () => void }) {
  return <AlertDialog><AlertDialogTrigger asChild>{trigger}</AlertDialogTrigger><AlertDialogContent><AlertDialogTitle className="text-lg font-semibold">{title}</AlertDialogTitle><AlertDialogDescription className="mt-2 text-sm text-muted-foreground">{description}</AlertDialogDescription><div className="mt-6 flex justify-end gap-2"><AlertDialogCancel asChild><Button variant="outline" disabled={pending}>Volver</Button></AlertDialogCancel><AlertDialogAction asChild><Button variant={destructive ? 'destructive' : 'default'} disabled={pending} onClick={onConfirm}>{pending ? 'Actualizando…' : actionText}</Button></AlertDialogAction></div></AlertDialogContent></AlertDialog>
}
export function FormFieldError({ id, message }: { id: string; message?: string }) {
  return message ? <p id={id} className="text-sm text-destructive">{message}</p> : null
}
export function CopyValue({ value, label }: { value: string; label: string }) {
  const [copied, setCopied] = useState(false)
  const copy = async () => { try { await navigator.clipboard.writeText(value); setCopied(true); window.setTimeout(() => setCopied(false), 1500) } catch { setCopied(false) } }
  return <div className="flex items-center gap-2"><code className="min-w-0 break-all rounded bg-muted px-2 py-1 text-sm">{value}</code><Button size="icon" variant="outline" aria-label={label} onClick={copy}>{copied ? <Check className="size-4" /> : <Clipboard className="size-4" />}</Button><span className="sr-only" aria-live="polite">{copied ? 'Copiado' : ''}</span></div>
}

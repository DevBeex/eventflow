import * as React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import * as AlertDialogPrimitive from '@radix-ui/react-alert-dialog'
import * as SelectPrimitive from '@radix-ui/react-select'
import * as TooltipPrimitive from '@radix-ui/react-tooltip'
import { cva, type VariantProps } from 'class-variance-authority'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'

const buttonVariants = cva('inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-ring', {
  variants: {
    variant: {
      default: 'bg-primary text-primary-foreground hover:bg-primary-hover',
      secondary: 'bg-secondary text-secondary-foreground hover:bg-muted',
      outline: 'border bg-background hover:bg-accent hover:text-accent-foreground',
      ghost: 'hover:bg-accent hover:text-accent-foreground',
      destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive-hover',
    },
    size: { default: 'h-10 px-4 py-2', sm: 'h-9 px-3', icon: 'size-10' },
  },
  defaultVariants: { variant: 'default', size: 'default' },
})
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(({ className, variant, size, ...props }, ref) => <button ref={ref} className={cn(buttonVariants({ variant, size }), className)} {...props} />)
Button.displayName = 'Button'

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(({ className, ...props }, ref) => <input ref={ref} className={cn('h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50', className)} {...props} />)
Input.displayName = 'Input'
export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(({ className, ...props }, ref) => <textarea ref={ref} className={cn('min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-ring', className)} {...props} />)
Textarea.displayName = 'Textarea'
export const Label = ({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) => <label className={cn('text-sm font-medium', className)} {...props} />
export const Card = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div className={cn('rounded-lg border bg-card text-card-foreground shadow-[var(--shadow-card)]', className)} {...props} />
export const CardHeader = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div className={cn('p-6 pb-3', className)} {...props} />
export const CardTitle = ({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) => <h2 className={cn('text-lg font-semibold', className)} {...props} />
export const CardContent = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div className={cn('p-6 pt-3', className)} {...props} />
export const Badge = ({ className, ...props }: React.HTMLAttributes<HTMLSpanElement>) => <span className={cn('inline-flex rounded-full bg-muted px-2.5 py-1 text-xs font-medium', className)} {...props} />
export const Alert = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div role="alert" className={cn('rounded-md border bg-card p-4 text-sm', className)} {...props} />
export const Skeleton = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div aria-hidden className={cn('animate-pulse rounded-md bg-muted', className)} {...props} />
export const Table = ({ className, ...props }: React.TableHTMLAttributes<HTMLTableElement>) => <div className="w-full overflow-x-auto"><table className={cn('w-full text-sm', className)} {...props} /></div>
export const TableHeader = (props: React.HTMLAttributes<HTMLTableSectionElement>) => <thead className="border-b" {...props} />
export const TableBody = (props: React.HTMLAttributes<HTMLTableSectionElement>) => <tbody className="divide-y" {...props} />
export const TableRow = ({ className, ...props }: React.HTMLAttributes<HTMLTableRowElement>) => <tr className={cn('hover:bg-muted/60', className)} {...props} />
export const TableHead = (props: React.ThHTMLAttributes<HTMLTableCellElement>) => <th className="h-11 px-3 text-left font-medium text-muted-foreground" {...props} />
export const TableCell = (props: React.TdHTMLAttributes<HTMLTableCellElement>) => <td className="px-3 py-3" {...props} />
export const Separator = (props: React.HTMLAttributes<HTMLHRElement>) => <hr className="border-border" {...props} />

export const Dialog = DialogPrimitive.Root
export const DialogTrigger = DialogPrimitive.Trigger
export const DialogClose = DialogPrimitive.Close
export const DialogContent = ({ className, children, ...props }: React.ComponentProps<typeof DialogPrimitive.Content>) => <DialogPrimitive.Portal><DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-foreground/30" /><DialogPrimitive.Content className={cn('fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-lg border bg-popover p-6 shadow-lg', className)} {...props}>{children}<DialogPrimitive.Close className="absolute right-4 top-4 rounded-md p-1" aria-label="Cerrar"><X className="size-4" /></DialogPrimitive.Close></DialogPrimitive.Content></DialogPrimitive.Portal>
export const DialogHeader = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div className={cn('mb-4 space-y-1', className)} {...props} />
export const DialogTitle = DialogPrimitive.Title
export const DialogDescription = DialogPrimitive.Description

export const AlertDialog = AlertDialogPrimitive.Root
export const AlertDialogTrigger = AlertDialogPrimitive.Trigger
export const AlertDialogCancel = AlertDialogPrimitive.Cancel
export const AlertDialogAction = AlertDialogPrimitive.Action
export const AlertDialogContent = ({ children, ...props }: React.ComponentProps<typeof AlertDialogPrimitive.Content>) => <AlertDialogPrimitive.Portal><AlertDialogPrimitive.Overlay className="fixed inset-0 z-40 bg-foreground/30" /><AlertDialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-lg border bg-popover p-6 shadow-lg" {...props}>{children}</AlertDialogPrimitive.Content></AlertDialogPrimitive.Portal>
export const AlertDialogTitle = AlertDialogPrimitive.Title
export const AlertDialogDescription = AlertDialogPrimitive.Description

export const Select = SelectPrimitive.Root
export const SelectValue = SelectPrimitive.Value
export const SelectTrigger = ({ className, children, ...props }: React.ComponentProps<typeof SelectPrimitive.Trigger>) => <SelectPrimitive.Trigger className={cn('flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 text-sm', className)} {...props}>{children}</SelectPrimitive.Trigger>
export const SelectContent = ({ children, ...props }: React.ComponentProps<typeof SelectPrimitive.Content>) => <SelectPrimitive.Portal><SelectPrimitive.Content className="z-50 overflow-hidden rounded-md border bg-popover shadow-lg" {...props}><SelectPrimitive.Viewport className="p-1">{children}</SelectPrimitive.Viewport></SelectPrimitive.Content></SelectPrimitive.Portal>
export const SelectItem = ({ className, children, ...props }: React.ComponentProps<typeof SelectPrimitive.Item>) => <SelectPrimitive.Item className={cn('cursor-pointer rounded px-3 py-2 text-sm outline-none focus:bg-accent', className)} {...props}><SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText></SelectPrimitive.Item>
export const TooltipProvider = TooltipPrimitive.Provider
export const Tooltip = TooltipPrimitive.Root
export const TooltipTrigger = TooltipPrimitive.Trigger
export const TooltipContent = ({ className, ...props }: React.ComponentProps<typeof TooltipPrimitive.Content>) => <TooltipPrimitive.Portal><TooltipPrimitive.Content className={cn('z-50 rounded-md bg-foreground px-2 py-1 text-xs text-background', className)} {...props} /></TooltipPrimitive.Portal>
export const Sheet = DialogPrimitive.Root
export const SheetTrigger = DialogPrimitive.Trigger
export const SheetContent = ({ className, children, ...props }: React.ComponentProps<typeof DialogPrimitive.Content>) => <DialogPrimitive.Portal><DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-foreground/30" /><DialogPrimitive.Content className={cn('fixed inset-y-0 right-0 z-50 w-[85%] max-w-sm border-l bg-popover p-6 shadow-lg', className)} {...props}>{children}<DialogPrimitive.Close className="absolute right-4 top-4" aria-label="Cerrar"><X className="size-4" /></DialogPrimitive.Close></DialogPrimitive.Content></DialogPrimitive.Portal>

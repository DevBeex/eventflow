import { Menu, Workflow, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { Button } from '@/components/ui'
import { cn } from '@/lib/utils'
import { routes } from '@/lib/config/routes'

const links = [
  [routes.demo, 'Demo'],
  [routes.orders, 'Órdenes'],
  [routes.customers, 'Clientes'],
  [routes.products, 'Productos'],
  [routes.notifications, 'Notificaciones'],
] as const

function Navigation({
  orientation,
  onNavigate,
}: {
  orientation: 'horizontal' | 'vertical'
  onNavigate?: () => void
}) {
  return (
    <nav
      aria-label="Principal"
      className={cn(
        'gap-1',
        orientation === 'horizontal' ? 'flex flex-row items-center' : 'flex flex-col',
      )}
    >
      {links.map(([to, label]) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              'rounded-md px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-accent hover:text-accent-foreground',
              isActive && 'bg-accent text-accent-foreground',
            )
          }
        >
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export function AppShell() {
  const [open, setOpen] = useState(false)

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b bg-card">
        <div className="mx-auto flex h-16 max-w-[var(--content-max-width)] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <NavLink to={routes.demo} className="flex shrink-0 items-center gap-2 font-bold">
            <Workflow className="size-6 text-primary" />
            EventFlow
          </NavLink>
          <div className="hidden md:block">
            <Navigation orientation="horizontal" />
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            aria-label={open ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            {open ? <X /> : <Menu />}
          </Button>
        </div>
        {open ? (
          <div className="border-t bg-card px-4 py-3 md:hidden">
            <Navigation orientation="vertical" onNavigate={() => setOpen(false)} />
          </div>
        ) : null}
      </header>
      <main className="mx-auto max-w-[var(--content-max-width)] px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  )
}

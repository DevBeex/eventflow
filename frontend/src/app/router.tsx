import { lazy, Suspense } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import { EmptyState, LoadingState } from '@/components/core'
import { Button } from '@/components/ui'
import { AppShell } from '@/app/layout/app-shell'
import { routes } from '@/lib/config/routes'

const DemoPage = lazy(() => import('@/pages/demo/demo-page'))
const CustomersPage = lazy(() => import('@/features/customers/public').then((module) => ({ default: module.CustomersPage })))
const ProductsPage = lazy(() => import('@/features/products/public').then((module) => ({ default: module.ProductsPage })))
const OrdersPage = lazy(() => import('@/features/orders/public').then((module) => ({ default: module.OrdersPage })))
const OrderDetailPage = lazy(() => import('@/features/orders/public').then((module) => ({ default: module.OrderDetailPage })))
const NotificationsPage = lazy(() => import('@/features/notifications/public').then((module) => ({ default: module.NotificationsPage })))
const NotificationDetailPage = lazy(() => import('@/features/notifications/public').then((module) => ({ default: module.NotificationDetailPage })))
const load = (node: React.ReactNode) => <Suspense fallback={<LoadingState label="Cargando página" />}>{node}</Suspense>

export const router = createBrowserRouter([{
  element: <AppShell />,
  children: [
    { index: true, element: <Navigate to={routes.demo} replace /> },
    { path: 'demo', element: load(<DemoPage />) },
    { path: 'orders', element: load(<OrdersPage />) },
    { path: 'orders/:id', element: load(<OrderDetailPage />) },
    { path: 'customers', element: load(<CustomersPage />) },
    { path: 'products', element: load(<ProductsPage />) },
    { path: 'notifications', element: load(<NotificationsPage />) },
    { path: 'notifications/:id', element: load(<NotificationDetailPage />) },
    { path: '*', element: <EmptyState title="Página no encontrada" description="La ruta solicitada no existe." action={<Button onClick={() => window.location.assign(routes.demo)}>Volver a la demo</Button>} /> },
  ],
}])

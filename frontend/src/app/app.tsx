import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RouterProvider } from 'react-router-dom'
import { ErrorState } from '@/components/core'
import { Providers } from '@/app/providers'
import { router } from '@/app/router'

class AppErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  componentDidCatch(error: Error, info: ErrorInfo) { console.error(error, info) }
  render() { return this.state.failed ? <main className="mx-auto max-w-2xl p-8"><ErrorState message="La aplicación encontró un error inesperado." onRetry={() => window.location.reload()} /></main> : this.props.children }
}
export function App() {
  return <AppErrorBoundary><Providers><RouterProvider router={router} /></Providers></AppErrorBoundary>
}

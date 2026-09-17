import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { getRetry } from '@/lib/http'

export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { retry: getRetry, staleTime: 15_000, refetchOnWindowFocus: true }, mutations: { retry: false } } }))
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

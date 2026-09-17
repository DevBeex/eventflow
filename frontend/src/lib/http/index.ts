import { isLosslessNumber, parse, stringify } from 'lossless-json'
import type { ZodType } from 'zod'

export interface ApiError {
  status: number
  code: string
  message: string
  fieldErrors: Record<string, string>
  uncertain?: boolean
}

declare module '@tanstack/react-query' {
  interface Register {
    defaultError: ApiError
  }
}

const messages: Record<string, string> = {
  VALIDATION_ERROR: 'Revisa los datos ingresados.',
  DUPLICATE_CUSTOMER_EMAIL: 'Ya existe un cliente con este correo.',
  CUSTOMER_NOT_FOUND: 'El cliente ya no existe.',
  PRODUCT_NOT_FOUND: 'El producto ya no existe.',
  PRODUCT_INACTIVE: 'El producto ya no está activo.',
  ORDER_NOT_FOUND: 'La orden no existe.',
  NOTIFICATION_NOT_FOUND: 'La notificación no existe.',
  INVALID_ORDER_STATUS_TRANSITION: 'El estado de la orden cambió y la acción ya no es válida.',
  SERVICE_UNAVAILABLE: 'El servicio no está disponible temporalmente.',
  CONTRACT_ERROR: 'La respuesta del servidor no coincide con el contrato esperado.',
  NETWORK_ERROR: 'No pudimos conectar con el servidor.',
  TIMEOUT: 'La solicitud agotó el tiempo de espera.',
}

export function errorMessage(code: string) {
  return messages[code] ?? 'Ocurrió un error inesperado.'
}

function normalizeNumber(value: unknown): unknown {
  if (isLosslessNumber(value)) return value.toString()
  if (Array.isArray(value)) return value.map(normalizeNumber)
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, normalizeNumber(item)]))
  return value
}

function serializeReplacer(_key: string, value: unknown) {
  return typeof value === 'bigint' ? value.toString() : value
}

export async function request<T>(path: string, schema: ZodType<T>, options: RequestInit & { uncertainOnFailure?: boolean } = {}): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 15_000)
  const externalSignal = options.signal
  const abort = () => controller.abort()
  externalSignal?.addEventListener('abort', abort, { once: true })
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}${path}`, {
      ...options,
      body: options.body && typeof options.body !== 'string' ? stringify(options.body, serializeReplacer) : options.body,
      headers: options.body ? { 'Content-Type': 'application/json', ...options.headers } : options.headers,
      signal: controller.signal,
    })
    const text = await response.text()
    const raw = text ? normalizeNumber(parse(text)) : null
    if (!response.ok) {
      const body = raw as { error?: string; message?: string; details?: Array<{ field: string; message: string }> } | null
      const code = body?.error ?? `HTTP_${response.status}`
      throw { status: response.status, code, message: errorMessage(code), fieldErrors: Object.fromEntries((body?.details ?? []).map((item) => [item.field, item.message])) } satisfies ApiError
    }
    const result = schema.safeParse(raw)
    if (!result.success) throw { status: 500, code: 'CONTRACT_ERROR', message: errorMessage('CONTRACT_ERROR'), fieldErrors: {} } satisfies ApiError
    return result.data
  } catch (error) {
    if (isApiError(error)) throw error
    const timedOut = controller.signal.aborted && !externalSignal?.aborted
    const code = timedOut ? 'TIMEOUT' : 'NETWORK_ERROR'
    throw { status: 0, code, message: errorMessage(code), fieldErrors: {}, uncertain: Boolean(options.uncertainOnFailure) } satisfies ApiError
  } finally {
    window.clearTimeout(timeout)
    externalSignal?.removeEventListener('abort', abort)
  }
}

export function isApiError(error: unknown): error is ApiError {
  return Boolean(error && typeof error === 'object' && 'code' in error && 'fieldErrors' in error)
}

export const getRetry = (failureCount: number, error: ApiError) => failureCount < 1 && (error.status === 0 || [502, 503, 504].includes(error.status))

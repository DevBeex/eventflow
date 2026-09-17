import { LosslessNumber, stringify } from 'lossless-json'
import { describe, expect, it } from 'vitest'
import { errorMessage } from '@/lib/http'
import { centsToDecimal, decimalToCents, idSchema, idToJson } from '@/lib/http/codecs'
import { buildOrderPayload, estimateOrderCents } from '@/features/orders/public'
import { shouldContinuePolling } from '@/features/notifications/public'
import type { Customer, Product } from '@/lib/types/contracts'

const customer: Customer = { id: '9007199254740993', name: 'Ana', email: 'ana@example.com', createdAt: '', updatedAt: '' }
const product = (id: string, price: string): Product => ({ id, name: `Producto ${id}`, description: null, price, currency: 'USD', active: true, createdAt: '', updatedAt: '' })

describe('codecs exactos', () => {
  it('conserva IDs int64 fuera del rango seguro de JavaScript', () => {
    expect(idSchema.parse('9007199254740993')).toBe('9007199254740993')
    expect(stringify({ id: idToJson('9007199254740993') })).toBe('{"id":9007199254740993}')
    expect(() => idToJson('9223372036854775808')).toThrow()
  })
  it('convierte dinero sin multiplicar floats', () => {
    expect(decimalToCents('75.00')).toBe(7500)
    expect(centsToDecimal(20_000)).toBe('200.00')
    expect(() => decimalToCents('1.501')).toThrow()
  })
})

describe('creación de orden', () => {
  it('calcula el estimado y construye exclusivamente el DTO permitido', () => {
    const draft = { customer, items: [{ product: product('10', '75.00'), quantity: 2 }, { product: product('20', '50.00'), quantity: 1 }] }
    expect(estimateOrderCents(draft.items)).toBe(20_000)
    const payload = buildOrderPayload(draft)
    expect(Object.keys(payload)).toEqual(['customerId', 'items'])
    expect(Object.keys(payload.items[0])).toEqual(['productId', 'quantity'])
    expect(payload.customerId).toBeInstanceOf(LosslessNumber)
  })
})

describe('errores y polling', () => {
  it('traduce códigos backend al español', () => {
    expect(errorMessage('PRODUCT_INACTIVE')).toContain('activo')
    expect(errorMessage('UNKNOWN')).toContain('inesperado')
  })
  it('detiene polling por resultado, error, visibilidad o plazo', () => {
    expect(shouldContinuePolling(31_000, false, false, false, 1_000)).toBe(true)
    expect(shouldContinuePolling(31_000, true, false, false, 1_000)).toBe(false)
    expect(shouldContinuePolling(31_000, false, true, false, 1_000)).toBe(false)
    expect(shouldContinuePolling(31_000, false, false, true, 1_000)).toBe(false)
    expect(shouldContinuePolling(31_000, false, false, false, 31_000)).toBe(false)
  })
})

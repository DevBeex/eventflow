import { LosslessNumber, stringify } from 'lossless-json'
import { describe, expect, it } from 'vitest'
import { errorMessage, parseApiJson } from '@/lib/http'
import { centsToDecimal, decimalSchema, decimalToCents, idSchema, idToJson, instantSchema, integerSchema } from '@/lib/http/codecs'
import { buildOrderPayload, estimateOrderCents } from '@/features/orders/public'
import { shouldContinuePolling } from '@/features/notifications/public'
import { orderDetailSchema, pageSchema, orderSummarySchema, customerSchema, productSchema, notificationSchema, type Customer, type Product } from '@/lib/types/contracts'

const customer: Customer = {
  id: '9007199254740993',
  name: 'Ana',
  email: 'ana@example.com',
  createdAt: '2026-09-17T21:23:41.097382Z',
  updatedAt: '2026-09-17T21:23:41.097382Z',
}
const product = (id: string, price: string): Product => ({
  id,
  name: `Producto ${id}`,
  description: null,
  price,
  currency: 'USD',
  active: true,
  createdAt: '2026-09-17T21:23:41.097382Z',
  updatedAt: '2026-09-17T21:23:41.097382Z',
})

describe('codecs exactos', () => {
  it('conserva IDs int64 fuera del rango seguro de JavaScript', () => {
    expect(idSchema.parse('9007199254740993')).toBe('9007199254740993')
    expect(stringify({ id: idToJson('9007199254740993') })).toBe('{"id":9007199254740993}')
    expect(() => idToJson('9223372036854775808')).toThrow()
  })
  it('acepta LosslessNumber en IDs, enteros y dinero sin float', () => {
    expect(idSchema.parse(new LosslessNumber('9007199254740993'))).toBe('9007199254740993')
    expect(integerSchema.parse(new LosslessNumber('3'))).toBe(3)
    expect(decimalSchema.parse(new LosslessNumber('375.00'))).toBe('375.00')
    expect(decimalSchema.parse(new LosslessNumber('375'))).toBe('375.00')
    expect(decimalSchema.parse(new LosslessNumber('375.5'))).toBe('375.50')
  })
  it('acepta Instant de Quarkus/Jackson con o sin Z y fracciones', () => {
    expect(instantSchema.parse('2026-09-17T21:23:41.097382Z')).toBe('2026-09-17T21:23:41.097382Z')
    expect(instantSchema.parse('2026-09-17T21:23:41Z')).toBe('2026-09-17T21:23:41Z')
    expect(instantSchema.parse('2026-09-17T21:23:41.097382')).toBe('2026-09-17T21:23:41.097382')
    expect(instantSchema.parse('2026-09-17T21:23:41+00:00')).toBe('2026-09-17T21:23:41+00:00')
  })
  it('convierte dinero sin multiplicar floats', () => {
    expect(decimalToCents('75.00')).toBe(7500)
    expect(centsToDecimal(20_000)).toBe('200.00')
    expect(() => decimalToCents('1.501')).toThrow()
  })
})

describe('contratos de respuesta backend', () => {
  it('parsea OrderDetailResponse realista (números lossless + Instant)', () => {
    const wire = `{
      "id": 1,
      "customer": {
        "id": 1,
        "name": "Benji",
        "email": "benji@gmail.com",
        "createdAt": "2026-09-17T21:23:41.097382Z",
        "updatedAt": "2026-09-17T21:23:41.097382Z"
      },
      "status": "CREATED",
      "total": 375.00,
      "currency": "USD",
      "items": [
        {
          "productId": 2,
          "productName": "Widget",
          "quantity": 3,
          "unitPrice": 125.00,
          "subtotal": 375.00
        }
      ],
      "createdAt": "2026-09-17T21:23:41.097382Z",
      "updatedAt": "2026-09-17T21:23:41.097382"
    }`
    const parsed = orderDetailSchema.parse(parseApiJson(wire))
    expect(parsed.id).toBe('1')
    expect(parsed.customer.id).toBe('1')
    expect(parsed.total).toBe('375.00')
    expect(parsed.items[0]?.unitPrice).toBe('125.00')
    expect(parsed.items[0]?.quantity).toBe(3)
    expect(parsed.updatedAt).toBe('2026-09-17T21:23:41.097382')
  })

  it('parsea listados de órdenes, clientes, productos y notificaciones', () => {
    const ordersPage = `{
      "items": [{
        "id": 1,
        "customer": {
          "id": 1,
          "name": "Benji",
          "email": "benji@gmail.com",
          "createdAt": "2026-09-17T21:23:41.097382Z",
          "updatedAt": "2026-09-17T21:23:41.097382Z"
        },
        "status": "CREATED",
        "total": 375.00,
        "currency": "USD",
        "createdAt": "2026-09-17T21:23:41.097382Z",
        "updatedAt": "2026-09-17T21:23:41.097382Z"
      }],
      "page": 0,
      "size": 20,
      "totalItems": 1,
      "totalPages": 1
    }`
    expect(pageSchema(orderSummarySchema).parse(parseApiJson(ordersPage)).items[0]?.total).toBe('375.00')

    const customersPage = `{
      "items": [{
        "id": 9007199254740993,
        "name": "Ana",
        "email": "ana@example.com",
        "createdAt": "2026-09-17T21:23:41Z",
        "updatedAt": "2026-09-17T21:23:41Z"
      }],
      "page": 0, "size": 20, "totalItems": 1, "totalPages": 1
    }`
    expect(pageSchema(customerSchema).parse(parseApiJson(customersPage)).items[0]?.id).toBe('9007199254740993')

    const productsPage = `{
      "items": [{
        "id": 2,
        "name": "Widget",
        "description": null,
        "price": 125.00,
        "currency": "USD",
        "active": true,
        "createdAt": "2026-09-17T21:23:41.097382Z",
        "updatedAt": "2026-09-17T21:23:41.097382Z"
      }],
      "page": 0, "size": 20, "totalItems": 1, "totalPages": 1
    }`
    expect(pageSchema(productSchema).parse(parseApiJson(productsPage)).items[0]?.price).toBe('125.00')

    const notificationsPage = `{
      "items": [{
        "id": 5,
        "eventId": "550e8400-e29b-41d4-a716-446655440000",
        "orderId": 1,
        "type": "ORDER_CREATED",
        "message": "order created",
        "createdAt": "2026-09-17T21:23:41.097382Z"
      }],
      "page": 0, "size": 20, "totalItems": 1, "totalPages": 1
    }`
    expect(pageSchema(notificationSchema).parse(parseApiJson(notificationsPage)).items[0]?.orderId).toBe('1')
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

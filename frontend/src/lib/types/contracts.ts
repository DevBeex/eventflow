import { z } from 'zod'
import { decimalSchema, idSchema, integerSchema } from '@/lib/http/codecs'

export const customerSchema = z.object({ id: idSchema, name: z.string(), email: z.string(), createdAt: z.string(), updatedAt: z.string() }).strict()
export type Customer = z.infer<typeof customerSchema>
export const productSchema = z.object({ id: idSchema, name: z.string(), description: z.string().nullable(), price: decimalSchema, currency: z.literal('USD'), active: z.boolean(), createdAt: z.string(), updatedAt: z.string() }).strict()
export type Product = z.infer<typeof productSchema>
export const orderStatusSchema = z.enum(['CREATED', 'COMPLETED', 'CANCELLED'])
const customerRefSchema = z.object({ id: idSchema, name: z.string(), email: z.string() }).strict()
export const orderSummarySchema = z.object({ id: idSchema, customer: customerRefSchema, status: orderStatusSchema, total: decimalSchema, currency: z.literal('USD'), createdAt: z.string(), updatedAt: z.string() }).strict()
export type OrderSummary = z.infer<typeof orderSummarySchema>
export const orderDetailSchema = orderSummarySchema.extend({ items: z.array(z.object({ productId: idSchema, productName: z.string(), quantity: integerSchema, unitPrice: decimalSchema, subtotal: decimalSchema }).strict()) }).strict()
export type OrderDetail = z.infer<typeof orderDetailSchema>
export const notificationSchema = z.object({ id: idSchema, eventId: z.string().uuid(), orderId: idSchema, type: z.literal('ORDER_CREATED'), message: z.string(), createdAt: z.string() }).strict()
export type Notification = z.infer<typeof notificationSchema>
export function pageSchema<T extends z.ZodType>(item: T) {
  return z.object({ items: z.array(item), page: integerSchema, size: integerSchema, totalItems: integerSchema, totalPages: integerSchema }).strict()
}
export type PageResponse<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number }

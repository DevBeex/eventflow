import { LosslessNumber } from 'lossless-json'
import { z } from 'zod'

const MAX_ID = BigInt('9223372036854775807')
export const idSchema = z.string().regex(/^[1-9]\d*$/).refine((value) => BigInt(value) <= MAX_ID, 'ID fuera de rango')
export const integerSchema = z.union([z.number().int().safe(), z.string().regex(/^\d+$/).transform(Number)]).refine(Number.isSafeInteger)
export const decimalSchema = z.union([z.string(), z.number().transform((value) => value.toFixed(2))]).pipe(z.string().regex(/^\d+\.\d{2}$/))

export function idToJson(id: string) {
  idSchema.parse(id)
  return new LosslessNumber(id)
}
export function decimalToCents(value: string): number {
  const normalized = value.trim().replace(',', '.')
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) throw new Error('Importe inválido')
  const [whole, fraction = ''] = normalized.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  if (!Number.isSafeInteger(cents)) throw new Error('Importe fuera de rango')
  return cents
}
export function centsToDecimal(cents: number) {
  if (!Number.isSafeInteger(cents)) throw new Error('Importe fuera de rango')
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`
}

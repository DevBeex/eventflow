import { isLosslessNumber, LosslessNumber } from 'lossless-json'
import { z } from 'zod'

const MAX_ID = BigInt('9223372036854775807')

/** Coerce lossless-json numbers (and plain numbers) to exact decimal strings without float math. */
function asExactString(value: unknown): unknown {
  if (isLosslessNumber(value)) return value.toString()
  if (typeof value === 'bigint') return value.toString()
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return value
}

/** Normalize money wire values to a two-decimal string domain. */
function asMoneyString(value: unknown): unknown {
  const raw = asExactString(value)
  if (typeof raw !== 'string') return raw
  if (!/^\d+(?:\.\d+)?$/.test(raw)) return raw
  const [whole, fraction = ''] = raw.split('.')
  if (fraction.length > 2) return raw
  return `${whole}.${fraction.padEnd(2, '0')}`
}

export const idSchema = z.preprocess(
  asExactString,
  z.string().regex(/^[1-9]\d*$/).refine((value) => BigInt(value) <= MAX_ID, 'ID fuera de rango'),
)

export const integerSchema = z.preprocess(
  asExactString,
  z.union([z.number().int().safe(), z.string().regex(/^\d+$/).transform(Number)]).refine(Number.isSafeInteger),
)

export const decimalSchema = z.preprocess(
  asMoneyString,
  z.string().regex(/^\d+\.\d{2}$/),
)

/** Quarkus/Jackson Instant: optional fraction, optional Z or numeric offset. */
export const instantSchema = z.string().regex(
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})?$/,
)

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

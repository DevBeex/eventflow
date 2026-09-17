import { decimalToCents } from '@/lib/http/codecs'

export const formatMoney = (decimal: string) => {
  const cents = decimalToCents(decimal)
  return new Intl.NumberFormat('es-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(cents / 100)
}
export const formatDate = (value: string) => new Intl.DateTimeFormat('es', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

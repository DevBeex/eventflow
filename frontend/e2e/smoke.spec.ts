import { expect, test } from '@playwright/test'

test('abre la experiencia principal', async ({ page }) => {
  await page.goto('/demo')
  await expect(page.getByRole('heading', { name: /Crea una orden/ })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Crear orden' })).toBeVisible()
})

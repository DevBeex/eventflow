import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../src/', import.meta.url))
const violations = []
async function walk(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) await walk(path)
    else if (['.ts', '.tsx', '.css'].includes(extname(path)) && entry.name !== 'tokens.css') {
      const source = await readFile(path, 'utf8')
      const checks = [
        [/(?:#(?:[\da-f]{3,8})\b|rgba?\s*\(|hsla?\s*\(|oklch\s*\()/gi, 'color literal'],
        [/\b(?:bg|text|border|ring|fill|stroke)-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-\d{2,3}\b/g, 'palette utility'],
      ]
      for (const [pattern, label] of checks) if (pattern.test(source)) violations.push(`${relative(root, path)}: ${label}`)
    }
  }
}
await walk(root)
if (violations.length) {
  console.error(violations.join('\n'))
  process.exit(1)
}
console.log('Diseño validado: todos los colores usan tokens.')

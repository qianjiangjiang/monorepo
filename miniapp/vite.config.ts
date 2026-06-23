import { defineConfig } from 'vite'
import uniPlugin from '@dcloudio/vite-plugin-uni'

const uni = (
  uniPlugin as typeof uniPlugin & {
    default?: typeof uniPlugin
  }
).default ?? uniPlugin

export default defineConfig({
  plugins: [uni()],
})

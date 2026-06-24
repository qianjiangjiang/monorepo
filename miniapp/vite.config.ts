import { defineConfig } from 'vite'
import uniPlugin from '@dcloudio/vite-plugin-uni'

const uni = (
  uniPlugin as typeof uniPlugin & {
    default?: typeof uniPlugin
  }
).default ?? uniPlugin

export default defineConfig({
  plugins: [uni()],
  server: {
    // H5 开发环境：把同源的 /api 请求代理到后端，规避浏览器跨域(CORS)。
    // 仅作用于 vite dev server（H5），mp-weixin 编译不受影响。
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

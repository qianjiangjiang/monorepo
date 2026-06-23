import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import uviewPlus from 'uview-plus'
import 'uview-plus/index.scss'
import App from './App.vue'
import { setupRequestInterceptors } from './services/request'

export function createApp() {
  const app = createSSRApp(App)

  setupRequestInterceptors()
  app.use(createPinia())
  app.use(uviewPlus)

  return {
    app,
  }
}

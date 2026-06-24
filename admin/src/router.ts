import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from './api'

const EmptyView = { setup: () => () => null }

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: EmptyView,
      meta: { public: true },
    },
    {
      path: '/',
      name: 'console',
      component: EmptyView,
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const authed = Boolean(getToken())
  if (to.meta.requiresAuth && !authed) {
    return { name: 'login' }
  }
  if (to.name === 'login' && authed) {
    return { name: 'console' }
  }
  return true
})

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'FileManager',
    component: () => import('../views/FileManager.vue')
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})

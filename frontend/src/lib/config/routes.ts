export const routes = {
  demo: '/demo',
  orders: '/orders',
  order: (id: string) => `/orders/${id}`,
  customers: '/customers',
  products: '/products',
  notifications: '/notifications',
  notification: (id: string) => `/notifications/${id}`,
}

import axios from 'axios'
import { mockAdapter } from './mock'

export const MODO_MOCK = import.meta.env.VITE_MOCK === 'true'

const api = axios.create({ baseURL: '/api' })

if (MODO_MOCK) api.defaults.adapter = mockAdapter

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    const esAuth = err.config?.url?.startsWith('/auth/')
    if (err.response?.status === 401 && !esAuth) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export function mensajeError(err, porDefecto = 'Ocurrió un error, intenta nuevamente') {
  return err?.response?.data?.mensaje ?? porDefecto
}

export default api

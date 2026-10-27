import axios from 'axios'

// En local se usa el proxy de Vite (/api → localhost:8080).
// En Vercel se define VITE_API_URL con la URL pública del backend, por ejemplo https://mi-backend.com/api
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  // Evita la página de aviso de ngrok cuando el backend está detrás de un túnel (no afecta en local).
  headers: { 'ngrok-skip-browser-warning': '1' }
})

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

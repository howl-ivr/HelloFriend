import { createContext, useContext, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

function leerPayload(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const relleno = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    return JSON.parse(atob(relleno))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const payload = token ? leerPayload(token) : null

  const login = async (usuario, password) => {
    const { data } = await api.post('/auth/login', { usuario, password })
    localStorage.setItem('token', data.token)
    setToken(data.token)
    return leerPayload(data.token)
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } finally {
      localStorage.removeItem('token')
      setToken(null)
    }
  }

  return (
    <AuthContext.Provider
      value={{
        token: payload ? token : null,
        rol: payload?.rol ?? null,
        usuario: payload?.usuario ?? null,
        login,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)

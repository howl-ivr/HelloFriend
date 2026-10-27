import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/client'
import { ROLES } from '../utils/formato'
import { AvatarAnonimo } from './ui'

const ENLACES = {
  ESTUDIANTE: [
    { to: '/inicio', texto: 'Inicio' },
    { to: '/mis-incidencias', texto: 'Mis reportes' }
  ],
  PROFESOR: [{ to: '/inicio', texto: 'Inicio' }],
  DIRECTOR: [
    { to: '/director/pendientes', texto: 'Pendientes' },
    { to: '/director/publicadas', texto: 'Publicadas' },
    { to: '/director/estadisticas', texto: 'Estadísticas' }
  ],
  ADMINISTRADOR: [
    { to: '/admin/cuentas', texto: 'Usuarios' },
    { to: '/admin/canales', texto: 'Canales' },
    { to: '/admin/auditoria', texto: 'Auditoría' }
  ]
}

export default function Navbar() {
  const { token, rol, usuario, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [noLeidas, setNoLeidas] = useState(0)

  useEffect(() => {
    if (!token || rol === 'ADMINISTRADOR') return
    api.get('/notificaciones')
      .then(r => setNoLeidas(r.data.filter(n => !n.leida).length))
      .catch(() => {})
  }, [token, rol, location.pathname])

  if (!token) return null

  const salir = async () => {
    try { await logout() } catch { /* La sesión local se elimina incluso sin conexión. */ }
    navigate('/login')
  }

  const claseEnlace = ({ isActive }) =>
    `rounded-full px-3 py-1.5 text-sm font-medium transition-colors ${isActive ? 'bg-marca-suave text-marca-oscuro' : 'text-suave hover:text-tinta hover:bg-fondo'}`

  return (
    <header className="sticky top-0 z-30 bg-white/90 backdrop-blur border-b border-linea">
      <nav className="max-w-6xl mx-auto px-4 h-14 flex items-center gap-4">
        <Link to="/" className="font-logo font-bold text-lg tracking-tight">
          hello_friend<span className="cursor-terminal">▍</span>
        </Link>

        <div className="flex items-center gap-1">
          {(ENLACES[rol] ?? []).map(e => (
            <NavLink key={e.to} to={e.to} className={claseEnlace}>{e.texto}</NavLink>
          ))}
        </div>

        <div className="ml-auto flex items-center gap-2">
          {rol !== 'ADMINISTRADOR' && (
            <Link to="/notificaciones" className="relative h-9 w-9 rounded-full grid place-items-center hover:bg-fondo" aria-label={`Notificaciones${noLeidas ? `, ${noLeidas} sin leer` : ''}`}>
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-2 text-suave" aria-hidden="true">
                <path d="M6 8a6 6 0 1 1 12 0c0 7 3 8 3 8H3s3-1 3-8M10.3 20a1.9 1.9 0 0 0 3.4 0" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              {noLeidas > 0 && (
                <span className="absolute top-0.5 right-0.5 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-4 h-4 px-1 grid place-items-center">
                  {noLeidas}
                </span>
              )}
            </Link>
          )}
          <div className="hidden sm:flex items-center gap-2 pl-2 pr-1 py-1 rounded-full bg-fondo">
            <AvatarAnonimo tamaño="sm" />
            <span className="text-sm leading-tight">
              <span className="block font-semibold">{usuario}</span>
              <span className="block text-[11px] text-suave">{ROLES[rol]}</span>
            </span>
          </div>
          <button onClick={salir} className="rounded-full px-3 py-1.5 text-sm text-suave hover:text-tinta hover:bg-fondo">Salir</button>
        </div>
      </nav>
    </header>
  )
}

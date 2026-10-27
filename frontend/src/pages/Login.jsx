import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { MODO_MOCK, mensajeError } from '../api/client'
import { Alerta, claseBoton, claseInput } from '../components/ui'

const CUENTAS_DEMO = [
  ['alumno1', 'alumno123', 'Estudiante'],
  ['alumno2', 'alumno123', 'Estudiante'],
  ['profesor', 'profesor123', 'Profesor'],
  ['director', 'director123', 'Director de carrera'],
  ['admin', 'admin123', 'Administrador'],
  ['exalumno', 'alumno123', 'Cuenta desactivada (no puede ingresar)']
]

export default function Login() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  if (token) return <Navigate to="/" replace />

  const enviar = async e => {
    e.preventDefault()
    setError('')
    setCargando(true)
    try {
      await login(usuario.trim(), password)
      navigate('/')
    } catch (err) {
      setError(mensajeError(err, 'No se pudo iniciar sesión'))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="max-w-sm mx-auto mt-10">
      <div className="text-center mb-6">
        <p className="font-logo text-3xl font-bold text-tinta">hello_friend<span className="cursor-terminal">▍</span></p>
        <p className="text-sm text-suave mt-2">Tu identidad está protegida.</p>
      </div>

      <div className="bg-white rounded-2xl border border-linea p-6">
        <h1 className="text-lg font-bold mb-1">Iniciar sesión</h1>
        <p className="text-sm text-suave mb-5">Reportes anónimos · Ingeniería Informática PUCP</p>

        <form onSubmit={enviar} className="space-y-4">
          <div>
            <label className="text-sm font-medium text-tinta">Usuario</label>
            <input
              value={usuario}
              onChange={e => setUsuario(e.target.value)}
              maxLength={15}
              required
              autoComplete="username"
              className={`${claseInput} mt-1`}
            />
          </div>
          <div>
            <label className="text-sm font-medium text-tinta">Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              className={`${claseInput} mt-1`}
            />
          </div>
          {error && <Alerta>{error}</Alerta>}
          <button disabled={cargando} className={`${claseBoton} w-full`}>
            {cargando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <p className="text-sm text-center mt-4 text-suave">
          ¿Eres estudiante y no tienes cuenta?{' '}
          <Link to="/registro" className="text-marca font-semibold hover:underline">Regístrate</Link>
        </p>
      </div>

      {MODO_MOCK && (
        <div className="mt-4 bg-amber-50 border border-amber-200 rounded-2xl p-4 text-xs text-amber-900">
          <p className="font-semibold mb-2">Cuentas de demostración</p>
          <ul className="space-y-1">
            {CUENTAS_DEMO.map(([u, p, desc]) => (
              <li key={u}>
                <button
                  type="button"
                  onClick={() => { setUsuario(u); setPassword(p) }}
                  className="font-mono hover:underline"
                >
                  {u} / {p}
                </button>{' '}
                — {desc}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

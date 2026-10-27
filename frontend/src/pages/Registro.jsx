import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, claseBoton, claseInput } from '../components/ui'

export default function Registro() {
  const [form, setForm] = useState({ correo: '', usuario: '', password: '', confirmar: '' })
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [listo, setListo] = useState(false)

  const set = campo => e => setForm(f => ({ ...f, [campo]: e.target.value }))

  const validar = () => {
    if (!form.correo.trim().toLowerCase().endsWith('@pucp.edu.pe')) return 'Debes usar tu correo @pucp.edu.pe'
    if (!/^[a-zA-Z0-9._-]{3,15}$/.test(form.usuario)) return 'El usuario debe tener entre 3 y 15 caracteres: letras, números, punto o guion'
    if (form.password.length < 6) return 'La contraseña debe tener al menos 6 caracteres'
    if (form.password !== form.confirmar) return 'Las contraseñas no coinciden'
    return null
  }

  const enviar = async e => {
    e.preventDefault()
    const problema = validar()
    if (problema) return setError(problema)
    setError('')
    setCargando(true)
    try {
      await api.post('/auth/registro', { correo: form.correo.trim(), usuario: form.usuario.trim(), password: form.password })
      setListo(true)
    } catch (err) {
      setError(mensajeError(err, 'No se pudo completar el registro'))
    } finally {
      setCargando(false)
    }
  }

  if (listo) {
    return (
      <div className="max-w-sm mx-auto mt-10 bg-white rounded-2xl border border-linea p-6 text-center">
        <p className="text-4xl mb-3">✅</p>
        <h1 className="text-lg font-bold mb-2">Cuenta creada</h1>
        <p className="text-sm text-suave mb-5">
          Ya puedes ingresar con tu usuario <span className="font-mono font-semibold">{form.usuario}</span>.
        </p>
        <Link to="/login" className={claseBoton}>Ir al inicio de sesión</Link>
      </div>
    )
  }

  return (
    <div className="max-w-sm mx-auto mt-10 bg-white rounded-2xl border border-linea p-6">
      <h1 className="text-lg font-bold mb-1">Crear cuenta</h1>
      <p className="text-sm text-suave mb-5">Solo estudiantes de Ingeniería Informática PUCP</p>

      <form onSubmit={enviar} className="space-y-4">
        <div>
          <label className="text-sm font-medium text-tinta">Correo PUCP</label>
          <input type="email" value={form.correo} onChange={set('correo')} placeholder="tu.nombre@pucp.edu.pe" required className={`${claseInput} mt-1`} />
          <p className="text-xs text-suave mt-1">Solo lo usa la administración para gestionar cuentas. No se muestra en la plataforma.</p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Usuario</label>
          <input value={form.usuario} onChange={set('usuario')} maxLength={15} required className={`${claseInput} mt-1`} />
          <p className="text-xs text-suave mt-1">
            {form.usuario.length}/15 · Con este usuario inicias sesión; no aparece en tus reportes.
          </p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Contraseña</label>
          <input type="password" value={form.password} onChange={set('password')} required className={`${claseInput} mt-1`} />
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Confirmar contraseña</label>
          <input type="password" value={form.confirmar} onChange={set('confirmar')} required className={`${claseInput} mt-1`} />
        </div>
        {error && <Alerta>{error}</Alerta>}
        <button disabled={cargando} className={`${claseBoton} w-full`}>
          {cargando ? 'Enviando...' : 'Registrarme'}
        </button>
      </form>

      <p className="text-sm text-center mt-4 text-suave">
        ¿Ya tienes cuenta? <Link to="/login" className="text-marca font-semibold hover:underline">Inicia sesión</Link>
      </p>
    </div>
  )
}

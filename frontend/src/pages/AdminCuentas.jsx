import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { Alerta, Cargando, Titulo, Vacio, claseBotonSecundario, claseInput } from '../components/ui'
import { ROLES, fechaHora } from '../utils/formato'

const ESTADOS_CUENTA = {
  ACTIVA: { etiqueta: 'Activa', clase: 'bg-green-100 text-green-800' },
  DESACTIVADA: { etiqueta: 'Desactivada', clase: 'bg-slate-100 text-slate-600' }
}

export default function AdminCuentas() {
  const { usuario: yo } = useAuth()
  const [buscar, setBuscar] = useState('')
  const [rol, setRol] = useState('')
  const [version, setVersion] = useState(0)
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: '' })
  const [error, setError] = useState('')
  const [confirmando, setConfirmando] = useState(null)
  const [enviando, setEnviando] = useState(null)

  const clave = `${buscar}|${rol}|${version}`

  useEffect(() => {
    let vigente = true
    const k = `${buscar}|${rol}|${version}`
    api.get('/admin/usuarios', { params: { buscar: buscar.trim() || undefined, rol: rol || undefined } })
      .then(r => vigente && setResultado({ clave: k, lista: r.data, error: '' }))
      .catch(e => vigente && setResultado({ clave: k, lista: [], error: mensajeError(e) }))
    return () => { vigente = false }
  }, [buscar, rol, version])

  const ejecutar = async (cuenta, peticion) => {
    setEnviando(cuenta.id)
    setError('')
    try {
      await peticion()
      setConfirmando(null)
      setVersion(v => v + 1)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(null)
    }
  }

  const cambiarRol = (cuenta, nuevo) => ejecutar(cuenta, () => api.put(`/admin/usuarios/${cuenta.id}/rol`, { rol: nuevo }))
  const desactivar = cuenta => ejecutar(cuenta, () => api.post(`/admin/usuarios/${cuenta.id}/desactivar`))
  const reactivar = cuenta => ejecutar(cuenta, () => api.post(`/admin/usuarios/${cuenta.id}/reactivar`))

  const cargando = resultado.clave !== clave

  return (
    <div>
      <Titulo subtitulo="Todo usuario nuevo parte como estudiante. Desde aquí asignas el rol de profesor, director o administrador, o desactivas una cuenta; su historial se conserva.">
        Usuarios
      </Titulo>

      <div className="grid gap-3 sm:grid-cols-[1fr_220px] mb-4">
        <input value={buscar} onChange={e => setBuscar(e.target.value)} placeholder="Busca por usuario o correo..." className={claseInput} />
        <select value={rol} onChange={e => setRol(e.target.value)} className={claseInput} aria-label="Filtrar por rol">
          <option value="">Todos los roles</option>
          {Object.entries(ROLES).map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
        </select>
      </div>

      {error && <div className="mb-4"><Alerta>{error}</Alerta></div>}

      {cargando ? (
        <Cargando />
      ) : resultado.error ? (
        <Alerta>{resultado.error}</Alerta>
      ) : resultado.lista.length === 0 ? (
        <Vacio>Ningún usuario coincide con la búsqueda.</Vacio>
      ) : (
        <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-fondo text-left text-suave">
              <tr>
                <th className="px-4 py-2 font-medium">Usuario</th>
                <th className="px-4 py-2 font-medium">Correo</th>
                <th className="px-4 py-2 font-medium">Rol</th>
                <th className="px-4 py-2 font-medium">Estado</th>
                <th className="px-4 py-2 font-medium">Registro</th>
                <th className="px-4 py-2 font-medium text-right">Acción</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-linea">
              {resultado.lista.map(c => {
                const esYo = c.usuario === yo
                const estado = ESTADOS_CUENTA[c.estado] ?? ESTADOS_CUENTA.ACTIVA
                const activa = c.estado === 'ACTIVA'
                return (
                  <tr key={c.id}>
                    <td className="px-4 py-2 whitespace-nowrap font-mono font-semibold text-tinta">
                      {c.usuario}{esYo && <span className="ml-1 font-sans font-normal text-xs text-suave">(tú)</span>}
                    </td>
                    <td className="px-4 py-2 text-suave">{c.correo}</td>
                    <td className="px-4 py-2">
                      <select
                        value={c.rol}
                        onChange={e => cambiarRol(c, e.target.value)}
                        disabled={esYo || !activa || enviando === c.id}
                        aria-label={`Rol de ${c.usuario}`}
                        className={`${claseInput} py-1`}
                      >
                        {Object.entries(ROLES).map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
                      </select>
                    </td>
                    <td className="px-4 py-2">
                      <span className={`text-xs px-2 py-0.5 rounded-full whitespace-nowrap ${estado.clase}`}>{estado.etiqueta}</span>
                    </td>
                    <td className="px-4 py-2 whitespace-nowrap text-suave">{fechaHora(c.fechaRegistro)}</td>
                    <td className="px-4 py-2 text-right whitespace-nowrap">
                      {esYo ? (
                        <span className="text-suave">—</span>
                      ) : !activa ? (
                        <button onClick={() => reactivar(c)} disabled={enviando === c.id} className={claseBotonSecundario}>Reactivar</button>
                      ) : confirmando === c.id ? (
                        <span className="inline-flex gap-2">
                          <button onClick={() => desactivar(c)} disabled={enviando === c.id} className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium">
                            Sí, desactivar
                          </button>
                          <button onClick={() => setConfirmando(null)} className={claseBotonSecundario}>Cancelar</button>
                        </span>
                      ) : (
                        <button onClick={() => setConfirmando(c.id)} className={claseBotonSecundario}>Desactivar</button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

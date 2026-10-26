import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, Vacio, claseBoton, claseBotonSecundario } from '../components/ui'
import { fechaHora } from '../utils/formato'

export default function AdminCuentas() {
  const [cuentas, setCuentas] = useState(null)
  const [enviando, setEnviando] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/admin/usuarios', { params: { estado: 'PENDIENTE' } })
      .then(r => setCuentas(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [])

  const decidir = async (cuenta, accion) => {
    setEnviando(cuenta.id)
    setError('')
    try {
      await api.post(`/admin/usuarios/${cuenta.id}/${accion}`)
      setCuentas(l => l.filter(x => x.id !== cuenta.id))
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(null)
    }
  }

  if (!cuentas) return error ? <Alerta>{error}</Alerta> : <Cargando />

  return (
    <div>
      <Titulo subtitulo="Verifica en el campus que el solicitante sea alumno de la facultad y luego acepta o rechaza el registro.">
        Solicitudes de registro <span className="text-suave/70 font-normal">({cuentas.length})</span>
      </Titulo>

      {error && <div className="mb-4"><Alerta>{error}</Alerta></div>}

      {cuentas.length === 0 ? (
        <Vacio>No hay solicitudes pendientes.</Vacio>
      ) : (
        <ul className="bg-white border border-linea rounded-2xl divide-y divide-linea">
          {cuentas.map(c => (
            <li key={c.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
              <div className="flex-1 min-w-40">
                <p className="font-mono font-semibold text-tinta">{c.usuario}</p>
                <p className="text-xs text-suave">Solicitado {fechaHora(c.fechaRegistro)}</p>
              </div>
              <button onClick={() => decidir(c, 'aprobar')} disabled={enviando === c.id} className={claseBoton}>
                Aceptar
              </button>
              <button onClick={() => decidir(c, 'rechazar')} disabled={enviando === c.id} className={claseBotonSecundario}>
                Rechazar
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

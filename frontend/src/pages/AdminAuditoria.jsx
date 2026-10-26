import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, Vacio, claseInput } from '../components/ui'
import { fechaHora } from '../utils/formato'

const ACCIONES = {
  CUENTA_REGISTRADA: 'Cuenta registrada',
  CUENTA_APROBADA: 'Cuenta aprobada',
  CUENTA_RECHAZADA: 'Cuenta rechazada',
  CANAL_CREADO: 'Canal creado',
  INCIDENCIA_REGISTRADA: 'Incidencia registrada',
  INCIDENCIA_APROBADA: 'Incidencia aprobada',
  INCIDENCIA_RECHAZADA: 'Incidencia rechazada',
  INCIDENCIA_RESUELTA: 'Incidencia resuelta'
}

export default function AdminAuditoria() {
  const [registros, setRegistros] = useState(null)
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('')

  useEffect(() => {
    api.get('/admin/auditoria')
      .then(r => setRegistros(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [])

  if (error) return <Alerta>{error}</Alerta>
  if (!registros) return <Cargando />

  const f = filtro.trim().toLowerCase()
  const visibles = registros.filter(r =>
    !f || [ACCIONES[r.accion], r.actor, r.detalle].some(v => v?.toLowerCase().includes(f))
  )

  return (
    <div>
      <Titulo subtitulo="Registro de acciones del sistema. La identidad de los autores de incidencias nunca se registra.">
        Auditoría
      </Titulo>

      <input value={filtro} onChange={e => setFiltro(e.target.value)} placeholder="Filtrar por acción, actor o detalle..." className={`${claseInput} mb-4`} />

      {visibles.length === 0 ? (
        <Vacio>Sin registros.</Vacio>
      ) : (
        <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-fondo text-left text-suave">
              <tr>
                <th className="px-4 py-2 font-medium">Fecha</th>
                <th className="px-4 py-2 font-medium">Acción</th>
                <th className="px-4 py-2 font-medium">Actor</th>
                <th className="px-4 py-2 font-medium">Incidencia</th>
                <th className="px-4 py-2 font-medium">Detalle</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-linea">
              {visibles.map(r => (
                <tr key={r.id}>
                  <td className="px-4 py-2 whitespace-nowrap text-suave">{fechaHora(r.fecha)}</td>
                  <td className="px-4 py-2 whitespace-nowrap">{ACCIONES[r.accion] ?? r.accion}</td>
                  <td className="px-4 py-2 whitespace-nowrap font-mono text-xs">{r.actor}</td>
                  <td className="px-4 py-2">{r.incidenciaId ? `#${r.incidenciaId}` : '—'}</td>
                  <td className="px-4 py-2 text-suave">{r.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

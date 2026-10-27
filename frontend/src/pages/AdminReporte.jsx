import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import Historial, { Versiones } from '../components/Historial'
import { Alerta, CategoriaChip, Cargando, EstadoBadge, PrioridadBadge } from '../components/ui'
import { fechaHora } from '../utils/formato'

export default function AdminReporte() {
  const { id } = useParams()
  const [r, setR] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/admin/reportes/${id}`).then(x => setR(x.data)).catch(e => setError(mensajeError(e)))
  }, [id])

  if (error) return <Alerta>{error}</Alerta>
  if (!r) return <Cargando />

  const confirmantes = r.eventos.filter(e => e.tipo === 'CONFIRMADA').map(e => e.responsable)

  return (
    <div>
      <Link to="/admin/auditoria" className="text-sm text-suave hover:underline">← Volver a la auditoría</Link>

      <article className="bg-white border border-linea rounded-2xl p-6 mt-3">
        <div className="flex items-center gap-2 text-xs text-suave mb-3 flex-wrap">
          <span className="font-semibold text-tinta">{r.canal.nombre}</span>
          <span>·</span>
          <span>{r.canal.periodo}</span>
          <span>·</span>
          <span>{fechaHora(r.fechaCreacion)}</span>
          <span className="ml-auto"><EstadoBadge estado={r.estado} /></span>
        </div>
        <h1 className="text-xl font-bold text-tinta mb-2">{r.titulo}</h1>
        <div className="flex flex-wrap items-center gap-1.5 mb-3">
          <PrioridadBadge prioridad={r.prioridad} />
          <CategoriaChip categoria={r.categoria} />
        </div>
        <p className="text-tinta whitespace-pre-line mb-5">{r.descripcion}</p>

        <dl className="grid gap-3 sm:grid-cols-2 text-sm border-t border-linea pt-4">
          <div>
            <dt className="text-suave">Autor</dt>
            <dd className="font-medium text-tinta">{r.autor.usuario} <span className="text-suave font-normal">· {r.autor.correo}</span></dd>
          </div>
          <div>
            <dt className="text-suave">Confirmaciones ({r.confirmaciones})</dt>
            <dd className="font-medium text-tinta">{confirmantes.length ? confirmantes.join(', ') : 'Sin confirmaciones'}</dd>
          </div>
        </dl>

        {r.solucion && (
          <div className="rounded-2xl border border-green-200 bg-green-50 p-4 mt-5">
            <p className="text-sm font-semibold text-green-800">Solución aplicada</p>
            <p className="text-sm text-tinta mt-1 whitespace-pre-line">{r.solucion}</p>
          </div>
        )}
      </article>

      <Historial eventos={r.eventos} titulo="Historial completo" />
      <Versiones versiones={r.versiones} />
    </div>
  )
}

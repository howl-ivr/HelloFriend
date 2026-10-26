import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, CategoriaChip, Cargando, EstadoBadge, PrioridadBadge, Titulo, Vacio, claseBoton } from '../components/ui'
import { hace } from '../utils/formato'

export default function MisIncidencias() {
  const [lista, setLista] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/mis-incidencias').then(r => setLista(r.data)).catch(e => setError(mensajeError(e)))
  }, [])

  if (error) return <Alerta>{error}</Alerta>
  if (!lista) return <Cargando />

  return (
    <div>
      <Titulo
        subtitulo="Seguimiento de las incidencias que reportaste. Solo tú puedes ver esta lista."
        accion={<Link to="/incidencias/nueva" className={claseBoton}>+ Reportar</Link>}
      >
        Mis incidencias
      </Titulo>

      {lista.length === 0 ? (
        <Vacio>Aún no has reportado incidencias.</Vacio>
      ) : (
        <div className="space-y-3">
          {lista.map(i => (
            <Link key={i.id} to={`/incidencias/${i.id}`} className="block bg-white border border-linea rounded-2xl p-4 hover:border-marca/40">
              <div className="flex items-center gap-2 text-xs text-suave mb-1">
                <span className="font-semibold text-tinta">{i.canal.nombre}</span>
                <span>·</span>
                <span>{hace(i.fechaCreacion)}</span>
                <span className="ml-auto"><EstadoBadge estado={i.estado} /></span>
              </div>
              <h3 className="font-semibold text-tinta">{i.titulo}</h3>
              <div className="flex flex-wrap items-center gap-1.5 mt-1.5">
                <PrioridadBadge prioridad={i.prioridad} />
                <CategoriaChip categoria={i.categoria} />
              </div>

              {i.estado === 'RECHAZADA' && i.revision && (
                <p className="text-sm text-red-700 mt-2">
                  {[i.revision.motivo, i.revision.comentario].filter(Boolean).join(' — ')}
                </p>
              )}
              {i.estado === 'PENDIENTE' && <p className="text-sm text-amber-700 mt-2">En revisión por la dirección de carrera</p>}
              {i.estado === 'OBSERVADA' && (
                <p className="text-sm text-orange-700 mt-2">
                  La dirección pidió cambios{i.observacion ? `: ${i.observacion}` : ''}. Edita y reenvía tu reporte.
                </p>
              )}
              {(i.estado === 'PUBLICADA' || i.estado === 'EN_ATENCION' || i.estado === 'RESUELTA') && (
                <p className="text-sm text-confirma mt-2">✔ {i.confirmaciones} confirmaciones · 💬 {i.totalComentarios}</p>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

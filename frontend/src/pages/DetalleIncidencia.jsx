import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import BotonConfirmar from '../components/BotonConfirmar'
import Reacciones from '../components/Reacciones'
import Comentarios from '../components/Comentarios'
import AccionesDirector from '../components/AccionesDirector'
import { Alerta, CategoriaChip, Cargando, EstadoBadge, PrioridadBadge } from '../components/ui'
import { fechaHora, hace } from '../utils/formato'

function Revision({ revision }) {
  if (!revision) return null
  const rechazada = revision.decision === 'RECHAZADA'
  return (
    <div className={`rounded-2xl border p-4 mb-5 ${rechazada ? 'bg-red-50 border-red-200' : 'bg-blue-50 border-blue-200'}`}>
      <p className={`text-sm font-semibold ${rechazada ? 'text-red-800' : 'text-blue-800'}`}>
        {rechazada ? 'Respuesta de la dirección: rechazada' : 'Aprobada por la dirección'}
      </p>
      {revision.motivo && <p className="text-sm text-tinta mt-1"><span className="font-medium">Motivo:</span> {revision.motivo}</p>}
      {revision.comentario && <p className="text-sm text-tinta mt-1"><span className="font-medium">Comentario:</span> {revision.comentario}</p>}
      <p className="text-xs text-suave mt-2">{fechaHora(revision.fecha)}</p>
    </div>
  )
}

export default function DetalleIncidencia() {
  const { id } = useParams()
  const { rol } = useAuth()
  const navigate = useNavigate()
  const { hash } = useLocation()
  const [inc, setInc] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/incidencias/${id}`).then(r => setInc(r.data)).catch(e => setError(mensajeError(e)))
  }, [id])

  useEffect(() => {
    if (inc && hash === '#comentarios') document.getElementById('comentarios')?.scrollIntoView()
  }, [inc, hash])

  if (error) return <Alerta>{error}</Alerta>
  if (!inc) return <Cargando />

  const visible = inc.estado === 'PUBLICADA' || inc.estado === 'RESUELTA'
  const volver = rol === 'DIRECTOR'
    ? { to: inc.estado === 'PENDIENTE' ? '/director/pendientes' : '/director/publicadas', texto: '← Volver a la bandeja' }
    : inc.esMia && !visible
      ? { to: '/mis-incidencias', texto: '← Mis incidencias' }
      : { to: `/inicio?canal=${inc.canal.id}`, texto: `← ${inc.canal.nombre}` }

  return (
    <div>
      <Link to={volver.to} className="text-sm text-suave hover:underline">{volver.texto}</Link>

      <article className="bg-white border border-linea rounded-2xl p-6 mt-3">
        <div className="flex items-center gap-2 text-xs text-suave mb-3 flex-wrap">
          <span className="font-semibold text-tinta">{inc.canal.nombre}</span>
          <span>·</span>
          <span>Alumno anónimo</span>
          <span>·</span>
          <span title={fechaHora(inc.fechaCreacion)}>{hace(inc.fechaCreacion)}</span>
          {inc.esMia && <span className="text-confirma">(tu reporte)</span>}
          <span className="ml-auto"><EstadoBadge estado={inc.estado} /></span>
        </div>

        <h1 className="text-xl font-bold text-tinta mb-2">{inc.titulo}</h1>
        <div className="flex flex-wrap items-center gap-1.5 mb-3">
          <PrioridadBadge prioridad={inc.prioridad} />
          <CategoriaChip categoria={inc.categoria} />
        </div>
        <p className="text-tinta whitespace-pre-line mb-5">{inc.descripcion}</p>

        {(inc.esMia || rol === 'DIRECTOR') && <Revision revision={inc.revision} />}

        {inc.esMia && inc.estado === 'PENDIENTE' && (
          <Alerta tipo="info">Tu incidencia está en revisión. Solo tú y la dirección pueden verla hasta que sea aprobada.</Alerta>
        )}

        {rol === 'DIRECTOR' && (
          <div className="mb-2">
            <AccionesDirector incidencia={inc} onHecho={() => navigate(volver.to)} />
          </div>
        )}

        {visible && (
          <div className="space-y-5">
            {rol === 'ESTUDIANTE' && inc.estado === 'PUBLICADA' ? (
              <BotonConfirmar incidencia={inc} />
            ) : (
              <p className="text-sm font-semibold text-confirma">✔ {inc.confirmaciones} confirmaciones</p>
            )}
            <Reacciones incidencia={inc} habilitado={inc.puedeReaccionar} />
          </div>
        )}
      </article>

      {visible && (
        <div id="comentarios" className="bg-white border border-linea rounded-2xl p-6 mt-4 scroll-mt-20">
          <Comentarios incidencia={inc} />
        </div>
      )}
    </div>
  )
}

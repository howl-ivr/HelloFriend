import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import BotonConfirmar from '../components/BotonConfirmar'
import Reacciones from '../components/Reacciones'
import Comentarios from '../components/Comentarios'
import AccionesDirector from '../components/AccionesDirector'
import Historial, { Versiones } from '../components/Historial'
import { Alerta, Autor, CategoriaChip, Cargando, EstadoBadge, PrioridadBadge, claseBoton } from '../components/ui'
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

function Observaciones({ inc }) {
  if (!inc.observaciones?.length) return null
  const vigente = inc.estado === 'OBSERVADA'
  return (
    <div className={`rounded-2xl border p-4 mb-5 ${vigente ? 'bg-orange-50 border-orange-200' : 'bg-fondo border-linea'}`}>
      <p className={`text-sm font-semibold ${vigente ? 'text-orange-800' : 'text-tinta'}`}>
        {vigente ? 'La dirección pidió cambios' : 'Observaciones recibidas'}
      </p>
      <ul className="mt-2 space-y-2">
        {inc.observaciones.map(o => (
          <li key={o.fecha} className="text-sm text-tinta">
            {o.texto}
            <span className="block text-xs text-suave">Versión {o.version} · {fechaHora(o.fecha)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function Solucion({ inc }) {
  if (!inc.solucion) return null
  return (
    <div className="rounded-2xl border border-green-200 bg-green-50 p-4 mb-5">
      <p className="text-sm font-semibold text-green-800">Solución aplicada</p>
      <p className="text-sm text-tinta mt-1 whitespace-pre-line">{inc.solucion}</p>
      {inc.fechaResolucion && <p className="text-xs text-suave mt-2">{fechaHora(inc.fechaResolucion)}</p>}
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

  const visible = inc.estado === 'PUBLICADA' || inc.estado === 'EN_ATENCION' || inc.estado === 'RESUELTA'
  const enRevision = inc.estado === 'PENDIENTE' || inc.estado === 'OBSERVADA'
  const volver = rol === 'DIRECTOR'
    ? { to: enRevision ? '/director/pendientes' : '/director/publicadas', texto: '← Volver a la bandeja' }
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
          <Autor incidencia={inc} />
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

        <Solucion inc={inc} />
        {(inc.esMia || rol === 'DIRECTOR') && <Revision revision={inc.revision} />}
        <Observaciones inc={inc} />

        {inc.esMia && inc.estado === 'PENDIENTE' && (
          <div className="mb-4">
            <Alerta tipo="info">Tu incidencia está en revisión. Solo tú y la dirección pueden verla hasta que sea aprobada. Mientras tanto puedes editarla.</Alerta>
          </div>
        )}
        {rol === 'DIRECTOR' && inc.estado === 'OBSERVADA' && (
          <div className="mb-4">
            <Alerta tipo="info">Esperando que el estudiante edite y reenvíe la incidencia.</Alerta>
          </div>
        )}

        {inc.puedeEditar && (
          <div className="mb-2">
            <Link to={`/incidencias/${inc.id}/editar`} className={claseBoton}>
              {inc.estado === 'OBSERVADA' ? 'Editar y reenviar' : 'Editar'}
            </Link>
          </div>
        )}

        {rol === 'DIRECTOR' && (
          <div className="mb-2">
            <AccionesDirector incidencia={inc} onHecho={() => navigate(volver.to)} />
          </div>
        )}

        {visible && (
          <div className="space-y-5 mt-5">
            {rol === 'ESTUDIANTE' && inc.estado !== 'RESUELTA' ? (
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

      <Historial eventos={inc.historial} titulo={rol === 'DIRECTOR' ? 'Historial del reporte' : 'Historial de estados'} />
      {rol === 'DIRECTOR' && <Versiones versiones={inc.versiones} />}
    </div>
  )
}

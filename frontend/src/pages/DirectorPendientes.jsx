import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import AccionesDirector from '../components/AccionesDirector'
import { Alerta, Autor, CategoriaChip, Cargando, PrioridadBadge, Titulo, Vacio } from '../components/ui'
import { fechaHora, hace } from '../utils/formato'

export default function DirectorPendientes() {
  const [estado, setEstado] = useState('PENDIENTE')
  const [lista, setLista] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/director/incidencias', { params: { estado } })
      .then(r => setLista(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [estado])

  const cambiarEstado = valor => {
    setLista(null)
    setError('')
    setEstado(valor)
  }

  const pestaña = valor =>
    `px-3 py-1.5 text-sm rounded-full ${estado === valor ? 'bg-tinta text-white' : 'text-suave hover:bg-linea'}`

  return (
    <div>
      <Titulo subtitulo="Revisa cada incidencia antes de que se publique: apruébala, pide cambios al estudiante o recházala con un motivo.">
        Pendientes de revisión{lista && estado === 'PENDIENTE' && <span className="text-suave/70 font-normal"> ({lista.length})</span>}
      </Titulo>

      <div className="flex gap-2 mb-4">
        <button onClick={() => cambiarEstado('PENDIENTE')} className={pestaña('PENDIENTE')}>Por revisar</button>
        <button onClick={() => cambiarEstado('OBSERVADA')} className={pestaña('OBSERVADA')}>Observadas</button>
      </div>

      {error ? (
        <Alerta>{error}</Alerta>
      ) : !lista ? (
        <Cargando />
      ) : lista.length === 0 ? (
        <Vacio>{estado === 'PENDIENTE' ? 'No hay incidencias pendientes. 🎉' : 'No hay incidencias observadas esperando respuesta del estudiante.'}</Vacio>
      ) : (
        <div className="space-y-4">
          {lista.map(i => (
            <div key={i.id} className="bg-white border border-linea rounded-2xl p-5">
              <div className="flex items-center gap-2 text-xs text-suave mb-2">
                <span className="font-semibold text-tinta">{i.canal.nombre}</span>
                <span>·</span>
                <Autor incidencia={i} />
                <span>·</span>
                <span title={fechaHora(i.fechaCreacion)}>{hace(i.fechaCreacion)}</span>
                <Link to={`/incidencias/${i.id}`} className="ml-auto hover:underline">Ver detalle</Link>
              </div>
              <h3 className="font-semibold text-tinta mb-1">{i.titulo}</h3>
              <div className="flex flex-wrap items-center gap-1.5 mb-2">
                <PrioridadBadge prioridad={i.prioridad} />
                <CategoriaChip categoria={i.categoria} />
              </div>
              <p className="text-sm text-tinta whitespace-pre-line mb-4">{i.descripcion}</p>
              {estado === 'OBSERVADA' ? (
                <Alerta tipo="info">Esperando que el estudiante edite y reenvíe. Observación: {i.observacion}</Alerta>
              ) : (
                <AccionesDirector incidencia={i} onHecho={() => setLista(l => l.filter(x => x.id !== i.id))} />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import AccionesDirector from '../components/AccionesDirector'
import { Alerta, Cargando, Titulo, Vacio } from '../components/ui'
import { fechaHora, hace } from '../utils/formato'

export default function DirectorPendientes() {
  const [lista, setLista] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/director/incidencias', { params: { estado: 'PENDIENTE' } })
      .then(r => setLista(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [])

  if (error) return <Alerta>{error}</Alerta>
  if (!lista) return <Cargando />

  return (
    <div>
      <Titulo subtitulo="Revisa cada incidencia antes de que se publique. Si la rechazas, el estudiante recibirá el motivo.">
        Pendientes de revisión <span className="text-suave/70 font-normal">({lista.length})</span>
      </Titulo>

      {lista.length === 0 ? (
        <Vacio>No hay incidencias pendientes. 🎉</Vacio>
      ) : (
        <div className="space-y-4">
          {lista.map(i => (
            <div key={i.id} className="bg-white border border-linea rounded-2xl p-5">
              <div className="flex items-center gap-2 text-xs text-suave mb-2">
                <span className="font-semibold text-tinta">{i.canal.nombre}</span>
                <span>·</span>
                <span>Alumno anónimo</span>
                <span>·</span>
                <span title={fechaHora(i.fechaCreacion)}>{hace(i.fechaCreacion)}</span>
                <Link to={`/incidencias/${i.id}`} className="ml-auto hover:underline">Ver detalle</Link>
              </div>
              <h3 className="font-semibold text-tinta mb-1">{i.titulo}</h3>
              <p className="text-sm text-tinta whitespace-pre-line mb-4">{i.descripcion}</p>
              <AccionesDirector incidencia={i} onHecho={() => setLista(l => l.filter(x => x.id !== i.id))} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

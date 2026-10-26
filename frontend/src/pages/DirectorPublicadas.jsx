import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import AccionesDirector from '../components/AccionesDirector'
import { Cargando, Titulo, Vacio } from '../components/ui'
import { hace, REACCIONES } from '../utils/formato'

export default function DirectorPublicadas() {
  const [estado, setEstado] = useState('PUBLICADA')
  const [lista, setLista] = useState(null)

  useEffect(() => {
    api.get('/director/incidencias', { params: { estado } }).then(r => setLista(r.data))
  }, [estado])

  const pestaña = valor =>
    `px-3 py-1.5 text-sm rounded ${estado === valor ? 'bg-tinta text-white' : 'text-suave hover:bg-linea'}`

  return (
    <div>
      <Titulo subtitulo="Ordenadas por prioridad: número de alumnos del curso que confirmaron la incidencia.">
        Incidencias publicadas
      </Titulo>

      <div className="flex gap-2 mb-4">
        <button onClick={() => { setLista(null); setEstado('PUBLICADA') }} className={pestaña('PUBLICADA')}>Por atender</button>
        <button onClick={() => { setLista(null); setEstado('RESUELTA') }} className={pestaña('RESUELTA')}>Resueltas</button>
      </div>

      {!lista ? (
        <Cargando />
      ) : lista.length === 0 ? (
        <Vacio>No hay incidencias en esta lista.</Vacio>
      ) : (
        <div className="space-y-3">
          {lista.map((i, idx) => (
            <div key={i.id} className="bg-white border border-linea rounded-2xl p-4 flex gap-4">
              <div className="text-center w-16 shrink-0">
                <p className="text-2xl font-bold text-confirma">{i.confirmaciones}</p>
                <p className="text-[11px] text-suave leading-tight">confirma&shy;ciones</p>
                {estado === 'PUBLICADA' && <p className="text-[11px] text-suave/70 mt-1">#{idx + 1}</p>}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 text-xs text-suave mb-1">
                  <span className="font-semibold text-tinta">{i.canal.nombre}</span>
                  <span>·</span>
                  <span>{hace(i.fechaCreacion)}</span>
                </div>
                <Link to={`/incidencias/${i.id}`} className="font-semibold text-tinta hover:underline">{i.titulo}</Link>
                <p className="text-xs text-suave mt-1">
                  {REACCIONES.map(r => `${r.icono} ${i.reacciones[r.tipo]}`).join('   ')}   💬 {i.totalComentarios}
                </p>
                {estado === 'PUBLICADA' && (
                  <div className="mt-3">
                    <AccionesDirector incidencia={i} onHecho={() => setLista(l => l.filter(x => x.id !== i.id))} />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

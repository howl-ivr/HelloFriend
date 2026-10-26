import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { claseBoton, claseBotonSecundario, claseInput } from './ui'

const botonVerde = 'bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium'
const botonRojo = 'bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium'

export default function AccionesDirector({ incidencia, onHecho }) {
  const [modo, setModo] = useState(null)
  const [motivos, setMotivos] = useState([])
  const [motivoId, setMotivoId] = useState('')
  const [comentario, setComentario] = useState('')
  const [observacion, setObservacion] = useState('')
  const [solucion, setSolucion] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (modo === 'rechazar' && motivos.length === 0) {
      api.get('/motivos-rechazo').then(r => setMotivos(r.data))
    }
  }, [modo, motivos.length])

  const ejecutar = async (accion, body) => {
    setEnviando(true)
    setError('')
    try {
      const { data } = await api.post(`/director/incidencias/${incidencia.id}/${accion}`, body)
      onHecho?.(data)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(false)
    }
  }

  const aviso = error && <p className="text-sm text-red-600 mt-2">{error}</p>

  if (incidencia.estado === 'PUBLICADA' || incidencia.estado === 'EN_ATENCION') {
    if (modo === 'resolver') {
      return (
        <div className="border border-green-200 bg-green-50 rounded-2xl p-4 space-y-3">
          <p className="text-sm font-medium text-green-800">Describe la solución aplicada (la verán los alumnos del canal)</p>
          <textarea
            value={solucion}
            onChange={e => setSolucion(e.target.value)}
            placeholder="¿Qué se hizo para resolver la incidencia?"
            rows={3}
            maxLength={1000}
            className={claseInput}
          />
          <div className="flex gap-2">
            <button onClick={() => ejecutar('resolver', { solucion })} disabled={solucion.trim().length < 10 || enviando} className={botonVerde}>
              Confirmar resolución
            </button>
            <button onClick={() => setModo(null)} className={claseBotonSecundario}>Cancelar</button>
          </div>
          {aviso}
        </div>
      )
    }
    return (
      <div>
        <div className="flex flex-wrap gap-2">
          {incidencia.estado === 'PUBLICADA' && (
            <button onClick={() => ejecutar('atender')} disabled={enviando} className={claseBotonSecundario}>
              Pasar a en atención
            </button>
          )}
          <button onClick={() => setModo('resolver')} disabled={enviando} className={botonVerde}>
            Marcar como resuelta
          </button>
        </div>
        {aviso}
      </div>
    )
  }

  if (incidencia.estado !== 'PENDIENTE') return null

  if (modo === 'observar') {
    return (
      <div className="border border-orange-200 bg-orange-50 rounded-2xl p-4 space-y-3">
        <p className="text-sm font-medium text-orange-800">Indica qué debe cambiar o editar el estudiante antes de publicarla</p>
        <textarea
          value={observacion}
          onChange={e => setObservacion(e.target.value)}
          placeholder="Ej.: Precisa en qué sesiones ocurrió y a cuántos alumnos afecta."
          rows={3}
          maxLength={500}
          className={claseInput}
        />
        <div className="flex gap-2">
          <button onClick={() => ejecutar('observar', { observacion })} disabled={observacion.trim().length < 10 || enviando} className={claseBoton}>
            Enviar observación
          </button>
          <button onClick={() => setModo(null)} className={claseBotonSecundario}>Cancelar</button>
        </div>
        {aviso}
      </div>
    )
  }

  if (modo === 'rechazar') {
    const valido = motivoId || comentario.trim()
    return (
      <div className="border border-red-200 bg-red-50 rounded-2xl p-4 space-y-3">
        <p className="text-sm font-medium text-red-800">Motivo del rechazo (el estudiante lo recibirá)</p>
        <select value={motivoId} onChange={e => setMotivoId(e.target.value)} className={claseInput}>
          <option value="">Elige un motivo...</option>
          {motivos.map(m => <option key={m.id} value={m.id}>{m.descripcion}</option>)}
        </select>
        <textarea
          value={comentario}
          onChange={e => setComentario(e.target.value)}
          placeholder="Comentario para el estudiante (opcional si eliges un motivo)"
          rows={3}
          maxLength={500}
          className={claseInput}
        />
        <div className="flex gap-2">
          <button
            onClick={() => ejecutar('rechazar', { motivoId: motivoId ? Number(motivoId) : null, comentario })}
            disabled={!valido || enviando}
            className={botonRojo}
          >
            Confirmar rechazo
          </button>
          <button onClick={() => setModo(null)} className={claseBotonSecundario}>Cancelar</button>
        </div>
        {aviso}
      </div>
    )
  }

  return (
    <div>
      <div className="flex flex-wrap gap-2">
        <button onClick={() => ejecutar('aprobar')} disabled={enviando} className={claseBoton}>
          Aprobar y publicar
        </button>
        <button onClick={() => setModo('observar')} disabled={enviando} className={claseBotonSecundario}>
          Observar
        </button>
        <button onClick={() => setModo('rechazar')} disabled={enviando} className={claseBotonSecundario}>
          Rechazar
        </button>
      </div>
      {aviso}
    </div>
  )
}

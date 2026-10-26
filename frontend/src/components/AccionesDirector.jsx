import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { claseBoton, claseBotonSecundario, claseInput } from './ui'

export default function AccionesDirector({ incidencia, onHecho }) {
  const [modo, setModo] = useState(null)
  const [motivos, setMotivos] = useState([])
  const [motivoId, setMotivoId] = useState('')
  const [comentario, setComentario] = useState('')
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

  if (incidencia.estado === 'PUBLICADA') {
    return (
      <div>
        <button
          onClick={() => ejecutar('resolver')}
          disabled={enviando}
          className="bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium"
        >
          Marcar como resuelta
        </button>
        {error && <p className="text-sm text-red-600 mt-2">{error}</p>}
      </div>
    )
  }

  if (incidencia.estado !== 'PENDIENTE') return null

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
            className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium"
          >
            Confirmar rechazo
          </button>
          <button onClick={() => setModo(null)} className={claseBotonSecundario}>Cancelar</button>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    )
  }

  return (
    <div>
      <div className="flex gap-2">
        <button onClick={() => ejecutar('aprobar')} disabled={enviando} className={claseBoton}>
          Aprobar y publicar
        </button>
        <button onClick={() => setModo('rechazar')} disabled={enviando} className={claseBotonSecundario}>
          Rechazar
        </button>
      </div>
      {error && <p className="text-sm text-red-600 mt-2">{error}</p>}
    </div>
  )
}

import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { hace } from '../utils/formato'
import { claseBoton, claseInput } from './ui'

export default function Comentarios({ incidencia }) {
  const [comentarios, setComentarios] = useState(incidencia.comentarios ?? [])
  const [opciones, setOpciones] = useState([])
  const [elegido, setElegido] = useState('')
  const [error, setError] = useState('')
  const habilitado = incidencia.puedeComentar

  useEffect(() => {
    if (!habilitado) return
    api.get('/comentarios-predefinidos')
      .then(r => setOpciones(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [habilitado])

  const enviar = async e => {
    e.preventDefault()
    if (!elegido) return
    setError('')
    try {
      const { data } = await api.post(`/incidencias/${incidencia.id}/comentarios`, { comentarioPredefinidoId: Number(elegido) })
      setComentarios(c => [...c, data])
      setElegido('')
    } catch (err) {
      setError(mensajeError(err))
    }
  }

  return (
    <section>
      <h2 className="font-semibold text-tinta mb-3">Comentarios ({comentarios.length})</h2>

      {comentarios.length === 0 ? (
        <p className="text-sm text-suave mb-4">Aún no hay comentarios.</p>
      ) : (
        <ul className="space-y-2 mb-4">
          {comentarios.map(c => (
            <li key={c.id} className="bg-fondo border border-linea rounded-xl px-3 py-2 text-sm">
              <span className="text-tinta">{c.texto}</span>
              <span className="block text-xs text-suave mt-0.5">Alumno anónimo · {hace(c.fecha)}</span>
            </li>
          ))}
        </ul>
      )}

      {habilitado ? (
        <form onSubmit={enviar} className="flex gap-2">
          <select value={elegido} onChange={e => setElegido(e.target.value)} className={claseInput}>
            <option value="">Elige un comentario...</option>
            {opciones.map(o => <option key={o.id} value={o.id}>{o.texto}</option>)}
          </select>
          <button disabled={!elegido} className={claseBoton}>Comentar</button>
        </form>
      ) : (
        incidencia.estado === 'PUBLICADA' && (
          <p className="text-xs text-suave">Solo estudiantes suscritos al canal pueden comentar.</p>
        )
      )}
      {error && <p className="text-sm text-red-600 mt-2">{error}</p>}
    </section>
  )
}

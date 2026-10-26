import { useEffect, useRef, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { Alerta, CanalAvatar, Cargando, claseInput } from './ui'
import { partesCanal } from '../utils/formato'

export default function ExplorarCanales({ onCerrar, onCambio }) {
  const [canales, setCanales] = useState(null)
  const [busqueda, setBusqueda] = useState('')
  const [enviando, setEnviando] = useState(null)
  const [error, setError] = useState('')
  const input = useRef(null)

  useEffect(() => {
    api.get('/canales')
      .then(r => setCanales(r.data))
      .catch(e => setError(mensajeError(e)))
    input.current?.focus()
    const escape = e => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', escape)
    return () => document.removeEventListener('keydown', escape)
  }, [onCerrar])

  const alternar = async canal => {
    setEnviando(canal.id)
    setError('')
    try {
      const { data } = canal.suscrito
        ? await api.delete(`/canales/${canal.id}/suscripcion`)
        : await api.post(`/canales/${canal.id}/suscripcion`)
      setCanales(cs => cs.map(c => (c.id === data.id ? data : c)))
      onCambio?.()
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(null)
    }
  }

  const f = busqueda.trim().toLowerCase()
  const visibles = (canales ?? [])
    .filter(c => !f || c.nombre.toLowerCase().includes(f))
    .sort((a, b) => Number(a.suscrito) - Number(b.suscrito) || a.nombre.localeCompare(b.nombre))

  return (
    <div className="fixed inset-0 z-50 grid place-items-start sm:place-items-center bg-tinta/40 p-4 overflow-y-auto" onMouseDown={e => e.target === e.currentTarget && onCerrar()}>
      <div role="dialog" aria-modal="true" aria-labelledby="titulo-explorar" className="aparecer w-full max-w-lg bg-white rounded-3xl shadow-2xl mt-10 sm:mt-0">
        <div className="flex items-center justify-between px-5 pt-5">
          <h2 id="titulo-explorar" className="text-lg font-bold">Suscribirme a un canal</h2>
          <button onClick={onCerrar} className="h-8 w-8 rounded-full hover:bg-fondo text-suave text-xl leading-none" aria-label="Cerrar">×</button>
        </div>
        <p className="px-5 text-sm text-suave mt-1">Cada curso tiene un canal. Verás sus incidencias en tu feed y te avisaremos cuando se publique una nueva.</p>

        <div className="px-5 mt-4">
          <input ref={input} value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Busca por código o nombre del curso" className={claseInput} />
        </div>

        <ul className="mt-3 max-h-[55vh] overflow-y-auto px-2 pb-3">
          {!canales && <li>{error ? <Alerta>{error}</Alerta> : <Cargando />}</li>}
          {canales && error && <li className="px-3 pb-2"><Alerta>{error}</Alerta></li>}
          {canales && visibles.length === 0 && <li className="text-sm text-suave text-center py-8">Ningún curso coincide con “{busqueda}”.</li>}
          {visibles.map(c => {
            const { codigo, nombre } = partesCanal(c.nombre)
            return (
              <li key={c.id} className="flex items-center gap-3 rounded-2xl px-3 py-2.5 hover:bg-fondo">
                <CanalAvatar canal={c} />
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-sm truncate">{nombre} <span className="font-normal text-suave">{codigo}</span></p>
                  <p className="text-xs text-suave">{c.totalSuscritos} suscritos · {c.totalPublicadas} incidencias</p>
                </div>
                <button
                  onClick={() => alternar(c)}
                  disabled={enviando === c.id}
                  className={`group rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors disabled:opacity-50 ${
                    c.suscrito ? 'bg-fondo text-suave hover:bg-red-50 hover:text-red-700' : 'bg-marca text-white hover:bg-marca-oscuro'
                  }`}
                >
                  {c.suscrito ? (
                    <>
                      <span className="group-hover:hidden">Suscrito</span>
                      <span className="hidden group-hover:inline">Salir</span>
                    </>
                  ) : 'Suscribirme'}
                </button>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}

import { useEffect, useRef, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { REACCIONES } from '../utils/formato'

// Funciona como en Facebook: una reacción por persona, se elige desde un selector y se puede cambiar o quitar.
export default function Reacciones({ incidencia, habilitado }) {
  const [reacciones, setReacciones] = useState(incidencia.reacciones)
  const [mia, setMia] = useState(incidencia.miReaccion)
  const [abierto, setAbierto] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const caja = useRef(null)

  useEffect(() => {
    if (!abierto) return
    const cerrar = e => { if (!caja.current?.contains(e.target)) setAbierto(false) }
    const escape = e => { if (e.key === 'Escape') setAbierto(false) }
    document.addEventListener('mousedown', cerrar)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', cerrar)
      document.removeEventListener('keydown', escape)
    }
  }, [abierto])

  const elegir = async tipo => {
    setAbierto(false)
    if (enviando) return
    setEnviando(true)
    setError('')
    try {
      const { data } = await api.put(`/incidencias/${incidencia.id}/reaccion`, { tipo: mia === tipo ? null : tipo })
      setReacciones(data.reacciones)
      setMia(data.miReaccion)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(false)
    }
  }

  const actual = REACCIONES.find(r => r.tipo === mia)
  const presentes = REACCIONES.filter(r => reacciones[r.tipo] > 0)
  const total = presentes.reduce((s, r) => s + reacciones[r.tipo], 0)

  return (
    <div className="flex items-center gap-2" ref={caja}>
      <div className="relative">
        <button
          type="button"
          onClick={() => (actual ? elegir(actual.tipo) : setAbierto(a => !a))}
          onContextMenu={e => { e.preventDefault(); if (habilitado) setAbierto(true) }}
          disabled={!habilitado || enviando}
          aria-haspopup="true"
          aria-expanded={abierto}
          title={habilitado ? (actual ? 'Quitar tu reacción' : 'Reaccionar') : 'Suscríbete al canal para reaccionar'}
          className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
            actual ? 'bg-marca-suave text-marca-oscuro' : 'text-suave enabled:hover:bg-fondo'
          }`}
        >
          <span aria-hidden="true">{actual ? actual.icono : '🤝'}</span>
          {actual ? actual.etiqueta : 'Reaccionar'}
        </button>

        {habilitado && actual && (
          <button
            type="button"
            onClick={() => setAbierto(a => !a)}
            className="absolute -right-1 -top-1 h-4 w-4 rounded-full bg-white text-[9px] text-suave shadow grid place-items-center"
            aria-label="Cambiar reacción"
            title="Cambiar reacción"
          >
            ▾
          </button>
        )}

        {abierto && (
          <div role="menu" className="aparecer absolute bottom-full left-0 mb-2 z-20 flex gap-1 rounded-full bg-white p-1.5 shadow-lg ring-1 ring-linea">
            {REACCIONES.map(r => (
              <button
                key={r.tipo}
                type="button"
                role="menuitem"
                onClick={() => elegir(r.tipo)}
                title={r.etiqueta}
                aria-label={r.etiqueta}
                className={`h-10 w-10 rounded-full text-xl transition-transform hover:scale-125 focus-visible:scale-125 ${mia === r.tipo ? 'bg-marca-suave' : ''}`}
              >
                {r.icono}
              </button>
            ))}
          </div>
        )}
      </div>

      {total > 0 && (
        <span className="flex items-center gap-1 text-sm text-suave" title={presentes.map(r => `${r.etiqueta}: ${reacciones[r.tipo]}`).join(', ')}>
          <span className="flex -space-x-1" aria-hidden="true">
            {presentes.slice(0, 3).map(r => (
              <span key={r.tipo} className="h-5 w-5 rounded-full bg-white ring-2 ring-white grid place-items-center text-xs">{r.icono}</span>
            ))}
          </span>
          {total}
        </span>
      )}

      {error && <span className="text-xs text-red-600">{error}</span>}
    </div>
  )
}

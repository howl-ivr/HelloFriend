import { useState } from 'react'
import api from '../api/client'

export default function BotonConfirmar({ incidencia, compacto = false }) {
  const [total, setTotal] = useState(incidencia.confirmaciones)
  const [mia, setMia] = useState(incidencia.confirmadaPorMi)
  const [enviando, setEnviando] = useState(false)
  const habilitado = incidencia.puedeConfirmar

  const confirmar = async () => {
    setEnviando(true)
    try {
      const { data } = await api.post(`/incidencias/${incidencia.id}/confirmacion`)
      setTotal(data.confirmaciones)
      setMia(data.confirmadaPorMi)
    } finally {
      setEnviando(false)
    }
  }

  const motivo = incidencia.esMia
    ? 'No puedes confirmar tu propia incidencia'
    : incidencia.estado !== 'PUBLICADA'
      ? 'Esta incidencia ya no recibe confirmaciones'
      : 'Solo estudiantes suscritos al canal pueden confirmar'

  const boton = (
    <button
      type="button"
      onClick={confirmar}
      disabled={!habilitado || enviando}
      title={habilitado ? 'Confirmar aumenta la prioridad de la incidencia' : motivo}
      aria-pressed={mia}
      className={`flex items-center gap-1.5 rounded-full font-semibold transition-colors disabled:cursor-not-allowed ${
        compacto ? 'px-3 py-1.5 text-sm' : 'px-4 py-2 text-sm'
      } ${
        mia
          ? 'bg-confirma text-white'
          : habilitado
            ? 'bg-confirma-suave text-confirma hover:bg-confirma hover:text-white'
            : 'bg-fondo text-suave'
      }`}
    >
      <span aria-hidden="true">✔</span>
      {mia ? 'Confirmado' : 'Me pasa también'}
      <span className={`ml-0.5 rounded-full px-1.5 text-xs ${mia ? 'bg-white/25' : 'bg-white/70'}`}>{total}</span>
    </button>
  )

  if (compacto) return boton
  return (
    <div>
      {boton}
      <p className="text-xs text-suave mt-1.5">{habilitado ? 'Confirmar aumenta la prioridad de la incidencia' : motivo}</p>
    </div>
  )
}

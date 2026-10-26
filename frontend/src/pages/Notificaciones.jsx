import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client'
import { Cargando, Titulo, Vacio, claseBotonSecundario } from '../components/ui'
import { hace } from '../utils/formato'

export default function Notificaciones() {
  const [lista, setLista] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/notificaciones').then(r => setLista(r.data))
  }, [])

  const abrir = async n => {
    if (!n.leida) await api.patch(`/notificaciones/${n.id}/leida`)
    if (n.incidenciaId) navigate(`/incidencias/${n.incidenciaId}`)
    else setLista(l => l.map(x => (x.id === n.id ? { ...x, leida: true } : x)))
  }

  const marcarTodas = async () => {
    await api.patch('/notificaciones/leidas')
    setLista(l => l.map(n => ({ ...n, leida: true })))
  }

  if (!lista) return <Cargando />
  const hayNoLeidas = lista.some(n => !n.leida)

  return (
    <div>
      <Titulo accion={hayNoLeidas && <button onClick={marcarTodas} className={claseBotonSecundario}>Marcar todas como leídas</button>}>
        Notificaciones
      </Titulo>

      {lista.length === 0 ? (
        <Vacio>No tienes notificaciones.</Vacio>
      ) : (
        <ul className="bg-white border border-linea rounded-2xl divide-y divide-linea">
          {lista.map(n => (
            <li key={n.id}>
              <button onClick={() => abrir(n)} className="w-full text-left px-4 py-3 hover:bg-fondo flex gap-3">
                <span className={`mt-1.5 h-2 w-2 rounded-full shrink-0 ${n.leida ? 'bg-transparent' : 'bg-confirma'}`} />
                <span className="flex-1">
                  <span className={`block text-sm ${n.leida ? 'text-suave' : 'text-tinta font-medium'}`}>{n.mensaje}</span>
                  <span className="block text-xs text-suave mt-0.5">{hace(n.fecha)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

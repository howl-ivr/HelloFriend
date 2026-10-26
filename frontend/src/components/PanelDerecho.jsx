import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { Tarjeta } from './ui'
import { partesCanal } from '../utils/formato'

function LoMasConfirmado({ version }) {
  const [lista, setLista] = useState(null)

  useEffect(() => {
    api.get('/feed/destacadas').then(r => setLista(r.data)).catch(() => setLista([]))
  }, [version])

  return (
    <Tarjeta className="p-4">
      <h2 className="font-bold">Lo más confirmado</h2>
      <p className="text-xs text-suave mb-3">Incidencias que más compañeros viven en tus canales</p>
      {lista && lista.length === 0 && (
        <p className="text-sm text-suave">Cuando tus compañeros confirmen incidencias, las más respaldadas aparecerán aquí.</p>
      )}
      <ol className="space-y-3">
        {(lista ?? []).map(i => (
          <li key={i.id}>
            <Link to={`/incidencias/${i.id}`} className="group flex gap-3">
              <span className="shrink-0 h-10 w-10 rounded-xl bg-confirma-suave text-confirma grid place-items-center leading-none" title={`${i.confirmaciones} confirmaciones`}>
                <span className="text-sm font-bold">{i.confirmaciones}</span>
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-semibold leading-snug line-clamp-2 group-hover:text-marca">{i.titulo}</span>
                <span className="block text-xs text-suave mt-0.5">{partesCanal(i.canal.nombre).nombre}</span>
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </Tarjeta>
  )
}

const RESUMEN = [
  { estado: 'PENDIENTE', etiqueta: 'En revisión', color: 'bg-amber-400' },
  { estado: 'PUBLICADA', etiqueta: 'Publicadas', color: 'bg-marca' },
  { estado: 'RESUELTA', etiqueta: 'Resueltas', color: 'bg-confirma' },
  { estado: 'RECHAZADA', etiqueta: 'Rechazadas', color: 'bg-red-500' }
]

function TusReportes() {
  const [lista, setLista] = useState(null)

  useEffect(() => {
    api.get('/mis-incidencias').then(r => setLista(r.data)).catch(() => setLista([]))
  }, [])

  if (!lista) return null
  return (
    <Tarjeta className="p-4">
      <div className="flex items-baseline justify-between mb-3">
        <h2 className="font-bold">Tus reportes</h2>
        <Link to="/mis-incidencias" className="text-sm font-semibold text-marca hover:underline">Ver todos</Link>
      </div>
      {lista.length === 0 ? (
        <p className="text-sm text-suave">Aún no has reportado nada. Lo que publiques solo lo verá la dirección hasta que lo apruebe.</p>
      ) : (
        <ul className="space-y-2">
          {RESUMEN.map(r => (
            <li key={r.estado} className="flex items-center gap-2 text-sm">
              <span className={`h-2 w-2 rounded-full ${r.color}`} aria-hidden="true" />
              <span className="flex-1">{r.etiqueta}</span>
              <span className="font-semibold">{lista.filter(i => i.estado === r.estado).length}</span>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}

function TuIdentidad() {
  return (
    <div className="rounded-2xl p-4 bg-tinta text-white">
      <h2 className="font-bold">Tu identidad está protegida</h2>
      <ul className="mt-2 space-y-1.5 text-sm text-white/80">
        <li>Nadie ve quién publica, ni siquiera la dirección.</li>
        <li>Tu correo solo sirve para validar tu cuenta.</li>
        <li>Las respuestas son predefinidas para evitar ataques personales.</li>
      </ul>
    </div>
  )
}

export default function PanelDerecho({ rol, version }) {
  return (
    <aside className="space-y-4">
      <LoMasConfirmado version={version} />
      {rol === 'ESTUDIANTE' && <TusReportes />}
      <TuIdentidad />
    </aside>
  )
}

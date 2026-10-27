import { CanalAvatar } from './ui'
import { partesCanal } from '../utils/formato'

function IconoTodos() {
  return (
    <span className="h-9 w-9 shrink-0 rounded-xl bg-tinta text-white grid place-items-center" aria-hidden="true">
      <svg viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current stroke-2">
        <path d="M4 6h16M4 12h16M4 18h10" strokeLinecap="round" />
      </svg>
    </span>
  )
}

export function BotonSuscribir({ onClick, className = '' }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-center gap-2 rounded-full bg-marca hover:bg-marca-oscuro text-white text-sm font-semibold px-4 py-2.5 transition-colors ${className}`}
    >
      <span className="text-lg leading-none" aria-hidden="true">+</span>
      Suscribirme a un canal
    </button>
  )
}

export default function SidebarCanales({ canales, activo, onElegir, onExplorar }) {
  const item = seleccionado =>
    `w-full flex items-center gap-3 rounded-2xl px-2 py-2 text-left transition-colors ${
      seleccionado ? 'bg-white shadow-sm ring-1 ring-linea' : 'hover:bg-white/70'
    }`

  return (
    <nav aria-label="Tus canales" className="space-y-4">
      <BotonSuscribir onClick={onExplorar} className="w-full" />

      <div>
        <h2 className="px-2 mb-1.5 text-sm font-semibold text-suave">Tus canales</h2>
        <ul className="space-y-0.5">
          <li>
            <button onClick={() => onElegir(null)} className={item(!activo)} aria-current={!activo ? 'page' : undefined}>
              <IconoTodos />
              <span className="text-sm font-semibold">Todo tu feed</span>
            </button>
          </li>
          {canales.map(c => {
            const { codigo, nombre } = partesCanal(c.nombre)
            return (
              <li key={c.id}>
                <button onClick={() => onElegir(c.id)} className={item(activo === c.id)} aria-current={activo === c.id ? 'page' : undefined}>
                  <CanalAvatar canal={c} />
                  <span className="min-w-0 leading-tight">
                    <span className="block text-sm font-semibold truncate">{nombre}</span>
                    <span className="block text-xs text-suave">{codigo} · {c.periodo}</span>
                  </span>
                </button>
              </li>
            )
          })}
        </ul>
        {canales.length === 0 && (
          <p className="px-2 text-sm text-suave">Aún no sigues ningún curso. Suscríbete a los que llevas para ver lo que pasa en ellos.</p>
        )}
      </div>
    </nav>
  )
}

export function ChipsCanales({ canales, activo, onElegir, onExplorar }) {
  const chip = sel => `shrink-0 flex items-center gap-2 rounded-full pl-1 pr-3 py-1 text-sm font-semibold ${sel ? 'bg-tinta text-white' : 'bg-white text-tinta'}`
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 -mx-4 px-4">
      <button onClick={onExplorar} className="shrink-0 rounded-full bg-marca text-white px-3 py-1 text-sm font-semibold">+ Canal</button>
      <button onClick={() => onElegir(null)} className={chip(!activo)}><span className="pl-2">Todo</span></button>
      {canales.map(c => (
        <button key={c.id} onClick={() => onElegir(c.id)} className={chip(activo === c.id)}>
          <CanalAvatar canal={c} tamaño="sm" />
          {partesCanal(c.nombre).codigo} <span className="font-normal opacity-70">{c.periodo}</span>
        </button>
      ))}
    </div>
  )
}

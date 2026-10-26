import { ESTADOS, PRIORIDADES, partesCanal, tonoCanal } from '../utils/formato'

export function Cargando({ texto = 'Cargando...' }) {
  return <p className="text-center text-suave py-10">{texto}</p>
}

export function Vacio({ children }) {
  return (
    <div className="text-center text-suave py-10 px-6 rounded-2xl bg-white/70 border border-dashed border-linea">
      {children}
    </div>
  )
}

export function Alerta({ tipo = 'error', children }) {
  const estilos = {
    error: 'bg-red-50 text-red-700 border-red-200',
    exito: 'bg-confirma-suave text-confirma border-confirma/20',
    info: 'bg-marca-suave text-marca-oscuro border-marca/20'
  }
  return <div className={`border rounded-xl px-3 py-2 text-sm ${estilos[tipo]}`}>{children}</div>
}

export function EstadoBadge({ estado }) {
  const e = ESTADOS[estado]
  if (!e) return null
  return <span className={`text-xs px-2 py-0.5 rounded-full whitespace-nowrap ${e.clase}`}>{e.etiqueta}</span>
}

export function PrioridadBadge({ prioridad }) {
  const p = PRIORIDADES[prioridad]
  if (!p) return null
  return <span className={`text-xs px-2 py-0.5 rounded-full whitespace-nowrap ${p.clase}`}>{p.etiqueta}</span>
}

export function CategoriaChip({ categoria }) {
  if (!categoria) return null
  return <span className="text-xs px-2 py-0.5 rounded-full whitespace-nowrap bg-fondo text-suave border border-linea">{categoria}</span>
}

export function Titulo({ children, subtitulo, accion }) {
  return (
    <div className="flex items-start justify-between gap-4 mb-5">
      <div>
        <h1 className="text-2xl font-bold">{children}</h1>
        {subtitulo && <p className="text-sm text-suave mt-1">{subtitulo}</p>}
      </div>
      {accion}
    </div>
  )
}

export function CanalAvatar({ canal, tamaño = 'md' }) {
  const { codigo } = partesCanal(canal.nombre)
  const medidas = { sm: 'h-7 w-7 text-[10px]', md: 'h-9 w-9 text-xs', lg: 'h-12 w-12 text-sm' }
  return (
    <span className={`${medidas[tamaño]} ${tonoCanal(canal.id)} shrink-0 rounded-xl font-bold grid place-items-center`} aria-hidden="true">
      {codigo.replace(/^[A-Z]+/, '') || codigo.slice(0, 3)}
    </span>
  )
}

export function AvatarAnonimo({ tamaño = 'md' }) {
  const medidas = { sm: 'h-7 w-7', md: 'h-9 w-9', lg: 'h-11 w-11' }
  return (
    <span className={`${medidas[tamaño]} shrink-0 rounded-full bg-tinta grid place-items-center`} aria-hidden="true">
      <svg viewBox="0 0 24 24" className="h-1/2 w-1/2 fill-white/90">
        <path d="M12 3c-4.4 0-8 2.2-8 6.2 0 2.9 1.6 5.4 3.6 7l.4 3.3c.1.8.8 1.5 1.6 1.5h4.8c.8 0 1.5-.7 1.6-1.5l.4-3.3c2-1.6 3.6-4.1 3.6-7C20 5.2 16.4 3 12 3Zm-3.3 9.2a1.7 1.7 0 1 1 0-3.4 1.7 1.7 0 0 1 0 3.4Zm6.6 0a1.7 1.7 0 1 1 0-3.4 1.7 1.7 0 0 1 0 3.4Z" />
      </svg>
    </span>
  )
}

export function Tarjeta({ children, className = '' }) {
  return <div className={`bg-white rounded-2xl shadow-[0_1px_2px_rgba(27,34,56,0.06),0_4px_16px_-8px_rgba(27,34,56,0.12)] ${className}`}>{children}</div>
}

export const claseInput = 'w-full border border-linea rounded-xl px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-marca/40 focus:border-marca'
export const claseBoton = 'bg-marca hover:bg-marca-oscuro disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-semibold transition-colors'
export const claseBotonSecundario = 'border border-linea bg-white hover:bg-fondo disabled:opacity-50 text-tinta rounded-full px-4 py-2 text-sm font-medium transition-colors'

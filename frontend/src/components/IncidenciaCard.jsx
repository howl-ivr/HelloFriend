import { Link } from 'react-router-dom'
import { AvatarAnonimo, CanalAvatar, CategoriaChip, EstadoBadge, PrioridadBadge, Tarjeta } from './ui'
import BotonConfirmar from './BotonConfirmar'
import Reacciones from './Reacciones'
import { fechaHora, hace, partesCanal } from '../utils/formato'

export default function IncidenciaCard({ incidencia, onElegirCanal }) {
  const i = incidencia
  const { codigo, nombre } = partesCanal(i.canal.nombre)

  return (
    <Tarjeta className="p-4 sm:p-5">
      <header className="flex items-center gap-3 mb-3">
        <CanalAvatar canal={i.canal} />
        <div className="min-w-0 flex-1 leading-tight">
          <button
            type="button"
            onClick={() => onElegirCanal?.(i.canal.id)}
            className="font-semibold text-sm hover:underline truncate max-w-full text-left"
          >
            {nombre} <span className="text-suave font-normal">{codigo} · {i.canal.periodo}</span>
          </button>
          <p className="flex items-center gap-1.5 text-xs text-suave mt-0.5">
            <AvatarAnonimo tamaño="sm" />
            <span>Alumno anónimo{i.esMia && ' (tú)'}</span>
            <span aria-hidden="true">·</span>
            <time dateTime={i.fechaCreacion} title={fechaHora(i.fechaCreacion)}>{hace(i.fechaCreacion)}</time>
          </p>
        </div>
        {i.estado !== 'PUBLICADA' && <EstadoBadge estado={i.estado} />}
      </header>

      <Link to={`/incidencias/${i.id}`} className="group block">
        <h3 className="text-lg font-bold leading-snug group-hover:text-marca transition-colors">{i.titulo}</h3>
        <p className="text-[15px] leading-relaxed text-tinta/85 mt-1 line-clamp-4">{i.descripcion}</p>
      </Link>

      <div className="flex flex-wrap items-center gap-1.5 mt-3">
        <PrioridadBadge prioridad={i.prioridad} />
        <CategoriaChip categoria={i.categoria} />
      </div>

      <footer className="flex flex-wrap items-center gap-1 mt-4 pt-3 border-t border-linea">
        <BotonConfirmar incidencia={i} compacto />
        <Reacciones incidencia={i} habilitado={i.puedeReaccionar} />
        <Link
          to={`/incidencias/${i.id}#comentarios`}
          className="ml-auto flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium text-suave hover:bg-fondo"
        >
          <span aria-hidden="true">💬</span>
          {i.totalComentarios === 1 ? '1 comentario' : `${i.totalComentarios} comentarios`}
        </Link>
      </footer>
    </Tarjeta>
  )
}

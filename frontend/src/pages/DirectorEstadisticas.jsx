import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, PrioridadBadge, Tarjeta, Titulo, Vacio, claseInput } from '../components/ui'
import { ESTADOS } from '../utils/formato'

function duracion(horas) {
  if (horas == null) return 'Sin resueltas'
  if (horas < 1) return 'menos de 1 h'
  if (horas < 48) return `${Math.round(horas)} h`
  const dias = Math.floor(horas / 24)
  const resto = Math.round(horas - dias * 24)
  return resto ? `${dias} d ${resto} h` : `${dias} d`
}

function Dato({ etiqueta, valor, nota }) {
  return (
    <Tarjeta className="p-4">
      <p className="text-sm text-suave">{etiqueta}</p>
      <p className="text-2xl font-bold text-tinta mt-1">{valor}</p>
      {nota && <p className="text-xs text-suave mt-1">{nota}</p>}
    </Tarjeta>
  )
}

function Barras({ titulo, filas, vacio = 'Sin datos en este rango.' }) {
  const max = Math.max(1, ...filas.map(f => f.total))
  const hayDatos = filas.some(f => f.total > 0)
  return (
    <Tarjeta className="p-5">
      <h2 className="font-bold mb-3">{titulo}</h2>
      {!hayDatos ? (
        <p className="text-sm text-suave">{vacio}</p>
      ) : (
        <ul className="space-y-2.5">
          {filas.map(f => (
            <li key={f.nombre} className="text-sm">
              <div className="flex items-baseline justify-between gap-3">
                <span className="truncate">{f.nombre}</span>
                <span className="font-semibold tabular-nums">{f.total}</span>
              </div>
              <div className="h-2 rounded-full bg-fondo mt-1" aria-hidden="true">
                <div className="h-2 rounded-full bg-marca" style={{ width: `${(f.total / max) * 100}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}

export default function DirectorEstadisticas() {
  const [filtros, setFiltros] = useState({ desde: '', hasta: '', periodo: '' })
  const [periodos, setPeriodos] = useState([])
  const [res, setRes] = useState({ clave: null, datos: null, error: '' })
  const rangoInvalido = Boolean(filtros.desde && filtros.hasta && filtros.desde > filtros.hasta)
  const clave = JSON.stringify(filtros)

  useEffect(() => {
    api.get('/periodos').then(r => setPeriodos(r.data)).catch(() => setPeriodos([]))
  }, [])

  useEffect(() => {
    if (rangoInvalido) return
    let vigente = true
    const k = JSON.stringify(filtros)
    const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v))
    api.get('/director/estadisticas', { params })
      .then(r => vigente && setRes({ clave: k, datos: r.data, error: '' }))
      .catch(e => vigente && setRes({ clave: k, datos: null, error: mensajeError(e) }))
    return () => { vigente = false }
  }, [filtros, rangoInvalido])

  const cambiar = campo => e => setFiltros(f => ({ ...f, [campo]: e.target.value }))
  const d = res.datos
  const cargando = !rangoInvalido && res.clave !== clave

  return (
    <div>
      <Titulo subtitulo="Identifica los problemas que más se repiten en la carrera. Las fechas se aplican a la fecha de registro de cada incidencia.">
        Estadísticas
      </Titulo>

      <div className="grid gap-3 sm:grid-cols-3 mb-5">
        <label className="text-sm font-medium text-tinta">Desde
          <input type="date" value={filtros.desde} onChange={cambiar('desde')} className={`${claseInput} mt-1`} />
        </label>
        <label className="text-sm font-medium text-tinta">Hasta
          <input type="date" value={filtros.hasta} onChange={cambiar('hasta')} className={`${claseInput} mt-1`} />
        </label>
        <label className="text-sm font-medium text-tinta">Periodo académico
          <select value={filtros.periodo} onChange={cambiar('periodo')} className={`${claseInput} mt-1`}>
            <option value="">Todos</option>
            {periodos.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
        </label>
      </div>

      {rangoInvalido ? (
        <Alerta>La fecha inicial no puede ser posterior a la final.</Alerta>
      ) : cargando ? (
        <Cargando />
      ) : res.error ? (
        <Alerta>{res.error}</Alerta>
      ) : d.total === 0 ? (
        <Vacio>No hay incidencias en este rango.</Vacio>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <Dato etiqueta="Incidencias registradas" valor={d.total} />
            <Dato etiqueta="Resueltas" valor={d.resueltas} nota={`${Math.round((d.resueltas / d.total) * 100)}% del total`} />
            <Dato etiqueta="Tiempo promedio de resolución" valor={duracion(d.tiempoPromedioHoras)} nota="Desde la aprobación hasta la resolución" />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Barras titulo="Por estado" filas={d.porEstado.map(x => ({ nombre: ESTADOS[x.estado].etiqueta, total: x.total }))} />
            <Barras titulo="Por categoría" filas={d.porCategoria} />
          </div>
          <Barras titulo="Por curso" filas={d.porCurso} />

          <Tarjeta className="p-5">
            <h2 className="font-bold">Más confirmadas</h2>
            <p className="text-xs text-suave mb-3">Las cinco incidencias con más respaldo de los alumnos</p>
            {d.masConfirmadas.length === 0 ? (
              <p className="text-sm text-suave">Aún no hay incidencias confirmadas en este rango.</p>
            ) : (
              <ol className="space-y-3">
                {d.masConfirmadas.map(i => (
                  <li key={i.id}>
                    <Link to={`/incidencias/${i.id}`} className="group flex gap-3">
                      <span className="shrink-0 h-10 w-10 rounded-xl bg-confirma-suave text-confirma grid place-items-center text-sm font-bold" title={`${i.confirmaciones} confirmaciones`}>
                        {i.confirmaciones}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-semibold leading-snug group-hover:text-marca">{i.titulo}</span>
                        <span className="flex flex-wrap items-center gap-1.5 text-xs text-suave mt-0.5">
                          {i.canal}
                          <PrioridadBadge prioridad={i.prioridad} />
                        </span>
                      </span>
                    </Link>
                  </li>
                ))}
              </ol>
            )}
          </Tarjeta>
        </div>
      )}
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, EstadoBadge, PrioridadBadge, Titulo, Vacio, claseInput } from '../components/ui'
import { ESTADOS, fechaHora } from '../utils/formato'

const ACCIONES = {
  CUENTA_REGISTRADA: 'Cuenta registrada',
  CUENTA_DESACTIVADA: 'Cuenta desactivada',
  CUENTA_REACTIVADA: 'Cuenta reactivada',
  ROL_ASIGNADO: 'Rol asignado',
  CURSO_REGISTRADO: 'Curso registrado',
  CANAL_CREADO: 'Canal creado',
  CANAL_DESACTIVADO: 'Canal desactivado',
  INCIDENCIA_REGISTRADA: 'Incidencia registrada',
  INCIDENCIA_EDITADA: 'Incidencia editada',
  INCIDENCIA_OBSERVADA: 'Incidencia observada',
  INCIDENCIA_REENVIADA: 'Incidencia reenviada',
  INCIDENCIA_APROBADA: 'Incidencia aprobada',
  INCIDENCIA_RECHAZADA: 'Incidencia rechazada',
  INCIDENCIA_EN_ATENCION: 'Incidencia en atención',
  INCIDENCIA_RESUELTA: 'Incidencia resuelta',
  INCIDENCIA_CONFIRMADA: 'Incidencia confirmada',
  INCIDENCIA_CONFIRMACION_RETIRADA: 'Confirmación retirada'
}

const FILTROS_VACIOS = { desde: '', hasta: '', cursoId: '', estado: '', usuario: '' }

function Reportes() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [cursos, setCursos] = useState([])
  const [res, setRes] = useState({ clave: null, lista: [], error: '' })
  const clave = JSON.stringify(filtros)

  useEffect(() => {
    api.get('/admin/cursos').then(r => setCursos(r.data)).catch(() => setCursos([]))
  }, [])

  useEffect(() => {
    let vigente = true
    const k = JSON.stringify(filtros)
    const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v))
    api.get('/admin/reportes', { params })
      .then(r => vigente && setRes({ clave: k, lista: r.data, error: '' }))
      .catch(e => vigente && setRes({ clave: k, lista: [], error: mensajeError(e) }))
    return () => { vigente = false }
  }, [filtros])

  const cambiar = campo => e => setFiltros(f => ({ ...f, [campo]: e.target.value }))
  const cargando = res.clave !== clave

  return (
    <div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5 mb-4">
        <label className="text-sm font-medium text-tinta">Desde
          <input type="date" value={filtros.desde} onChange={cambiar('desde')} className={`${claseInput} mt-1`} />
        </label>
        <label className="text-sm font-medium text-tinta">Hasta
          <input type="date" value={filtros.hasta} onChange={cambiar('hasta')} className={`${claseInput} mt-1`} />
        </label>
        <label className="text-sm font-medium text-tinta">Curso
          <select value={filtros.cursoId} onChange={cambiar('cursoId')} className={`${claseInput} mt-1`}>
            <option value="">Todos</option>
            {cursos.map(c => <option key={c.id} value={c.id}>{c.codigo} · {c.nombre}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium text-tinta">Estado
          <select value={filtros.estado} onChange={cambiar('estado')} className={`${claseInput} mt-1`}>
            <option value="">Todos</option>
            {Object.entries(ESTADOS).map(([valor, e]) => <option key={valor} value={valor}>{e.etiqueta}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium text-tinta">Usuario
          <input value={filtros.usuario} onChange={cambiar('usuario')} placeholder="Autor, confirmante o director" className={`${claseInput} mt-1`} />
        </label>
      </div>

      {cargando ? (
        <Cargando />
      ) : res.error ? (
        <Alerta>{res.error}</Alerta>
      ) : res.lista.length === 0 ? (
        <Vacio>No hay reportes con esos filtros.</Vacio>
      ) : (
        <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-fondo text-left text-suave">
              <tr>
                <th className="px-4 py-2 font-medium">Reporte</th>
                <th className="px-4 py-2 font-medium">Curso</th>
                <th className="px-4 py-2 font-medium">Autor</th>
                <th className="px-4 py-2 font-medium">Estado</th>
                <th className="px-4 py-2 font-medium">Prioridad</th>
                <th className="px-4 py-2 font-medium text-right">Versiones</th>
                <th className="px-4 py-2 font-medium">Fecha</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-linea">
              {res.lista.map(r => (
                <tr key={r.id}>
                  <td className="px-4 py-2">
                    <Link to={`/admin/auditoria/${r.id}`} className="font-medium text-tinta hover:text-marca hover:underline">#{r.id} {r.titulo}</Link>
                  </td>
                  <td className="px-4 py-2 whitespace-nowrap">{r.canal.nombre} <span className="text-suave">· {r.canal.periodo}</span></td>
                  <td className="px-4 py-2 font-mono text-xs">{r.autor}</td>
                  <td className="px-4 py-2"><EstadoBadge estado={r.estado} /></td>
                  <td className="px-4 py-2"><PrioridadBadge prioridad={r.prioridad} /></td>
                  <td className="px-4 py-2 text-right">{r.versiones}</td>
                  <td className="px-4 py-2 whitespace-nowrap text-suave">{fechaHora(r.fechaCreacion)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function Actividad() {
  const [registros, setRegistros] = useState(null)
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('')

  useEffect(() => {
    api.get('/admin/auditoria')
      .then(r => setRegistros(r.data))
      .catch(e => setError(mensajeError(e)))
  }, [])

  if (error) return <Alerta>{error}</Alerta>
  if (!registros) return <Cargando />

  const f = filtro.trim().toLowerCase()
  const visibles = registros.filter(r =>
    !f || [ACCIONES[r.accion], r.actor, r.detalle].some(v => v?.toLowerCase().includes(f))
  )

  return (
    <div>
      <input value={filtro} onChange={e => setFiltro(e.target.value)} placeholder="Filtrar por acción, actor o detalle..." className={`${claseInput} mb-4`} />

      {visibles.length === 0 ? (
        <Vacio>Sin registros.</Vacio>
      ) : (
        <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-fondo text-left text-suave">
              <tr>
                <th className="px-4 py-2 font-medium">Fecha</th>
                <th className="px-4 py-2 font-medium">Acción</th>
                <th className="px-4 py-2 font-medium">Actor</th>
                <th className="px-4 py-2 font-medium">Incidencia</th>
                <th className="px-4 py-2 font-medium">Detalle</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-linea">
              {visibles.map(r => (
                <tr key={r.id}>
                  <td className="px-4 py-2 whitespace-nowrap text-suave">{fechaHora(r.fecha)}</td>
                  <td className="px-4 py-2 whitespace-nowrap">{ACCIONES[r.accion] ?? r.accion}</td>
                  <td className="px-4 py-2 whitespace-nowrap font-mono text-xs">{r.actor}</td>
                  <td className="px-4 py-2">
                    {r.incidenciaId ? <Link to={`/admin/auditoria/${r.incidenciaId}`} className="hover:text-marca hover:underline">#{r.incidenciaId}</Link> : '—'}
                  </td>
                  <td className="px-4 py-2 text-suave">{r.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default function AdminAuditoria() {
  const [vista, setVista] = useState('reportes')
  const pestaña = valor =>
    `px-3 py-1.5 text-sm rounded-full ${vista === valor ? 'bg-tinta text-white' : 'text-suave hover:bg-linea'}`

  return (
    <div>
      <Titulo subtitulo="Solo lectura. Consulta el historial completo de cada reporte, con autor, confirmantes y responsables, o el registro general del sistema.">
        Auditoría
      </Titulo>

      <div className="flex gap-2 mb-4">
        <button onClick={() => setVista('reportes')} className={pestaña('reportes')}>Historial por reporte</button>
        <button onClick={() => setVista('actividad')} className={pestaña('actividad')}>Actividad del sistema</button>
      </div>

      {vista === 'reportes' ? <Reportes /> : <Actividad />}
    </div>
  )
}

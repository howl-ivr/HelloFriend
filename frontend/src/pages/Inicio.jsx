import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import IncidenciaCard from '../components/IncidenciaCard'
import ExplorarCanales from '../components/ExplorarCanales'
import SidebarCanales, { BotonSuscribir, ChipsCanales } from '../components/SidebarCanales'
import PanelDerecho from '../components/PanelDerecho'
import { Alerta, AvatarAnonimo, CanalAvatar, Cargando, Tarjeta, Vacio } from '../components/ui'
import { partesCanal } from '../utils/formato'

export default function Inicio() {
  const { rol } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const canalId = params.get('canal') || null

  const [todos, setTodos] = useState(null)
  const [errorCanales, setErrorCanales] = useState('')
  const [orden, setOrden] = useState('recientes')
  const [prioridad, setPrioridad] = useState('')
  const [version, setVersion] = useState(0)
  const [feed, setFeed] = useState({ clave: null, lista: [], error: '' })
  const [explorando, setExplorando] = useState(params.get('explorar') === '1')

  const clave = `${canalId}|${orden}|${prioridad}|${version}`

  useEffect(() => {
    api.get('/canales')
      .then(r => setTodos(r.data))
      .catch(e => setErrorCanales(mensajeError(e)))
  }, [version])

  useEffect(() => {
    const p = { orden }
    if (canalId) p.canalId = canalId
    if (prioridad) p.prioridad = prioridad
    const k = `${canalId}|${orden}|${prioridad}|${version}`
    api.get('/feed', { params: p })
      .then(r => setFeed({ clave: k, lista: r.data, error: '' }))
      .catch(e => setFeed({ clave: k, lista: [], error: mensajeError(e) }))
  }, [canalId, orden, prioridad, version])

  const elegirCanal = id => setParams(id ? { canal: String(id) } : {})
  const cerrarExplorar = useCallback(() => setExplorando(false), [])
  const huboCambio = useCallback(() => setVersion(v => v + 1), [])

  if (!todos) return errorCanales ? <Alerta>{errorCanales}</Alerta> : <Cargando />

  const suscritos = todos.filter(c => c.suscrito)
  const activo = canalId ? todos.find(c => c.id === canalId) : null
  const cargando = feed.clave !== clave

  const reportar = () => navigate(canalId ? `/incidencias/nueva?canal=${canalId}` : '/incidencias/nueva')
  const pestaña = valor =>
    `rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors ${orden === valor ? 'bg-tinta text-white' : 'text-suave hover:bg-white'}`

  return (
    <div className="grid gap-6 md:grid-cols-[220px_minmax(0,1fr)] lg:grid-cols-[240px_minmax(0,1fr)_288px]">
      <div className="hidden md:block">
        <div className="sticky top-20">
          <SidebarCanales canales={suscritos} activo={canalId} onElegir={elegirCanal} onExplorar={() => setExplorando(true)} />
        </div>
      </div>

      <main className="min-w-0 space-y-4">
        <div className="md:hidden">
          <ChipsCanales canales={suscritos} activo={canalId} onElegir={elegirCanal} onExplorar={() => setExplorando(true)} />
        </div>

        {activo && (
          <Tarjeta className="p-5 flex items-center gap-4">
            <CanalAvatar canal={activo} tamaño="lg" />
            <div className="min-w-0 flex-1">
              <h1 className="text-xl font-bold leading-tight">{partesCanal(activo.nombre).nombre}</h1>
              <p className="text-sm text-suave">{partesCanal(activo.nombre).codigo} · {activo.periodo} · {activo.totalSuscritos} suscritos</p>
              <p className="text-sm mt-1">{activo.descripcion}</p>
            </div>
          </Tarjeta>
        )}

        {activo && !activo.activo && (
          <Alerta tipo="info">Este canal está desactivado: conserva sus incidencias, pero ya no admite nuevas.</Alerta>
        )}

        {rol === 'ESTUDIANTE' && (!activo || activo.activo) && (
          <Tarjeta className="p-3 flex items-center gap-3">
            <AvatarAnonimo />
            <button
              onClick={reportar}
              className="flex-1 text-left rounded-full bg-fondo hover:bg-linea/60 px-4 py-2.5 text-[15px] text-suave transition-colors"
            >
              {activo ? `¿Qué está pasando en ${partesCanal(activo.nombre).nombre}?` : '¿Qué está pasando en tu curso?'}
            </button>
          </Tarjeta>
        )}

        <div className="flex items-center gap-1">
          <button onClick={() => setOrden('recientes')} className={pestaña('recientes')}>Recientes</button>
          <button onClick={() => setOrden('prioridad')} className={pestaña('prioridad')}>Más confirmadas</button>
          <select
            value={prioridad}
            onChange={e => setPrioridad(e.target.value)}
            aria-label="Filtrar por prioridad"
            className="ml-auto rounded-full border border-linea bg-white px-3 py-1.5 text-sm font-medium text-suave focus:outline-none focus:ring-2 focus:ring-marca/40"
          >
            <option value="">Toda prioridad</option>
            <option value="ALTA">Prioridad alta</option>
            <option value="MEDIA">Prioridad media</option>
            <option value="BAJA">Prioridad baja</option>
          </select>
        </div>

        {suscritos.length === 0 ? (
          <Vacio>
            <p className="font-semibold text-tinta mb-1">Tu feed está vacío</p>
            <p className="mb-4">Suscríbete a los canales de tus cursos para ver lo que reportan tus compañeros.</p>
            <BotonSuscribir onClick={() => setExplorando(true)} className="mx-auto" />
          </Vacio>
        ) : cargando ? (
          <Cargando />
        ) : feed.error ? (
          <Vacio>
            <p className="mb-4">{feed.error}</p>
            <button onClick={() => elegirCanal(null)} className="text-marca font-semibold hover:underline">Volver a todo tu feed</button>
          </Vacio>
        ) : feed.lista.length === 0 ? (
          <Vacio>
            {activo ? 'Aún no hay incidencias publicadas en este canal.' : 'Aún no hay incidencias publicadas en tus canales.'}
            {rol === 'ESTUDIANTE' && (
              <p className="mt-2">Si algo te afecta, <button onClick={reportar} className="text-marca font-semibold hover:underline">repórtalo de forma anónima</button>.</p>
            )}
          </Vacio>
        ) : (
          feed.lista.map(i => <IncidenciaCard key={i.id} incidencia={i} onElegirCanal={elegirCanal} />)
        )}

        {rol === 'ESTUDIANTE' && (
          <p className="text-center text-xs text-suave pt-2 lg:hidden">
            <Link to="/mis-incidencias" className="hover:underline">Ver el estado de tus reportes</Link>
          </p>
        )}
      </main>

      <div className="hidden lg:block">
        <div className="sticky top-20">
          <PanelDerecho rol={rol} version={version} />
        </div>
      </div>

      {explorando && <ExplorarCanales onCerrar={cerrarExplorar} onCambio={huboCambio} />}
    </div>
  )
}

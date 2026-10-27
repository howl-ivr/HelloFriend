import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, Vacio, claseBoton, claseBotonSecundario, claseInput } from '../components/ui'

const hoy = new Date()
const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export default function AdminCanales() {
  const [anio, setAnio] = useState(String(hoy.getFullYear()))
  const [semestre, setSemestre] = useState(hoy.getMonth() < 6 ? '1' : '2')
  const [seleccion, setSeleccion] = useState([])
  const [mostrarNuevo, setMostrarNuevo] = useState(false)
  const [nuevo, setNuevo] = useState({ codigo: '', nombre: '', ciclo: '' })
  const [busqueda, setBusqueda] = useState('')
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [filtro, setFiltro] = useState('')
  const [periodos, setPeriodos] = useState([])
  const [version, setVersion] = useState(0)
  const [cursosRes, setCursosRes] = useState({ clave: null, lista: [] })
  const [canalesRes, setCanalesRes] = useState({ clave: null, lista: [], error: '' })
  const [confirmando, setConfirmando] = useState(null)

  const periodo = `${anio}-${semestre}`
  const periodoValido = /^\d{4}$/.test(anio)
  const claveCursos = `${periodo}|${version}`
  const claveCanales = `${filtro}|${version}`

  useEffect(() => {
    api.get('/periodos').then(r => setPeriodos(r.data)).catch(() => setPeriodos([]))
  }, [version])

  useEffect(() => {
    if (!periodoValido) return
    let vigente = true
    const k = `${periodo}|${version}`
    api.get('/admin/cursos', { params: { periodo } })
      .then(r => vigente && setCursosRes({ clave: k, lista: r.data }))
      .catch(e => { if (vigente) { setCursosRes({ clave: k, lista: [] }); setMensaje({ tipo: 'error', texto: mensajeError(e) }) } })
    return () => { vigente = false }
  }, [periodo, periodoValido, version])

  useEffect(() => {
    let vigente = true
    const k = `${filtro}|${version}`
    api.get('/admin/canales', { params: { periodo: filtro || undefined } })
      .then(r => vigente && setCanalesRes({ clave: k, lista: r.data, error: '' }))
      .catch(e => vigente && setCanalesRes({ clave: k, lista: [], error: mensajeError(e) }))
    return () => { vigente = false }
  }, [filtro, version])

  const cursos = cursosRes.lista
  const consulta = normalizar(busqueda.trim())
  const cursosVisibles = cursos.filter(c => normalizar(`${c.codigo} ${c.nombre}`).includes(consulta))
  const ciclos = [...new Set(cursosVisibles.map(c => c.ciclo).filter(c => Number.isInteger(c)))].sort((a, b) => a - b)
  const grupos = ciclos.map(ciclo => ({ etiqueta: `Ciclo ${ciclo}`, cursos: cursosVisibles.filter(c => c.ciclo === ciclo) }))
  const electivos = cursosVisibles.filter(c => !Number.isInteger(c.ciclo))
  if (electivos.length) grupos.push({ etiqueta: 'Electivos', cursos: electivos })
  const elegidos = seleccion.filter(id => cursos.some(c => c.id === id && !c.tieneCanal))
  const cargandoCursos = periodoValido && cursosRes.clave !== claveCursos

  const alternar = id => setSeleccion(s => (s.includes(id) ? s.filter(x => x !== id) : [...s, id]))

  const registrarCurso = async e => {
    e.preventDefault()
    setMensaje(null)
    setEnviando(true)
    try {
      const { data } = await api.post('/admin/cursos', { ...nuevo, ciclo: nuevo.ciclo === 'electivo' ? null : Number(nuevo.ciclo) })
      setSeleccion(s => [...s, data.id])
      setNuevo({ codigo: '', nombre: '', ciclo: '' })
      setMostrarNuevo(false)
      setMensaje({ tipo: 'exito', texto: `Curso ${data.codigo} registrado y seleccionado` })
      setVersion(v => v + 1)
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) })
    } finally {
      setEnviando(false)
    }
  }

  const crear = async e => {
    e.preventDefault()
    setMensaje(null)
    setEnviando(true)
    try {
      const { data } = await api.post('/admin/canales', { periodo, cursoIds: elegidos })
      setMensaje({ tipo: 'exito', texto: `Se ${data.length === 1 ? 'creó 1 canal' : `crearon ${data.length} canales`} para ${periodo}` })
      setSeleccion([])
      setFiltro(periodo)
      setVersion(v => v + 1)
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) })
    } finally {
      setEnviando(false)
    }
  }

  const desactivar = async canal => {
    setEnviando(true)
    setMensaje(null)
    try {
      await api.post(`/admin/canales/${canal.id}/desactivar`)
      setConfirmando(null)
      setVersion(v => v + 1)
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) })
    } finally {
      setEnviando(false)
    }
  }

  const cargandoCanales = canalesRes.clave !== claveCanales

  return (
    <div>
      <Titulo subtitulo="Cada canal vincula un curso con un periodo académico. Elige el periodo, selecciona los cursos y crea sus canales; si un curso aún no existe, regístralo aquí mismo.">
        Canales
      </Titulo>

      <form onSubmit={crear} className="bg-white border border-linea rounded-2xl p-5 mb-6 space-y-4">
        <h2 className="font-semibold text-tinta">Crear canales por periodo</h2>

        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="text-sm font-medium text-tinta" htmlFor="anio">Año</label>
            <input id="anio" type="number" min="2000" max="2100" value={anio} onChange={e => setAnio(e.target.value)} className={`${claseInput} mt-1 w-28`} />
          </div>
          <div>
            <label className="text-sm font-medium text-tinta" htmlFor="semestre">Semestre</label>
            <select id="semestre" value={semestre} onChange={e => setSemestre(e.target.value)} className={`${claseInput} mt-1 w-28`}>
              <option value="1">1</option>
              <option value="2">2</option>
            </select>
          </div>
          <p className="pb-2 text-sm text-suave">Periodo: <span className="font-mono font-semibold text-tinta">{periodoValido ? periodo : '—'}</span></p>
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium text-tinta">Cursos</p>
            <button type="button" onClick={() => setMostrarNuevo(m => !m)} className="text-sm font-semibold text-marca hover:underline">
              {mostrarNuevo ? 'Cancelar' : '+ Registrar un curso nuevo'}
            </button>
          </div>

          <div className="mb-3">
            <label className="text-sm font-medium text-tinta" htmlFor="buscar-curso">Buscar curso por nombre o código</label>
            <input id="buscar-curso" type="search" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Ej. 1INF36 o Ética" className={`${claseInput} mt-1`} />
          </div>

          {mostrarNuevo && (
            <div className="rounded-xl border border-linea bg-fondo p-3 mb-3 grid gap-2 sm:grid-cols-[140px_1fr_160px_auto]">
              <input value={nuevo.codigo} onChange={e => setNuevo(n => ({ ...n, codigo: e.target.value }))} maxLength={10} placeholder="Código (ej. INF290)" aria-label="Código del curso" className={claseInput} />
              <input value={nuevo.nombre} onChange={e => setNuevo(n => ({ ...n, nombre: e.target.value }))} maxLength={150} placeholder="Nombre del curso" aria-label="Nombre del curso" className={claseInput} />
              <select value={nuevo.ciclo} onChange={e => setNuevo(n => ({ ...n, ciclo: e.target.value }))} aria-label="Ciclo académico del curso" className={claseInput}>
                <option value="" disabled>Selecciona un ciclo</option>
                {Array.from({ length: 10 }, (_, i) => i + 1).map(ciclo => <option key={ciclo} value={ciclo}>Ciclo {ciclo}</option>)}
                <option value="electivo">Electivo</option>
              </select>
              <button type="button" onClick={registrarCurso} disabled={enviando || !nuevo.codigo.trim() || !nuevo.nombre.trim() || !nuevo.ciclo} className={claseBoton}>Registrar curso</button>
            </div>
          )}

          {!periodoValido ? (
            <p className="text-sm text-suave">Escribe un año de 4 dígitos para ver los cursos.</p>
          ) : cargandoCursos ? (
            <Cargando />
          ) : cursos.length === 0 ? (
            <p className="text-sm text-suave">Aún no hay cursos registrados. Registra el primero con el botón de arriba.</p>
          ) : cursosVisibles.length === 0 ? (
            <p className="text-sm text-suave">Ningún curso coincide con “{busqueda}”.</p>
          ) : (
            <div className="space-y-4">
              {grupos.map(grupo => (
                <section key={grupo.etiqueta} aria-label={grupo.etiqueta}>
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="text-sm font-semibold text-tinta">{grupo.etiqueta}</h3>
                    <span className="text-xs text-suave bg-fondo border border-linea rounded-full px-2 py-0.5">{grupo.cursos.length}</span>
                  </div>
                  <ul className="grid gap-2 sm:grid-cols-2">
                    {grupo.cursos.map(c => (
                      <li key={c.id}>
                        <label className={`flex items-center gap-3 rounded-xl border px-3 py-2 text-sm ${c.tieneCanal ? 'border-linea bg-fondo text-suave' : 'border-linea bg-white hover:border-marca/40 cursor-pointer'}`}>
                          <input type="checkbox" checked={elegidos.includes(c.id)} disabled={c.tieneCanal} onChange={() => alternar(c.id)} className="h-4 w-4 accent-[var(--color-marca)]" />
                          <span className="flex-1 min-w-0 truncate"><span className="font-semibold">{c.codigo}</span> · {c.nombre}</span>
                          {!Number.isInteger(c.ciclo) && <span className="text-xs whitespace-nowrap rounded-full border border-linea bg-fondo px-2 py-0.5">Electivo</span>}
                          {c.tieneCanal && <span className="text-xs">ya tiene canal</span>}
                        </label>
                      </li>
                    ))}
                  </ul>
                </section>
              ))}
            </div>
          )}
        </div>

        <button disabled={elegidos.length === 0 || enviando || !periodoValido} className={claseBoton}>
          {elegidos.length === 0 ? 'Crear canales' : `Crear ${elegidos.length === 1 ? '1 canal' : `${elegidos.length} canales`} para ${periodo}`}
        </button>
        {mensaje && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      </form>

      <div className="flex items-center justify-between gap-3 mb-3">
        <h2 className="font-semibold text-tinta">Canales existentes</h2>
        <select value={filtro} onChange={e => setFiltro(e.target.value)} aria-label="Filtrar por periodo" className={`${claseInput} w-auto`}>
          <option value="">Todos los periodos</option>
          {periodos.map(p => <option key={p} value={p}>{p}</option>)}
        </select>
      </div>

      {cargandoCanales ? (
        <Cargando />
      ) : canalesRes.error ? (
        <Alerta>{canalesRes.error}</Alerta>
      ) : canalesRes.lista.length === 0 ? (
        <Vacio>No hay canales en este periodo.</Vacio>
      ) : (
        <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-fondo text-left text-suave">
              <tr>
                <th className="px-4 py-2 font-medium">Curso</th>
                <th className="px-4 py-2 font-medium">Periodo</th>
                <th className="px-4 py-2 font-medium">Estado</th>
                <th className="px-4 py-2 font-medium text-right">Suscritos</th>
                <th className="px-4 py-2 font-medium text-right">Publicadas</th>
                <th className="px-4 py-2 font-medium text-right">Pendientes</th>
                <th className="px-4 py-2 font-medium text-right">Acción</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-linea">
              {canalesRes.lista.map(c => (
                <tr key={c.id}>
                  <td className="px-4 py-2 font-medium text-tinta">{c.nombre}</td>
                  <td className="px-4 py-2 font-mono text-xs">{c.periodo}</td>
                  <td className="px-4 py-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full whitespace-nowrap ${c.activo ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-600'}`}>
                      {c.activo ? 'Activo' : 'Desactivado'}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-right">{c.totalSuscritos}</td>
                  <td className="px-4 py-2 text-right">{c.totalPublicadas}</td>
                  <td className="px-4 py-2 text-right">{c.totalPendientes}</td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    {!c.activo ? (
                      <span className="text-suave">—</span>
                    ) : confirmando === c.id ? (
                      <span className="inline-flex gap-2">
                        <button onClick={() => desactivar(c)} disabled={enviando} className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-full px-4 py-2 text-sm font-medium">
                          Sí, desactivar
                        </button>
                        <button onClick={() => setConfirmando(null)} className={claseBotonSecundario}>Cancelar</button>
                      </span>
                    ) : (
                      <button onClick={() => setConfirmando(c.id)} className={claseBotonSecundario}>Desactivar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

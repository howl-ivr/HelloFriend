import { useEffect, useState } from 'react'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, claseBoton, claseInput } from '../components/ui'

export default function AdminCanales() {
  const [canales, setCanales] = useState(null)
  const [cursos, setCursos] = useState([])
  const [cursoId, setCursoId] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [mensaje, setMensaje] = useState(null)

  const cargar = () =>
    Promise.all([api.get('/admin/canales'), api.get('/admin/cursos')]).then(([k, c]) => {
      setCanales(k.data)
      setCursos(c.data)
    })

  useEffect(() => {
    cargar()
  }, [])

  const crear = async e => {
    e.preventDefault()
    setMensaje(null)
    try {
      const { data } = await api.post('/admin/canales', { cursoId: Number(cursoId), descripcion })
      setMensaje({ tipo: 'exito', texto: `Canal "${data.nombre}" creado` })
      setCursoId('')
      setDescripcion('')
      cargar()
    } catch (err) {
      setMensaje({ tipo: 'error', texto: mensajeError(err) })
    }
  }

  if (!canales) return <Cargando />
  const disponibles = cursos.filter(c => !c.tieneCanal)

  return (
    <div>
      <Titulo subtitulo="Cada curso vigente tiene un único canal.">Canales</Titulo>

      <form onSubmit={crear} className="bg-white border border-linea rounded-2xl p-5 mb-6 space-y-3">
        <h2 className="font-semibold text-tinta">Crear canal</h2>
        {disponibles.length === 0 ? (
          <p className="text-sm text-suave">Todos los cursos vigentes ya tienen canal.</p>
        ) : (
          <>
            <select value={cursoId} onChange={e => setCursoId(e.target.value)} required className={claseInput}>
              <option value="">Elige un curso vigente...</option>
              {disponibles.map(c => <option key={c.id} value={c.id}>{c.codigo} · {c.nombre}</option>)}
            </select>
            <input
              value={descripcion}
              onChange={e => setDescripcion(e.target.value)}
              maxLength={200}
              placeholder="Descripción (opcional)"
              className={claseInput}
            />
            <button disabled={!cursoId} className={claseBoton}>Crear canal</button>
          </>
        )}
        {mensaje && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      </form>

      <div className="bg-white border border-linea rounded-2xl overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-fondo text-left text-suave">
            <tr>
              <th className="px-4 py-2 font-medium">Canal</th>
              <th className="px-4 py-2 font-medium text-right">Suscritos</th>
              <th className="px-4 py-2 font-medium text-right">Publicadas</th>
              <th className="px-4 py-2 font-medium text-right">Pendientes</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-linea">
            {canales.map(c => (
              <tr key={c.id}>
                <td className="px-4 py-2">
                  <p className="font-medium text-tinta">{c.nombre}</p>
                  <p className="text-xs text-suave">{c.descripcion}</p>
                </td>
                <td className="px-4 py-2 text-right">{c.totalSuscritos}</td>
                <td className="px-4 py-2 text-right">{c.totalPublicadas}</td>
                <td className="px-4 py-2 text-right">{c.totalPendientes}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

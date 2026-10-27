import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, claseBoton, claseBotonSecundario, claseInput } from '../components/ui'
import { fechaHora } from '../utils/formato'

export default function EditarIncidencia() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [datos, setDatos] = useState(null)
  const [form, setForm] = useState(null)
  const [errorCarga, setErrorCarga] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vigente = true
    Promise.all([api.get(`/incidencias/${id}`), api.get('/categorias')])
      .then(([i, k]) => {
        if (!vigente) return
        setDatos({ inc: i.data, categorias: k.data })
        setForm({ titulo: i.data.titulo, descripcion: i.data.descripcion, categoria: i.data.categoria })
      })
      .catch(e => vigente && setErrorCarga(mensajeError(e)))
    return () => { vigente = false }
  }, [id])

  if (errorCarga) return <Alerta>{errorCarga}</Alerta>
  if (!datos || !form) return <Cargando />

  const { inc, categorias } = datos

  if (!inc.puedeEditar) {
    return (
      <div className="max-w-2xl mx-auto space-y-4">
        <Alerta tipo="info">Esta incidencia ya no se puede editar: solo es posible mientras está pendiente u observada.</Alerta>
        <Link to={`/incidencias/${inc.id}`} className="text-sm text-suave hover:underline">← Volver al detalle</Link>
      </div>
    )
  }

  const observada = inc.estado === 'OBSERVADA'
  const cambio = form.titulo.trim() !== inc.titulo || form.descripcion.trim() !== inc.descripcion || form.categoria !== inc.categoria
  const valido = form.titulo.trim().length >= 5 && form.descripcion.trim().length >= 20 && form.categoria
  const campo = nombre => e => setForm(f => ({ ...f, [nombre]: e.target.value }))

  const guardar = async reenviar => {
    setError('')
    setEnviando(true)
    try {
      let actual = inc
      if (cambio) {
        const r = await api.put(`/incidencias/${inc.id}`, { ...form, titulo: form.titulo.trim(), descripcion: form.descripcion.trim() })
        actual = r.data
        setDatos(d => ({ ...d, inc: r.data }))
      }
      if (reenviar) await api.post(`/incidencias/${actual.id}/reenviar`)
      navigate(`/incidencias/${actual.id}`)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <Link to={`/incidencias/${inc.id}`} className="text-sm text-suave hover:underline">← Volver al detalle</Link>
      <div className="mt-3">
        <Titulo subtitulo={observada ? 'Atiende las observaciones de la dirección y vuelve a enviar tu reporte.' : 'Puedes cambiar tu reporte mientras la dirección no lo apruebe o rechace.'}>
          Editar incidencia
        </Titulo>
      </div>

      {observada && inc.observaciones?.length > 0 && (
        <div className="rounded-2xl border border-orange-200 bg-orange-50 p-4 mb-5">
          <p className="text-sm font-semibold text-orange-800">La dirección pidió cambios</p>
          <ul className="mt-2 space-y-2">
            {inc.observaciones.map(o => (
              <li key={o.fecha} className="text-sm text-tinta">
                {o.texto}
                <span className="block text-xs text-suave">Versión {o.version} · {fechaHora(o.fecha)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={e => { e.preventDefault(); guardar(observada) }} className="bg-white border border-linea rounded-2xl p-6 space-y-4">
        <div>
          <label className="text-sm font-medium text-tinta">Curso</label>
          <p className="mt-1 text-sm text-tinta">{inc.canal.nombre} · {inc.canal.periodo}</p>
          <p className="text-xs text-suave mt-1">El canal no se puede cambiar.</p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Categoría</label>
          <select value={form.categoria} onChange={campo('categoria')} required className={`${claseInput} mt-1`}>
            {categorias.map(k => <option key={k} value={k}>{k}</option>)}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Título</label>
          <input value={form.titulo} onChange={campo('titulo')} maxLength={100} required className={`${claseInput} mt-1`} />
          <p className="text-xs text-suave mt-1">{form.titulo.length}/100</p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Descripción</label>
          <textarea value={form.descripcion} onChange={campo('descripcion')} maxLength={1000} rows={6} required className={`${claseInput} mt-1`} />
          <p className="text-xs text-suave mt-1">{form.descripcion.length}/1000 · mínimo 20 caracteres · no incluyas nombres propios</p>
        </div>
        {error && <Alerta>{error}</Alerta>}
        <div className="flex flex-wrap gap-2">
          {observada ? (
            <>
              <button type="submit" disabled={!valido || enviando || (!cambio && !inc.puedeReenviar)} className={claseBoton}>
                {enviando ? 'Enviando...' : 'Guardar y reenviar a revisión'}
              </button>
              <button type="button" onClick={() => guardar(false)} disabled={!valido || !cambio || enviando} className={claseBotonSecundario}>
                Guardar sin reenviar
              </button>
            </>
          ) : (
            <button type="submit" disabled={!valido || !cambio || enviando} className={claseBoton}>
              {enviando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          )}
        </div>
      </form>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { mensajeError } from '../api/client'
import { Alerta, Cargando, Titulo, Vacio, claseBoton, claseBotonSecundario, claseInput } from '../components/ui'

export default function NuevaIncidencia() {
  const [params] = useSearchParams()
  const [canales, setCanales] = useState(null)
  const [canalId, setCanalId] = useState(params.get('canal') ?? '')
  const [titulo, setTitulo] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [enviada, setEnviada] = useState(false)

  useEffect(() => {
    api.get('/canales').then(r => setCanales(r.data))
  }, [])

  const enviar = async e => {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await api.post('/incidencias', { canalId: Number(canalId), titulo: titulo.trim(), descripcion: descripcion.trim() })
      setEnviada(true)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(false)
    }
  }

  if (!canales) return <Cargando />

  if (enviada) {
    return (
      <div className="max-w-lg mx-auto bg-white border border-linea rounded-2xl p-6 text-center">
        <p className="text-4xl mb-3">✅</p>
        <h1 className="text-lg font-bold mb-2">Incidencia enviada</h1>
        <p className="text-sm text-suave mb-5">
          La dirección de carrera la revisará antes de publicarla. Te avisaremos por notificación si se aprueba o se rechaza.
        </p>
        <div className="flex justify-center gap-2">
          <Link to="/inicio" className={claseBotonSecundario}>Volver al inicio</Link>
          <Link to="/mis-incidencias" className={claseBoton}>Ver mis reportes</Link>
        </div>
      </div>
    )
  }

  if (canales.length === 0) {
    return <Vacio>Aún no hay canales creados.</Vacio>
  }

  const valido = canalId && titulo.trim().length >= 5 && descripcion.trim().length >= 20

  return (
    <div className="max-w-2xl mx-auto">
      <Titulo>Reportar incidencia</Titulo>

      <div className="bg-confirma-suave border border-confirma/30 rounded-2xl p-4 mb-5 text-sm text-confirma">
        🔒 <span className="font-semibold">Tu identidad no será visible para nadie</span>: ni para otros alumnos, ni para profesores,
        ni para la dirección. Describe el hecho sin mencionar nombres de personas.
      </div>

      <form onSubmit={enviar} className="bg-white border border-linea rounded-2xl p-6 space-y-4">
        <div>
          <label className="text-sm font-medium text-tinta">Curso</label>
          <select value={canalId} onChange={e => setCanalId(e.target.value)} required className={`${claseInput} mt-1`}>
            <option value="">Elige el curso...</option>
            {canales.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
          <p className="text-xs text-suave mt-1">Al reportar quedas suscrito al canal para recibir novedades.</p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Título</label>
          <input
            value={titulo}
            onChange={e => setTitulo(e.target.value)}
            maxLength={100}
            placeholder="Ej.: Dos evaluaciones programadas el mismo día"
            required
            className={`${claseInput} mt-1`}
          />
          <p className="text-xs text-suave mt-1">{titulo.length}/100</p>
        </div>
        <div>
          <label className="text-sm font-medium text-tinta">Descripción</label>
          <textarea
            value={descripcion}
            onChange={e => setDescripcion(e.target.value)}
            maxLength={1000}
            rows={6}
            placeholder="¿Qué pasó? ¿Cuándo? ¿Cómo afecta a los alumnos?"
            required
            className={`${claseInput} mt-1`}
          />
          <p className="text-xs text-suave mt-1">{descripcion.length}/1000 · mínimo 20 caracteres</p>
        </div>
        {error && <Alerta>{error}</Alerta>}
        <button disabled={!valido || enviando} className={claseBoton}>
          {enviando ? 'Enviando...' : 'Enviar a revisión'}
        </button>
      </form>
    </div>
  )
}

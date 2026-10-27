export function hace(fecha) {
  const min = Math.max(0, Math.floor((Date.now() - new Date(fecha)) / 60000))
  if (min < 1) return 'hace un momento'
  if (min < 60) return `hace ${min} min`
  const h = Math.floor(min / 60)
  if (h < 24) return `hace ${h} h`
  const d = Math.floor(h / 24)
  if (d < 30) return `hace ${d} d`
  return new Date(fecha).toLocaleDateString('es-PE')
}

export function fechaHora(fecha) {
  return new Date(fecha).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' })
}

export const ESTADOS = {
  PENDIENTE: { etiqueta: 'Pendiente de revisión', clase: 'bg-amber-100 text-amber-800' },
  OBSERVADA: { etiqueta: 'Observada', clase: 'bg-orange-100 text-orange-800' },
  PUBLICADA: { etiqueta: 'Aprobada', clase: 'bg-blue-100 text-blue-800' },
  RECHAZADA: { etiqueta: 'Rechazada', clase: 'bg-red-100 text-red-800' },
  EN_ATENCION: { etiqueta: 'En atención', clase: 'bg-indigo-100 text-indigo-800' },
  RESUELTA: { etiqueta: 'Resuelta', clase: 'bg-green-100 text-green-800' }
}

export const EVENTOS = {
  CREADA: 'Enviada a revisión',
  EDITADA: 'Editada por el estudiante',
  OBSERVADA: 'Observada por la dirección',
  REENVIADA: 'Reenviada a revisión',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  EN_ATENCION: 'Pasó a en atención',
  RESUELTA: 'Resuelta',
  CONFIRMADA: 'Confirmada por un estudiante',
  CONFIRMACION_RETIRADA: 'Confirmación retirada por un estudiante'
}

export const PRIORIDADES = {
  ALTA: { etiqueta: 'Prioridad alta', clase: 'bg-red-100 text-red-800' },
  MEDIA: { etiqueta: 'Prioridad media', clase: 'bg-amber-100 text-amber-800' },
  BAJA: { etiqueta: 'Prioridad baja', clase: 'bg-slate-100 text-slate-600' }
}

export const REACCIONES = [
  { tipo: 'APOYO', icono: '🤝', etiqueta: 'Apoyo' },
  { tipo: 'PREOCUPANTE', icono: '😟', etiqueta: 'Preocupante' },
  { tipo: 'URGENTE', icono: '🔥', etiqueta: 'Urgente' },
  { tipo: 'LO_SIGO', icono: '👀', etiqueta: 'Lo sigo' }
]

export const ROLES = {
  ESTUDIANTE: 'Estudiante',
  PROFESOR: 'Profesor',
  DIRECTOR: 'Director de carrera',
  ADMINISTRADOR: 'Administrador'
}

const TONOS = [
  'bg-[#dbe6ff] text-[#2348c4]',
  'bg-[#dff3ea] text-[#0f6e4b]',
  'bg-[#fde8d9] text-[#a14a14]',
  'bg-[#efe3fb] text-[#6b2fb0]',
  'bg-[#fbe0e7] text-[#a3254b]',
  'bg-[#e1f1f7] text-[#14637d]'
]

export function tonoCanal(id) {
  const valor = String(id ?? '').split('').reduce((n, c) => n + c.charCodeAt(0), 0)
  return TONOS[valor % TONOS.length]
}

// "INF263 · Algoritmia" → { codigo: "INF263", nombre: "Algoritmia" }
export function partesCanal(nombreCompleto = '') {
  const [codigo, ...resto] = nombreCompleto.split(' · ')
  return { codigo, nombre: resto.join(' · ') || codigo }
}

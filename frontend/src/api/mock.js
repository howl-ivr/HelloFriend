// Backend simulado en el navegador. Se activa con VITE_MOCK=true (.env.development).
// Implementa el mismo contrato que docs/API.md, así el frontend no cambia al conectar Spring Boot.
// Los datos se guardan en localStorage; para reiniciarlos: localStorage.removeItem('hf_mock_db_v2')
import { AxiosError } from 'axios'

const CLAVE = 'hf_mock_db_v2'
const ahora = () => new Date().toISOString()
const haceHoras = h => new Date(Date.now() - h * 3600000).toISOString()

function semilla() {
  return {
    cursos: [
      { id: 1, codigo: 'INF263', nombre: 'Algoritmia' },
      { id: 2, codigo: 'INF281', nombre: 'Bases de Datos' },
      { id: 3, codigo: 'INF246', nombre: 'Ingeniería de Software' },
      { id: 4, codigo: 'INF226', nombre: 'Redes de Computadoras' },
      { id: 5, codigo: 'INF285', nombre: 'Sistemas Operativos' }
    ],
    canales: [
      { id: 1, cursoId: 1, nombre: 'INF263 · Algoritmia', descripcion: 'Incidencias del curso de Algoritmia', fechaCreacion: haceHoras(400) },
      { id: 2, cursoId: 2, nombre: 'INF281 · Bases de Datos', descripcion: 'Incidencias del curso de Bases de Datos', fechaCreacion: haceHoras(400) },
      { id: 3, cursoId: 3, nombre: 'INF246 · Ingeniería de Software', descripcion: 'Incidencias del curso de Ingeniería de Software', fechaCreacion: haceHoras(300) }
    ],
    usuarios: [
      { id: 1, usuario: 'admin', correo: 'admin@pucp.edu.pe', password: 'admin123', rol: 'ADMIN', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 2, usuario: 'director', correo: 'director.inf@pucp.edu.pe', password: 'director123', rol: 'DIRECTOR', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 3, usuario: 'profesor', correo: 'profesor.inf@pucp.edu.pe', password: 'profesor123', rol: 'PROFESOR', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 4, usuario: 'alumno1', correo: 'alumno1@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(500) },
      { id: 5, usuario: 'alumno2', correo: 'alumno2@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(480) },
      { id: 6, usuario: 'alumno3', correo: 'alumno3@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(470) },
      { id: 7, usuario: 'nuevo', correo: 'nuevo.alumno@pucp.edu.pe', password: 'nuevo123', rol: 'ESTUDIANTE', estado: 'PENDIENTE', fechaRegistro: haceHoras(3) },
      { id: 8, usuario: 'cachimbo26', correo: 'cachimbo26@pucp.edu.pe', password: 'nuevo123', rol: 'ESTUDIANTE', estado: 'PENDIENTE', fechaRegistro: haceHoras(1) }
    ],
    suscripciones: [
      { usuarioId: 3, canalId: 1 }, { usuarioId: 3, canalId: 2 },
      { usuarioId: 4, canalId: 1 }, { usuarioId: 4, canalId: 2 },
      { usuarioId: 5, canalId: 1 }, { usuarioId: 5, canalId: 3 },
      { usuarioId: 6, canalId: 2 }
    ],
    incidencias: [
      {
        id: 1, canalId: 1, autorId: 5, estado: 'PUBLICADA', fechaCreacion: haceHoras(50),
        titulo: 'Dos evaluaciones programadas el mismo día',
        descripcion: 'La práctica calificada y la entrega del laboratorio quedaron para el mismo jueves. Varios compañeros del horario no llegamos a prepararnos bien para ambas.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(45), directorId: 2 }
      },
      {
        id: 2, canalId: 1, autorId: 4, estado: 'PUBLICADA', fechaCreacion: haceHoras(20),
        titulo: 'Las notas del laboratorio no se publican',
        descripcion: 'Ya pasaron tres semanas desde el laboratorio 2 y aún no tenemos la nota ni retroalimentación, lo que complica prepararse para el siguiente.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(18), directorId: 2 }
      },
      {
        id: 3, canalId: 2, autorId: 6, estado: 'RESUELTA', fechaCreacion: haceHoras(200),
        titulo: 'El aula asignada no tiene proyector',
        descripcion: 'En las sesiones de los martes no funciona el proyector y la clase se dicta sin diapositivas.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(190), directorId: 2 }
      },
      {
        id: 4, canalId: 2, autorId: 4, estado: 'PENDIENTE', fechaCreacion: haceHoras(2),
        titulo: 'Enunciado del proyecto ambiguo',
        descripcion: 'El enunciado del proyecto final tiene requisitos contradictorios y no hay un canal para hacer consultas antes de la entrega.',
        revision: null
      },
      {
        id: 5, canalId: 1, autorId: 4, estado: 'RECHAZADA', fechaCreacion: haceHoras(100),
        titulo: 'Queja general del curso',
        descripcion: 'No me gusta el curso.',
        revision: { decision: 'RECHAZADA', motivoId: 2, comentario: 'Describe una situación concreta (qué pasó y cuándo) para poder evaluarla.', fecha: haceHoras(95), directorId: 2 }
      }
    ],
    confirmaciones: [
      { usuarioId: 4, incidenciaId: 1 },
      { usuarioId: 5, incidenciaId: 2 },
      { usuarioId: 4, incidenciaId: 3 }
    ],
    reacciones: [
      { usuarioId: 4, incidenciaId: 1, tipo: 'URGENTE' },
      { usuarioId: 6, incidenciaId: 1, tipo: 'APOYO' },
      { usuarioId: 5, incidenciaId: 2, tipo: 'PREOCUPANTE' }
    ],
    predefinidos: [
      { id: 1, texto: 'Me pasó lo mismo' },
      { id: 2, texto: 'Confirmo, estoy en el mismo horario' },
      { id: 3, texto: 'Sucedió otra vez esta semana' },
      { id: 4, texto: 'Ya lo comenté con el delegado' },
      { id: 5, texto: 'Se solucionó en mi caso' }
    ],
    comentarios: [
      { id: 1, incidenciaId: 1, usuarioId: 4, predefinidoId: 2, fecha: haceHoras(40) },
      { id: 2, incidenciaId: 2, usuarioId: 5, predefinidoId: 1, fecha: haceHoras(10) }
    ],
    motivos: [
      { id: 1, descripcion: 'Contiene ataques personales o lenguaje ofensivo' },
      { id: 2, descripcion: 'Falta información para evaluarla' },
      { id: 3, descripcion: 'No corresponde a una incidencia del curso' },
      { id: 4, descripcion: 'Duplicada: ya existe una incidencia similar' },
      { id: 5, descripcion: 'Otro motivo (ver comentario)' }
    ],
    notificaciones: [
      { id: 1, usuarioId: 4, incidenciaId: 5, mensaje: 'Tu incidencia "Queja general del curso" fue rechazada: Falta información para evaluarla.', leida: false, fecha: haceHoras(95) },
      { id: 2, usuarioId: 2, incidenciaId: 4, mensaje: 'Nueva incidencia pendiente en INF281 · Bases de Datos', leida: false, fecha: haceHoras(2) }
    ],
    auditoria: [
      { id: 1, accion: 'INCIDENCIA_APROBADA', actor: 'director', incidenciaId: 1, detalle: 'Publicada en INF263 · Algoritmia', fecha: haceHoras(45) },
      { id: 2, accion: 'INCIDENCIA_REGISTRADA', actor: 'Estudiante (anónimo)', incidenciaId: 4, detalle: 'Canal INF281 · Bases de Datos', fecha: haceHoras(2) }
    ]
  }
}

function cargar() {
  try {
    const raw = localStorage.getItem(CLAVE)
    if (raw) return JSON.parse(raw)
  } catch { /* datos corruptos: se regeneran */ }
  const db = semilla()
  guardar(db)
  return db
}

function guardar(db) {
  try { localStorage.setItem(CLAVE, JSON.stringify(db)) } catch { /* sin almacenamiento */ }
}

class HttpError extends Error {
  constructor(status, mensaje) {
    super(mensaje)
    this.status = status
  }
}
const error = (status, mensaje) => { throw new HttpError(status, mensaje) }
const siguienteId = lista => lista.reduce((max, x) => Math.max(max, x.id), 0) + 1

function crearToken(u) {
  const b64 = obj => btoa(JSON.stringify(obj)).replace(/=+$/, '')
  return `${b64({ alg: 'none' })}.${b64({ sub: u.id, usuario: u.usuario, rol: u.rol })}.mock`
}

function usuarioDelToken(db, config) {
  const h = config.headers?.Authorization ?? config.headers?.get?.('Authorization')
  if (!h) return null
  try {
    const payload = JSON.parse(atob(h.replace('Bearer ', '').split('.')[1]))
    return db.usuarios.find(u => u.id === payload.sub && u.estado === 'ACTIVA') ?? null
  } catch {
    return null
  }
}

function requerir(u, ...roles) {
  if (!u) error(401, 'Sesión no válida')
  if (roles.length && !roles.includes(u.rol)) error(403, 'No tienes permiso para esta acción')
}

// ---------- Reglas de negocio ----------
const estaSuscrito = (db, u, canalId) => db.suscripciones.some(s => s.usuarioId === u.id && s.canalId === canalId)
const canalDe = (db, id) => db.canales.find(c => c.id === id) ?? error(404, 'Canal no encontrado')
const incidenciaDe = (db, id) => db.incidencias.find(i => i.id === id) ?? error(404, 'Incidencia no encontrada')
const esVisible = estado => estado === 'PUBLICADA' || estado === 'RESUELTA'

function auditar(db, accion, actor, incidenciaId, detalle) {
  db.auditoria.push({ id: siguienteId(db.auditoria), accion, actor, incidenciaId, detalle, fecha: ahora() })
}

function notificar(db, usuarioIds, incidenciaId, mensaje) {
  for (const usuarioId of new Set(usuarioIds)) {
    db.notificaciones.push({ id: siguienteId(db.notificaciones), usuarioId, incidenciaId, mensaje, leida: false, fecha: ahora() })
  }
}

const suscritosDe = (db, canalId) => db.suscripciones.filter(s => s.canalId === canalId).map(s => s.usuarioId)

function conteoReacciones(db, incidenciaId) {
  const conteo = { APOYO: 0, PREOCUPANTE: 0, URGENTE: 0, LO_SIGO: 0 }
  db.reacciones.filter(r => r.incidenciaId === incidenciaId).forEach(r => { conteo[r.tipo]++ })
  return conteo
}

const confirmacionesDe = (db, id) => db.confirmaciones.filter(c => c.incidenciaId === id).length

// ---------- Serializadores (nunca exponen al autor) ----------
function serCanal(db, u, c) {
  const curso = db.cursos.find(x => x.id === c.cursoId)
  return {
    id: c.id,
    nombre: c.nombre,
    descripcion: c.descripcion,
    curso: { id: curso.id, codigo: curso.codigo, nombre: curso.nombre },
    suscrito: estaSuscrito(db, u, c.id),
    totalSuscritos: suscritosDe(db, c.id).length,
    totalPublicadas: db.incidencias.filter(i => i.canalId === c.id && esVisible(i.estado)).length
  }
}

function serRevision(db, r) {
  if (!r) return null
  return {
    decision: r.decision,
    motivo: db.motivos.find(m => m.id === r.motivoId)?.descripcion ?? null,
    comentario: r.comentario,
    fecha: r.fecha
  }
}

function serIncidencia(db, u, i, { detalle = false } = {}) {
  const canal = db.canales.find(c => c.id === i.canalId)
  const esMia = i.autorId === u.id
  const publicada = i.estado === 'PUBLICADA'
  const suscrito = estaSuscrito(db, u, canal.id)
  const estudiante = u.rol === 'ESTUDIANTE'
  const base = {
    id: i.id,
    titulo: i.titulo,
    descripcion: i.descripcion,
    estado: i.estado,
    fechaCreacion: i.fechaCreacion,
    canal: { id: canal.id, nombre: canal.nombre },
    confirmaciones: confirmacionesDe(db, i.id),
    confirmadaPorMi: db.confirmaciones.some(c => c.incidenciaId === i.id && c.usuarioId === u.id),
    reacciones: conteoReacciones(db, i.id),
    miReaccion: db.reacciones.find(r => r.incidenciaId === i.id && r.usuarioId === u.id)?.tipo ?? null,
    totalComentarios: db.comentarios.filter(c => c.incidenciaId === i.id).length,
    esMia,
    puedeConfirmar: publicada && estudiante && suscrito && !esMia,
    puedeReaccionar: publicada && estudiante && suscrito,
    puedeComentar: publicada && estudiante && suscrito,
    revision: esMia || u.rol === 'DIRECTOR' ? serRevision(db, i.revision) : null
  }
  if (detalle) {
    base.comentarios = db.comentarios
      .filter(c => c.incidenciaId === i.id)
      .sort((a, b) => a.fecha.localeCompare(b.fecha))
      .map(c => ({ id: c.id, texto: db.predefinidos.find(p => p.id === c.predefinidoId)?.texto, fecha: c.fecha }))
  }
  return base
}

const porPrioridad = db => (a, b) => confirmacionesDe(db, b.id) - confirmacionesDe(db, a.id) || b.fechaCreacion.localeCompare(a.fechaCreacion)
const porRecientes = (a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion)

// El correo nunca sale del backend: el administrador lo consulta en la base de datos para verificar en el campus.
function serUsuarioAdmin(u) {
  return { id: u.id, usuario: u.usuario, rol: u.rol, estado: u.estado, fechaRegistro: u.fechaRegistro }
}

// ---------- Rutas ----------
const rutas = [
  // Auth
  ['post', /^\/auth\/registro$/, ({ db, body }) => {
    const correo = (body.correo ?? '').trim().toLowerCase()
    const usuario = (body.usuario ?? '').trim()
    if (!correo.endsWith('@pucp.edu.pe')) error(400, 'Debes usar tu correo @pucp.edu.pe')
    if (!/^[a-zA-Z0-9._-]{3,15}$/.test(usuario)) error(400, 'El usuario debe tener entre 3 y 15 caracteres (letras, números, punto, guion)')
    if ((body.password ?? '').length < 6) error(400, 'La contraseña debe tener al menos 6 caracteres')
    if (db.usuarios.some(u => u.correo === correo)) error(409, 'Ese correo ya está registrado')
    if (db.usuarios.some(u => u.usuario.toLowerCase() === usuario.toLowerCase())) error(409, 'Ese usuario ya existe, elige otro')
    db.usuarios.push({ id: siguienteId(db.usuarios), usuario, correo, password: body.password, rol: 'ESTUDIANTE', estado: 'PENDIENTE', fechaRegistro: ahora() })
    auditar(db, 'CUENTA_REGISTRADA', 'Sistema', null, `Nueva cuenta pendiente: ${usuario}`)
    return { status: 201, data: { mensaje: 'Cuenta creada. Un administrador validará tu registro.' } }
  }],
  ['post', /^\/auth\/login$/, ({ db, body }) => {
    const u = db.usuarios.find(x => x.usuario.toLowerCase() === (body.usuario ?? '').trim().toLowerCase())
    if (!u || u.password !== body.password) error(401, 'Usuario o contraseña incorrectos')
    if (u.estado === 'PENDIENTE') error(403, 'Tu cuenta aún está pendiente de validación por el administrador')
    if (u.estado === 'RECHAZADA') error(403, 'Tu solicitud de cuenta fue rechazada. Comunícate con la coordinación.')
    return { data: { token: crearToken(u), usuario: u.usuario, rol: u.rol } }
  }],

  // Canales
  ['get', /^\/canales$/, ({ db, u }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    return { data: db.canales.map(c => serCanal(db, u, c)) }
  }],
  ['get', /^\/canales\/(\d+)$/, ({ db, u, args }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    return { data: serCanal(db, u, canalDe(db, args[0])) }
  }],
  ['post', /^\/canales\/(\d+)\/suscripcion$/, ({ db, u, args }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const canal = canalDe(db, args[0])
    if (!estaSuscrito(db, u, canal.id)) db.suscripciones.push({ usuarioId: u.id, canalId: canal.id })
    return { data: serCanal(db, u, canal) }
  }],
  ['delete', /^\/canales\/(\d+)\/suscripcion$/, ({ db, u, args }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const canal = canalDe(db, args[0])
    db.suscripciones = db.suscripciones.filter(s => !(s.usuarioId === u.id && s.canalId === canal.id))
    return { data: serCanal(db, u, canal) }
  }],
  ['get', /^\/canales\/(\d+)\/incidencias$/, ({ db, u, args, params }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const canal = canalDe(db, args[0])
    if (!estaSuscrito(db, u, canal.id)) error(403, 'Suscríbete al canal para ver sus incidencias')
    const lista = db.incidencias
      .filter(i => i.canalId === canal.id && esVisible(i.estado))
      .sort(params.orden === 'recientes' ? porRecientes : porPrioridad(db))
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],

  // Feed de los canales suscritos
  ['get', /^\/feed$/, ({ db, u, params }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const misCanales = db.suscripciones.filter(s => s.usuarioId === u.id).map(s => s.canalId)
    const canalId = params.canalId ? Number(params.canalId) : null
    if (canalId && !misCanales.includes(canalId)) error(403, 'Suscríbete al canal para ver sus incidencias')
    const lista = db.incidencias
      .filter(i => esVisible(i.estado) && (canalId ? i.canalId === canalId : misCanales.includes(i.canalId)))
      .sort(params.orden === 'prioridad' ? porPrioridad(db) : porRecientes)
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],
  ['get', /^\/feed\/destacadas$/, ({ db, u }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const misCanales = db.suscripciones.filter(s => s.usuarioId === u.id).map(s => s.canalId)
    const lista = db.incidencias
      .filter(i => i.estado === 'PUBLICADA' && misCanales.includes(i.canalId) && confirmacionesDe(db, i.id) > 0)
      .sort(porPrioridad(db))
      .slice(0, 5)
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],

  // Incidencias
  ['post', /^\/incidencias$/, ({ db, u, body }) => {
    requerir(u, 'ESTUDIANTE')
    const canal = canalDe(db, Number(body.canalId))
    const titulo = (body.titulo ?? '').trim()
    const descripcion = (body.descripcion ?? '').trim()
    if (titulo.length < 5 || titulo.length > 100) error(400, 'El título debe tener entre 5 y 100 caracteres')
    if (descripcion.length < 20 || descripcion.length > 1000) error(400, 'La descripción debe tener entre 20 y 1000 caracteres')
    const inc = { id: siguienteId(db.incidencias), canalId: canal.id, autorId: u.id, titulo, descripcion, estado: 'PENDIENTE', fechaCreacion: ahora(), revision: null }
    db.incidencias.push(inc)
    if (!estaSuscrito(db, u, canal.id)) db.suscripciones.push({ usuarioId: u.id, canalId: canal.id })
    notificar(db, db.usuarios.filter(x => x.rol === 'DIRECTOR').map(x => x.id), inc.id, `Nueva incidencia pendiente en ${canal.nombre}`)
    auditar(db, 'INCIDENCIA_REGISTRADA', 'Estudiante (anónimo)', inc.id, `Canal ${canal.nombre}`)
    return { status: 201, data: serIncidencia(db, u, inc) }
  }],
  ['get', /^\/incidencias\/(\d+)$/, ({ db, u, args }) => {
    requerir(u)
    const i = incidenciaDe(db, args[0])
    const esMia = i.autorId === u.id
    if (u.rol === 'ADMIN') error(403, 'El administrador consulta incidencias desde la auditoría')
    if (!esVisible(i.estado) && !esMia && u.rol !== 'DIRECTOR') error(404, 'Incidencia no encontrada')
    if (esVisible(i.estado) && !esMia && u.rol !== 'DIRECTOR' && !estaSuscrito(db, u, i.canalId)) {
      error(403, 'Suscríbete al canal para ver esta incidencia')
    }
    return { data: serIncidencia(db, u, i, { detalle: true }) }
  }],
  ['post', /^\/incidencias\/(\d+)\/confirmacion$/, ({ db, u, args }) => {
    requerir(u, 'ESTUDIANTE')
    const i = incidenciaDe(db, args[0])
    if (!serIncidencia(db, u, i).puedeConfirmar) error(403, 'No puedes confirmar esta incidencia')
    const ya = db.confirmaciones.some(c => c.incidenciaId === i.id && c.usuarioId === u.id)
    db.confirmaciones = ya
      ? db.confirmaciones.filter(c => !(c.incidenciaId === i.id && c.usuarioId === u.id))
      : [...db.confirmaciones, { usuarioId: u.id, incidenciaId: i.id }]
    return { data: { confirmaciones: confirmacionesDe(db, i.id), confirmadaPorMi: !ya } }
  }],
  ['put', /^\/incidencias\/(\d+)\/reaccion$/, ({ db, u, args, body }) => {
    requerir(u, 'ESTUDIANTE')
    const i = incidenciaDe(db, args[0])
    if (!serIncidencia(db, u, i).puedeReaccionar) error(403, 'No puedes reaccionar a esta incidencia')
    db.reacciones = db.reacciones.filter(r => !(r.incidenciaId === i.id && r.usuarioId === u.id))
    if (body.tipo) db.reacciones.push({ usuarioId: u.id, incidenciaId: i.id, tipo: body.tipo })
    return { data: { reacciones: conteoReacciones(db, i.id), miReaccion: body.tipo ?? null } }
  }],
  ['get', /^\/comentarios-predefinidos$/, ({ db, u }) => {
    requerir(u)
    return { data: db.predefinidos }
  }],
  ['post', /^\/incidencias\/(\d+)\/comentarios$/, ({ db, u, args, body }) => {
    requerir(u, 'ESTUDIANTE')
    const i = incidenciaDe(db, args[0])
    if (!serIncidencia(db, u, i).puedeComentar) error(403, 'Suscríbete al canal para comentar')
    const p = db.predefinidos.find(x => x.id === Number(body.comentarioPredefinidoId)) ?? error(400, 'Elige un comentario de la lista')
    const c = { id: siguienteId(db.comentarios), incidenciaId: i.id, usuarioId: u.id, predefinidoId: p.id, fecha: ahora() }
    db.comentarios.push(c)
    return { status: 201, data: { id: c.id, texto: p.texto, fecha: c.fecha } }
  }],
  ['get', /^\/mis-incidencias$/, ({ db, u }) => {
    requerir(u, 'ESTUDIANTE')
    return { data: db.incidencias.filter(i => i.autorId === u.id).sort(porRecientes).map(i => serIncidencia(db, u, i)) }
  }],

  // Notificaciones
  ['get', /^\/notificaciones$/, ({ db, u }) => {
    requerir(u)
    return { data: db.notificaciones.filter(n => n.usuarioId === u.id).sort((a, b) => b.fecha.localeCompare(a.fecha)).map(n => ({ id: n.id, incidenciaId: n.incidenciaId, mensaje: n.mensaje, leida: n.leida, fecha: n.fecha })) }
  }],
  ['patch', /^\/notificaciones\/(\d+)\/leida$/, ({ db, u, args }) => {
    requerir(u)
    const n = db.notificaciones.find(x => x.id === args[0] && x.usuarioId === u.id) ?? error(404, 'Notificación no encontrada')
    n.leida = true
    return { status: 204, data: null }
  }],
  ['patch', /^\/notificaciones\/leidas$/, ({ db, u }) => {
    requerir(u)
    db.notificaciones.filter(n => n.usuarioId === u.id).forEach(n => { n.leida = true })
    return { status: 204, data: null }
  }],

  // Director
  ['get', /^\/motivos-rechazo$/, ({ db, u }) => {
    requerir(u, 'DIRECTOR')
    return { data: db.motivos }
  }],
  ['get', /^\/director\/incidencias$/, ({ db, u, params }) => {
    requerir(u, 'DIRECTOR')
    const estado = params.estado ?? 'PENDIENTE'
    const lista = db.incidencias.filter(i => i.estado === estado)
    lista.sort(estado === 'PENDIENTE' ? (a, b) => a.fechaCreacion.localeCompare(b.fechaCreacion) : porPrioridad(db))
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/aprobar$/, ({ db, u, args }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PENDIENTE') error(409, 'La incidencia ya fue revisada')
    const canal = canalDe(db, i.canalId)
    i.estado = 'PUBLICADA'
    i.revision = { decision: 'APROBADA', motivoId: null, comentario: null, fecha: ahora(), directorId: u.id }
    notificar(db, [i.autorId], i.id, `Tu incidencia "${i.titulo}" fue aprobada y publicada en ${canal.nombre}`)
    notificar(db, suscritosDe(db, canal.id).filter(id => id !== i.autorId), i.id, `Nueva incidencia publicada en ${canal.nombre}: "${i.titulo}"`)
    auditar(db, 'INCIDENCIA_APROBADA', u.usuario, i.id, `Publicada en ${canal.nombre}`)
    return { data: serIncidencia(db, u, i) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/rechazar$/, ({ db, u, args, body }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PENDIENTE') error(409, 'La incidencia ya fue revisada')
    const motivo = body.motivoId ? db.motivos.find(m => m.id === Number(body.motivoId)) : null
    const comentario = (body.comentario ?? '').trim() || null
    if (!motivo && !comentario) error(400, 'Elige un motivo o escribe un comentario para el estudiante')
    i.estado = 'RECHAZADA'
    i.revision = { decision: 'RECHAZADA', motivoId: motivo?.id ?? null, comentario, fecha: ahora(), directorId: u.id }
    const razon = [motivo?.descripcion, comentario].filter(Boolean).join(' — ')
    notificar(db, [i.autorId], i.id, `Tu incidencia "${i.titulo}" fue rechazada: ${razon}`)
    auditar(db, 'INCIDENCIA_RECHAZADA', u.usuario, i.id, razon)
    return { data: serIncidencia(db, u, i) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/resolver$/, ({ db, u, args }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PUBLICADA') error(409, 'Solo se pueden resolver incidencias publicadas')
    const canal = canalDe(db, i.canalId)
    i.estado = 'RESUELTA'
    notificar(db, [i.autorId, ...suscritosDe(db, canal.id)], i.id, `La incidencia "${i.titulo}" de ${canal.nombre} fue marcada como resuelta`)
    auditar(db, 'INCIDENCIA_RESUELTA', u.usuario, i.id, `Canal ${canal.nombre}`)
    return { data: serIncidencia(db, u, i) }
  }],

  // Administrador
  ['get', /^\/admin\/usuarios$/, ({ db, u, params }) => {
    requerir(u, 'ADMIN')
    const lista = db.usuarios.filter(x => x.rol === 'ESTUDIANTE' && (!params.estado || x.estado === params.estado))
    return { data: lista.sort((a, b) => a.fechaRegistro.localeCompare(b.fechaRegistro)).map(serUsuarioAdmin) }
  }],
  ['post', /^\/admin\/usuarios\/(\d+)\/aprobar$/, ({ db, u, args }) => {
    requerir(u, 'ADMIN')
    const x = db.usuarios.find(y => y.id === args[0]) ?? error(404, 'Usuario no encontrado')
    if (x.estado !== 'PENDIENTE') error(409, 'La solicitud ya fue revisada')
    x.estado = 'ACTIVA'
    auditar(db, 'CUENTA_APROBADA', u.usuario, null, x.usuario)
    return { data: serUsuarioAdmin(x) }
  }],
  ['post', /^\/admin\/usuarios\/(\d+)\/rechazar$/, ({ db, u, args }) => {
    requerir(u, 'ADMIN')
    const x = db.usuarios.find(y => y.id === args[0]) ?? error(404, 'Usuario no encontrado')
    if (x.estado !== 'PENDIENTE') error(409, 'La solicitud ya fue revisada')
    x.estado = 'RECHAZADA'
    auditar(db, 'CUENTA_RECHAZADA', u.usuario, null, x.usuario)
    return { data: serUsuarioAdmin(x) }
  }],
  ['get', /^\/admin\/cursos$/, ({ db, u }) => {
    requerir(u, 'ADMIN')
    return { data: db.cursos.map(c => ({ ...c, tieneCanal: db.canales.some(k => k.cursoId === c.id) })) }
  }],
  ['get', /^\/admin\/canales$/, ({ db, u }) => {
    requerir(u, 'ADMIN')
    return {
      data: db.canales.map(c => ({
        ...serCanal(db, u, c),
        fechaCreacion: c.fechaCreacion,
        totalPendientes: db.incidencias.filter(i => i.canalId === c.id && i.estado === 'PENDIENTE').length
      }))
    }
  }],
  ['post', /^\/admin\/canales$/, ({ db, u, body }) => {
    requerir(u, 'ADMIN')
    const curso = db.cursos.find(c => c.id === Number(body.cursoId)) ?? error(400, 'Elige un curso')
    if (db.canales.some(c => c.cursoId === curso.id)) error(409, 'Ese curso ya tiene un canal')
    const canal = {
      id: siguienteId(db.canales),
      cursoId: curso.id,
      nombre: `${curso.codigo} · ${curso.nombre}`,
      descripcion: (body.descripcion ?? '').trim() || `Incidencias del curso de ${curso.nombre}`,
      fechaCreacion: ahora()
    }
    db.canales.push(canal)
    auditar(db, 'CANAL_CREADO', u.usuario, null, canal.nombre)
    return { status: 201, data: serCanal(db, u, canal) }
  }],
  ['get', /^\/admin\/auditoria$/, ({ db, u }) => {
    requerir(u, 'ADMIN')
    return { data: [...db.auditoria].sort((a, b) => b.fecha.localeCompare(a.fecha)) }
  }]
]

function respuesta(config, status, data) {
  return { data, status, statusText: String(status), headers: {}, config, request: {} }
}

export async function mockAdapter(config) {
  await new Promise(r => setTimeout(r, 200))
  const db = cargar()
  const metodo = (config.method ?? 'get').toLowerCase()
  const ruta = (config.url ?? '').split('?')[0]
  const body = typeof config.data === 'string' && config.data ? JSON.parse(config.data) : (config.data ?? {})
  const params = config.params ?? {}
  const u = usuarioDelToken(db, config)

  for (const [m, patron, manejador] of rutas) {
    if (m !== metodo) continue
    const match = ruta.match(patron)
    if (!match) continue
    try {
      const { status = 200, data } = manejador({ db, u, body, params, args: match.slice(1).map(Number) })
      guardar(db)
      return respuesta(config, status, data)
    } catch (e) {
      if (!(e instanceof HttpError)) throw e
      const res = respuesta(config, e.status, { mensaje: e.message })
      throw new AxiosError(e.message, 'ERR_BAD_REQUEST', config, {}, res)
    }
  }
  const res = respuesta(config, 404, { mensaje: `Mock: ${metodo.toUpperCase()} ${ruta} no implementado` })
  throw new AxiosError('No encontrado', 'ERR_BAD_REQUEST', config, {}, res)
}

export function reiniciarMock() {
  localStorage.removeItem(CLAVE)
}

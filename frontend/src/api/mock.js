// Backend simulado en el navegador. Se activa con VITE_MOCK=true (.env.development).
// Implementa el mismo contrato que docs/API.md, así el frontend no cambia al conectar Spring Boot.
// Los datos se guardan en localStorage; para reiniciarlos: localStorage.removeItem('hf_mock_db_v3')
import { AxiosError } from 'axios'

const CLAVE = 'hf_mock_db_v3'
const ROLES_VALIDOS = ['ESTUDIANTE', 'PROFESOR', 'DIRECTOR', 'ADMIN']
const CATEGORIAS = ['Evaluaciones y notas', 'Horarios y aulas', 'Infraestructura y equipos', 'Material y contenidos', 'Comunicación y avisos', 'Otro']
// Prioridad según confirmaciones: 0-1 baja, 2-4 media, 5 o más alta (RF-11.4)
const UMBRAL_ALTA = 5
const UMBRAL_MEDIA = 2
const prioridadDe = n => (n >= UMBRAL_ALTA ? 'ALTA' : n >= UMBRAL_MEDIA ? 'MEDIA' : 'BAJA')
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
      { id: 1, cursoId: 1, periodo: '2026-2', activo: true, nombre: 'INF263 · Algoritmia', descripcion: 'Incidencias del curso de Algoritmia (2026-2)', fechaCreacion: haceHoras(400) },
      { id: 2, cursoId: 2, periodo: '2026-2', activo: true, nombre: 'INF281 · Bases de Datos', descripcion: 'Incidencias del curso de Bases de Datos (2026-2)', fechaCreacion: haceHoras(400) },
      { id: 3, cursoId: 3, periodo: '2026-2', activo: true, nombre: 'INF246 · Ingeniería de Software', descripcion: 'Incidencias del curso de Ingeniería de Software (2026-2)', fechaCreacion: haceHoras(300) },
      { id: 4, cursoId: 1, periodo: '2026-1', activo: false, nombre: 'INF263 · Algoritmia', descripcion: 'Incidencias del curso de Algoritmia (2026-1)', fechaCreacion: haceHoras(3000) }
    ],
    usuarios: [
      { id: 1, usuario: 'admin', correo: 'admin@pucp.edu.pe', password: 'admin123', rol: 'ADMIN', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 2, usuario: 'director', correo: 'director.inf@pucp.edu.pe', password: 'director123', rol: 'DIRECTOR', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 3, usuario: 'profesor', correo: 'profesor.inf@pucp.edu.pe', password: 'profesor123', rol: 'PROFESOR', estado: 'ACTIVA', fechaRegistro: haceHoras(800) },
      { id: 4, usuario: 'alumno1', correo: 'alumno1@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(500) },
      { id: 5, usuario: 'alumno2', correo: 'alumno2@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(480) },
      { id: 6, usuario: 'alumno3', correo: 'alumno3@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(470) },
      { id: 7, usuario: 'exalumno', correo: 'exalumno@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'DESACTIVADA', fechaRegistro: haceHoras(600) },
      { id: 8, usuario: 'cachimbo26', correo: 'cachimbo26@pucp.edu.pe', password: 'nuevo123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(1) },
      { id: 9, usuario: 'alumno4', correo: 'alumno4@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(460) },
      { id: 10, usuario: 'alumno5', correo: 'alumno5@pucp.edu.pe', password: 'alumno123', rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: haceHoras(450) }
    ],
    suscripciones: [
      { usuarioId: 3, canalId: 1 }, { usuarioId: 3, canalId: 2 },
      { usuarioId: 4, canalId: 1 }, { usuarioId: 4, canalId: 2 },
      { usuarioId: 5, canalId: 1 }, { usuarioId: 5, canalId: 3 }, { usuarioId: 5, canalId: 4 },
      { usuarioId: 6, canalId: 2 },
      { usuarioId: 8, canalId: 1 }, { usuarioId: 9, canalId: 1 }, { usuarioId: 10, canalId: 1 }
    ],
    incidencias: [
      {
        id: 1, canalId: 1, autorId: 5, estado: 'PUBLICADA', fechaCreacion: haceHoras(50),
        categoria: 'Evaluaciones y notas',
        titulo: 'Dos evaluaciones programadas el mismo día',
        descripcion: 'La práctica calificada y la entrega del laboratorio quedaron para el mismo jueves. Varios compañeros del horario no llegamos a prepararnos bien para ambas.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(45), directorId: 2 }
      },
      {
        id: 2, canalId: 1, autorId: 4, estado: 'PUBLICADA', fechaCreacion: haceHoras(20),
        categoria: 'Evaluaciones y notas',
        titulo: 'Las notas del laboratorio no se publican',
        descripcion: 'Ya pasaron tres semanas desde el laboratorio 2 y aún no tenemos la nota ni retroalimentación, lo que complica prepararse para el siguiente.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(18), directorId: 2 }
      },
      {
        id: 3, canalId: 2, autorId: 6, estado: 'RESUELTA', fechaCreacion: haceHoras(200),
        categoria: 'Infraestructura y equipos',
        fechaAtencion: haceHoras(170), fechaResolucion: haceHoras(150),
        solucion: 'Se instaló un proyector nuevo en el aula y se verificó su funcionamiento con el docente del curso.',
        titulo: 'El aula asignada no tiene proyector',
        descripcion: 'En las sesiones de los martes no funciona el proyector y la clase se dicta sin diapositivas.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(190), directorId: 2 }
      },
      {
        id: 4, canalId: 2, autorId: 4, estado: 'PENDIENTE', fechaCreacion: haceHoras(2),
        categoria: 'Material y contenidos',
        titulo: 'Enunciado del proyecto ambiguo',
        descripcion: 'El enunciado del proyecto final tiene requisitos contradictorios y no hay un canal para hacer consultas antes de la entrega.',
        revision: null
      },
      {
        id: 5, canalId: 1, autorId: 4, estado: 'RECHAZADA', fechaCreacion: haceHoras(100),
        categoria: 'Otro',
        titulo: 'Queja general del curso',
        descripcion: 'No me gusta el curso.',
        revision: { decision: 'RECHAZADA', motivoId: 2, comentario: 'Describe una situación concreta (qué pasó y cuándo) para poder evaluarla.', fecha: haceHoras(95), directorId: 2 }
      },
      {
        id: 6, canalId: 4, autorId: 5, estado: 'RESUELTA', fechaCreacion: haceHoras(2900),
        categoria: 'Comunicación y avisos',
        fechaResolucion: haceHoras(2800),
        solucion: 'Desde este periodo los cambios de aula se avisan por el campus virtual con 48 horas de anticipación.',
        titulo: 'Cambio de aula sin aviso',
        descripcion: 'En el periodo anterior el aula del laboratorio cambió dos veces sin que se avisara por el campus virtual.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(2890), directorId: 2 }
      },
      {
        id: 7, canalId: 2, autorId: 4, estado: 'OBSERVADA', fechaCreacion: haceHoras(30), categoria: 'Horarios y aulas',
        titulo: 'Cruce de horarios entre teoría y laboratorio',
        descripcion: 'El horario del laboratorio se cruza con la clase de teoría y no hay alternativa de horario para los alumnos afectados.',
        revision: null,
        observaciones: [{ texto: 'Indica qué horarios se cruzan y cuántos alumnos se ven afectados.', fecha: haceHoras(6), directorId: 2, version: 1 }]
      },
      {
        id: 8, canalId: 2, autorId: 6, estado: 'EN_ATENCION', fechaCreacion: haceHoras(60), categoria: 'Material y contenidos',
        fechaAtencion: haceHoras(10),
        titulo: 'Las diapositivas de clase no están en el campus virtual',
        descripcion: 'Desde la segunda semana no se suben las diapositivas de las clases y varios compañeros no pueden repasar para la práctica.',
        revision: { decision: 'APROBADA', motivoId: null, comentario: null, fecha: haceHoras(55), directorId: 2 }
      }
    ],
    confirmaciones: [
      { usuarioId: 4, incidenciaId: 1 }, { usuarioId: 6, incidenciaId: 1 }, { usuarioId: 8, incidenciaId: 1 },
      { usuarioId: 9, incidenciaId: 1 }, { usuarioId: 10, incidenciaId: 1 },
      { usuarioId: 5, incidenciaId: 2 }, { usuarioId: 6, incidenciaId: 2 },
      { usuarioId: 4, incidenciaId: 3 },
      { usuarioId: 4, incidenciaId: 8 }, { usuarioId: 10, incidenciaId: 8 }
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
      { id: 2, usuarioId: 2, incidenciaId: 4, mensaje: 'Nueva incidencia pendiente en INF281 · Bases de Datos', leida: false, fecha: haceHoras(2) },
      { id: 3, usuarioId: 4, incidenciaId: 7, mensaje: 'Tu incidencia "Cruce de horarios entre teoría y laboratorio" fue observada: Indica qué horarios se cruzan y cuántos alumnos se ven afectados.', leida: false, fecha: haceHoras(6) }
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
  completarSemilla(db)
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
const esVisible = estado => estado === 'PUBLICADA' || estado === 'EN_ATENCION' || estado === 'RESUELTA'

// Estado en que queda la incidencia tras cada evento del historial (EDITADA no cambia el estado)
const ESTADO_DE_EVENTO = { CREADA: 'PENDIENTE', OBSERVADA: 'OBSERVADA', REENVIADA: 'PENDIENTE', APROBADA: 'PUBLICADA', RECHAZADA: 'RECHAZADA', EN_ATENCION: 'EN_ATENCION', RESUELTA: 'RESUELTA' }

function registrar(i, tipo, actorId, detalle = null) {
  i.historial.push({ tipo, fecha: ahora(), actorId, detalle })
}

function armarHistorial(i) {
  const ev = (tipo, fecha, actorId, detalle = null) => ({ tipo, fecha, actorId, detalle })
  const lista = [ev('CREADA', i.fechaCreacion, i.autorId)]
  for (const o of i.observaciones) lista.push(ev('OBSERVADA', o.fecha, o.directorId, o.texto))
  if (i.revision) lista.push(ev(i.revision.decision, i.revision.fecha, i.revision.directorId, i.revision.comentario))
  if (i.fechaAtencion) lista.push(ev('EN_ATENCION', i.fechaAtencion, 2))
  if (i.fechaResolucion) lista.push(ev('RESUELTA', i.fechaResolucion, 2, i.solucion))
  return lista.sort((a, b) => a.fecha.localeCompare(b.fecha))
}

// Cada incidencia conserva sus versiones, observaciones e historial de cambios (RF-12.2, RF-13.3)
function completarSemilla(db) {
  for (const i of db.incidencias) {
    i.versiones ??= [{ n: 1, titulo: i.titulo, descripcion: i.descripcion, categoria: i.categoria, fecha: i.fechaCreacion }]
    i.observaciones ??= []
    i.historial ??= armarHistorial(i)
  }
}

function validarContenido(body) {
  const titulo = (body.titulo ?? '').trim()
  const descripcion = (body.descripcion ?? '').trim()
  if (!CATEGORIAS.includes(body.categoria)) error(400, 'Elige una categoría para la incidencia')
  if (titulo.length < 5 || titulo.length > 100) error(400, 'El título debe tener entre 5 y 100 caracteres')
  if (descripcion.length < 20 || descripcion.length > 1000) error(400, 'La descripción debe tener entre 20 y 1000 caracteres')
  return { titulo, descripcion, categoria: body.categoria }
}

function historialPara(db, u, i) {
  const conActor = u.rol === 'DIRECTOR' || u.rol === 'ADMIN'
  if (!conActor && i.autorId !== u.id) return []
  const nombre = id => db.usuarios.find(x => x.id === id)?.usuario ?? null
  return i.historial
    .filter(e => conActor || ESTADO_DE_EVENTO[e.tipo])
    .map(e => ({
      tipo: e.tipo,
      estado: ESTADO_DE_EVENTO[e.tipo] ?? null,
      fecha: e.fecha,
      detalle: conActor ? e.detalle : null,
      responsable: conActor ? nombre(e.actorId) : null
    }))
}

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
    periodo: c.periodo,
    activo: c.activo,
    curso: { id: curso.id, codigo: curso.codigo, nombre: curso.nombre },
    suscrito: estaSuscrito(db, u, c.id),
    totalSuscritos: suscritosDe(db, c.id).length,
    totalPublicadas: db.incidencias.filter(i => i.canalId === c.id && esVisible(i.estado)).length
  }
}

function serCanalAdmin(db, u, c) {
  return {
    ...serCanal(db, u, c),
    fechaCreacion: c.fechaCreacion,
    totalPendientes: db.incidencias.filter(i => i.canalId === c.id && i.estado === 'PENDIENTE').length
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
  const publicada = i.estado === 'PUBLICADA' || i.estado === 'EN_ATENCION'
  const suscrito = estaSuscrito(db, u, canal.id)
  const estudiante = u.rol === 'ESTUDIANTE'
  const base = {
    id: i.id,
    titulo: i.titulo,
    descripcion: i.descripcion,
    estado: i.estado,
    fechaCreacion: i.fechaCreacion,
    canal: { id: canal.id, nombre: canal.nombre, periodo: canal.periodo },
    categoria: i.categoria,
    confirmaciones: confirmacionesDe(db, i.id),
    prioridad: prioridadDe(confirmacionesDe(db, i.id)),
    confirmadaPorMi: db.confirmaciones.some(c => c.incidenciaId === i.id && c.usuarioId === u.id),
    reacciones: conteoReacciones(db, i.id),
    miReaccion: db.reacciones.find(r => r.incidenciaId === i.id && r.usuarioId === u.id)?.tipo ?? null,
    totalComentarios: db.comentarios.filter(c => c.incidenciaId === i.id).length,
    esMia,
    puedeConfirmar: publicada && estudiante && suscrito && !esMia,
    puedeReaccionar: publicada && estudiante && suscrito,
    puedeComentar: publicada && estudiante && suscrito,
    revision: esMia || u.rol === 'DIRECTOR' ? serRevision(db, i.revision) : null,
    observacion: (esMia || u.rol === 'DIRECTOR') && i.estado === 'OBSERVADA' ? i.observaciones.at(-1)?.texto ?? null : null,
    solucion: esVisible(i.estado) || esMia || u.rol === 'DIRECTOR' ? i.solucion ?? null : null,
    fechaResolucion: i.fechaResolucion ?? null,
    puedeEditar: esMia && (i.estado === 'PENDIENTE' || i.estado === 'OBSERVADA'),
    puedeReenviar: esMia && i.estado === 'OBSERVADA' && i.versiones.length > (i.observaciones.at(-1)?.version ?? 0)
  }
  if (detalle) {
    if (esMia || u.rol === 'DIRECTOR') {
      base.observaciones = i.observaciones.map(o => ({ texto: o.texto, fecha: o.fecha, version: o.version }))
    }
    if (u.rol === 'DIRECTOR') base.versiones = i.versiones
    base.historial = historialPara(db, u, i)
    base.comentarios = db.comentarios
      .filter(c => c.incidenciaId === i.id)
      .sort((a, b) => a.fecha.localeCompare(b.fecha))
      .map(c => ({ id: c.id, texto: db.predefinidos.find(p => p.id === c.predefinidoId)?.texto, fecha: c.fecha }))
  }
  return base
}

const porPrioridad = db => (a, b) => confirmacionesDe(db, b.id) - confirmacionesDe(db, a.id) || b.fechaCreacion.localeCompare(a.fechaCreacion)
const porRecientes = (a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion)

// Solo el administrador recibe el correo de un usuario (para buscar y gestionar cuentas).
function serUsuarioAdmin(u) {
  return { id: u.id, usuario: u.usuario, correo: u.correo, rol: u.rol, estado: u.estado, fechaRegistro: u.fechaRegistro }
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
    db.usuarios.push({ id: siguienteId(db.usuarios), usuario, correo, password: body.password, rol: 'ESTUDIANTE', estado: 'ACTIVA', fechaRegistro: ahora() })
    auditar(db, 'CUENTA_REGISTRADA', 'Sistema', null, `Nueva cuenta: ${usuario}`)
    return { status: 201, data: { mensaje: 'Cuenta creada. Ya puedes iniciar sesión.' } }
  }],
  ['post', /^\/auth\/login$/, ({ db, body }) => {
    const u = db.usuarios.find(x => x.usuario.toLowerCase() === (body.usuario ?? '').trim().toLowerCase())
    if (!u || u.password !== body.password) error(401, 'Usuario o contraseña incorrectos')
    if (u.estado === 'DESACTIVADA') error(403, 'Tu cuenta está desactivada. Comunícate con la administración.')
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
    if (!canal.activo) error(409, 'Este canal está desactivado y ya no admite suscripciones')
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
      .filter(i => !params.prioridad || prioridadDe(confirmacionesDe(db, i.id)) === params.prioridad)
      .sort(params.orden === 'prioridad' ? porPrioridad(db) : porRecientes)
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],
  ['get', /^\/feed\/destacadas$/, ({ db, u }) => {
    requerir(u, 'ESTUDIANTE', 'PROFESOR')
    const misCanales = db.suscripciones.filter(s => s.usuarioId === u.id).map(s => s.canalId)
    const lista = db.incidencias
      .filter(i => (i.estado === 'PUBLICADA' || i.estado === 'EN_ATENCION') && misCanales.includes(i.canalId) && confirmacionesDe(db, i.id) > 0)
      .sort(porPrioridad(db))
      .slice(0, 5)
    return { data: lista.map(i => serIncidencia(db, u, i)) }
  }],

  // Incidencias
  ['post', /^\/incidencias$/, ({ db, u, body }) => {
    requerir(u, 'ESTUDIANTE')
    const canal = canalDe(db, Number(body.canalId))
    if (!canal.activo) error(409, 'Este canal está desactivado y ya no admite incidencias')
    const { titulo, descripcion, categoria } = validarContenido(body)
    const inc = { id: siguienteId(db.incidencias), canalId: canal.id, autorId: u.id, categoria, titulo, descripcion, estado: 'PENDIENTE', fechaCreacion: ahora(), revision: null, observaciones: [] }
    inc.versiones = [{ n: 1, titulo, descripcion, categoria, fecha: inc.fechaCreacion }]
    inc.historial = armarHistorial(inc)
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
  ['get', /^\/categorias$/, ({ u }) => {
    requerir(u)
    return { data: CATEGORIAS }
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
  ['put', /^\/incidencias\/(\d+)$/, ({ db, u, args, body }) => {
    requerir(u, 'ESTUDIANTE')
    const i = incidenciaDe(db, args[0])
    if (i.autorId !== u.id) error(403, 'Solo el autor puede editar su incidencia')
    if (i.estado !== 'PENDIENTE' && i.estado !== 'OBSERVADA') error(409, 'Solo se puede editar una incidencia pendiente u observada')
    const nuevo = validarContenido(body)
    if (nuevo.titulo === i.titulo && nuevo.descripcion === i.descripcion && nuevo.categoria === i.categoria) error(400, 'No hay cambios que guardar')
    Object.assign(i, nuevo)
    i.versiones.push({ n: i.versiones.length + 1, ...nuevo, fecha: ahora() })
    registrar(i, 'EDITADA', u.id, `Versión ${i.versiones.length}`)
    auditar(db, 'INCIDENCIA_EDITADA', u.usuario, i.id, `Versión ${i.versiones.length}`)
    return { data: serIncidencia(db, u, i, { detalle: true }) }
  }],
  ['post', /^\/incidencias\/(\d+)\/reenviar$/, ({ db, u, args }) => {
    requerir(u, 'ESTUDIANTE')
    const i = incidenciaDe(db, args[0])
    if (i.autorId !== u.id) error(403, 'Solo el autor puede reenviar su incidencia')
    if (i.estado !== 'OBSERVADA') error(409, 'Solo se puede reenviar una incidencia observada')
    if (!serIncidencia(db, u, i).puedeReenviar) error(409, 'Edita la incidencia atendiendo las observaciones antes de reenviarla')
    const canal = canalDe(db, i.canalId)
    i.estado = 'PENDIENTE'
    registrar(i, 'REENVIADA', u.id)
    notificar(db, db.usuarios.filter(x => x.rol === 'DIRECTOR').map(x => x.id), i.id, `Incidencia reenviada tras las observaciones en ${canal.nombre}: "${i.titulo}"`)
    auditar(db, 'INCIDENCIA_REENVIADA', u.usuario, i.id, `Versión ${i.versiones.length}`)
    return { data: serIncidencia(db, u, i, { detalle: true }) }
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
    registrar(i, 'APROBADA', u.id)
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
    registrar(i, 'RECHAZADA', u.id, razon)
    notificar(db, [i.autorId], i.id, `Tu incidencia "${i.titulo}" fue rechazada: ${razon}`)
    auditar(db, 'INCIDENCIA_RECHAZADA', u.usuario, i.id, razon)
    return { data: serIncidencia(db, u, i) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/observar$/, ({ db, u, args, body }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PENDIENTE') error(409, 'Solo se pueden observar incidencias pendientes')
    const texto = (body.observacion ?? '').trim()
    if (texto.length < 10) error(400, 'Indica al estudiante qué debe cambiar o editar (mínimo 10 caracteres)')
    if (texto.length > 500) error(400, 'La observación no puede superar los 500 caracteres')
    i.observaciones.push({ texto, fecha: ahora(), directorId: u.id, version: i.versiones.length })
    i.estado = 'OBSERVADA'
    registrar(i, 'OBSERVADA', u.id, texto)
    notificar(db, [i.autorId], i.id, `Tu incidencia "${i.titulo}" fue observada: ${texto}`)
    auditar(db, 'INCIDENCIA_OBSERVADA', u.usuario, i.id, texto)
    return { data: serIncidencia(db, u, i) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/atender$/, ({ db, u, args }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PUBLICADA') error(409, 'Solo se pueden pasar a atención incidencias aprobadas')
    const canal = canalDe(db, i.canalId)
    i.estado = 'EN_ATENCION'
    i.fechaAtencion = ahora()
    registrar(i, 'EN_ATENCION', u.id)
    notificar(db, [i.autorId, ...suscritosDe(db, canal.id)], i.id, `La incidencia "${i.titulo}" de ${canal.nombre} está en atención`)
    auditar(db, 'INCIDENCIA_EN_ATENCION', u.usuario, i.id, `Canal ${canal.nombre}`)
    return { data: serIncidencia(db, u, i) }
  }],
  ['post', /^\/director\/incidencias\/(\d+)\/resolver$/, ({ db, u, args, body }) => {
    requerir(u, 'DIRECTOR')
    const i = incidenciaDe(db, args[0])
    if (i.estado !== 'PUBLICADA' && i.estado !== 'EN_ATENCION') error(409, 'Solo se pueden resolver incidencias aprobadas')
    const solucion = (body.solucion ?? '').trim()
    if (solucion.length < 10) error(400, 'Describe la solución aplicada (mínimo 10 caracteres)')
    if (solucion.length > 1000) error(400, 'La solución no puede superar los 1000 caracteres')
    const canal = canalDe(db, i.canalId)
    i.estado = 'RESUELTA'
    i.solucion = solucion
    i.fechaResolucion = ahora()
    registrar(i, 'RESUELTA', u.id, solucion)
    notificar(db, [i.autorId, ...suscritosDe(db, canal.id)], i.id, `La incidencia "${i.titulo}" de ${canal.nombre} fue marcada como resuelta`)
    auditar(db, 'INCIDENCIA_RESUELTA', u.usuario, i.id, `Canal ${canal.nombre}: ${solucion}`)
    return { data: serIncidencia(db, u, i) }
  }],

  // Administrador
  ['get', /^\/admin\/usuarios$/, ({ db, u, params }) => {
    requerir(u, 'ADMIN')
    const q = (params.buscar ?? '').trim().toLowerCase()
    const lista = db.usuarios.filter(x =>
      (!q || x.usuario.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q)) &&
      (!params.rol || x.rol === params.rol)
    )
    return { data: lista.sort((a, b) => a.usuario.localeCompare(b.usuario)).map(serUsuarioAdmin) }
  }],
  ['put', /^\/admin\/usuarios\/(\d+)\/rol$/, ({ db, u, args, body }) => {
    requerir(u, 'ADMIN')
    const x = db.usuarios.find(y => y.id === args[0]) ?? error(404, 'Usuario no encontrado')
    if (!ROLES_VALIDOS.includes(body.rol)) error(400, 'Elige un rol válido')
    if (x.id === u.id) error(403, 'No puedes cambiar tu propio rol')
    if (x.rol !== body.rol) {
      auditar(db, 'ROL_ASIGNADO', u.usuario, null, `${x.usuario}: de ${x.rol} a ${body.rol}`)
      x.rol = body.rol
    }
    return { data: serUsuarioAdmin(x) }
  }],
  ['post', /^\/admin\/usuarios\/(\d+)\/desactivar$/, ({ db, u, args }) => {
    requerir(u, 'ADMIN')
    const x = db.usuarios.find(y => y.id === args[0]) ?? error(404, 'Usuario no encontrado')
    if (x.id === u.id) error(403, 'No puedes desactivar tu propia cuenta')
    if (x.estado === 'DESACTIVADA') error(409, 'La cuenta ya está desactivada')
    x.estado = 'DESACTIVADA'
    auditar(db, 'CUENTA_DESACTIVADA', u.usuario, null, x.usuario)
    return { data: serUsuarioAdmin(x) }
  }],
  ['get', /^\/periodos$/, ({ db, u }) => {
    requerir(u, 'ADMIN', 'DIRECTOR')
    return { data: [...new Set(db.canales.map(c => c.periodo))].sort().reverse() }
  }],
  ['get', /^\/admin\/cursos$/, ({ db, u, params }) => {
    requerir(u, 'ADMIN')
    const lista = db.cursos.map(c => ({ ...c, tieneCanal: db.canales.some(k => k.cursoId === c.id && k.periodo === params.periodo) }))
    return { data: lista.sort((a, b) => a.codigo.localeCompare(b.codigo)) }
  }],
  ['post', /^\/admin\/cursos$/, ({ db, u, body }) => {
    requerir(u, 'ADMIN')
    const codigo = (body.codigo ?? '').trim().toUpperCase()
    const nombre = (body.nombre ?? '').trim()
    if (!/^[A-Z0-9]{3,10}$/.test(codigo)) error(400, 'El código debe tener entre 3 y 10 letras o números')
    if (nombre.length < 3 || nombre.length > 80) error(400, 'El nombre debe tener entre 3 y 80 caracteres')
    if (db.cursos.some(c => c.codigo === codigo)) error(409, 'Ya existe un curso con ese código')
    const curso = { id: siguienteId(db.cursos), codigo, nombre }
    db.cursos.push(curso)
    auditar(db, 'CURSO_REGISTRADO', u.usuario, null, `${codigo} · ${nombre}`)
    return { status: 201, data: { ...curso, tieneCanal: false } }
  }],
  ['get', /^\/admin\/canales$/, ({ db, u, params }) => {
    requerir(u, 'ADMIN')
    const lista = db.canales
      .filter(c => !params.periodo || c.periodo === params.periodo)
      .sort((a, b) => b.periodo.localeCompare(a.periodo) || a.nombre.localeCompare(b.nombre))
    return { data: lista.map(c => serCanalAdmin(db, u, c)) }
  }],
  ['post', /^\/admin\/canales$/, ({ db, u, body }) => {
    requerir(u, 'ADMIN')
    const periodo = (body.periodo ?? '').trim()
    if (!/^\d{4}-[12]$/.test(periodo)) error(400, 'El periodo debe tener el formato año-semestre, por ejemplo 2026-2')
    const ids = [...new Set((body.cursoIds ?? []).map(Number))]
    if (ids.length === 0) error(400, 'Elige al menos un curso')
    const cursos = ids.map(id => db.cursos.find(c => c.id === id) ?? error(400, 'Uno de los cursos no existe'))
    const repetido = cursos.find(c => db.canales.some(k => k.cursoId === c.id && k.periodo === periodo))
    if (repetido) error(409, `Ya existe un canal de ${repetido.codigo} para ${periodo}`)
    const creados = cursos.map(curso => {
      const canal = {
        id: siguienteId(db.canales),
        cursoId: curso.id,
        periodo,
        activo: true,
        nombre: `${curso.codigo} · ${curso.nombre}`,
        descripcion: `Incidencias del curso de ${curso.nombre} (${periodo})`,
        fechaCreacion: ahora()
      }
      db.canales.push(canal)
      auditar(db, 'CANAL_CREADO', u.usuario, null, `${canal.nombre} (${periodo})`)
      return canal
    })
    return { status: 201, data: creados.map(c => serCanalAdmin(db, u, c)) }
  }],
  ['post', /^\/admin\/canales\/(\d+)\/desactivar$/, ({ db, u, args }) => {
    requerir(u, 'ADMIN')
    const canal = canalDe(db, args[0])
    if (!canal.activo) error(409, 'El canal ya está desactivado')
    canal.activo = false
    auditar(db, 'CANAL_DESACTIVADO', u.usuario, null, `${canal.nombre} (${canal.periodo})`)
    return { data: serCanalAdmin(db, u, canal) }
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

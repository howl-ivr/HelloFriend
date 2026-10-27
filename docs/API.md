# HelloFriend · Contrato de la API

Base: `/api` · JSON · Autenticación con `Authorization: Bearer <JWT>` (excepto `/api/auth/registro`, `/login`, `/recuperar` y `/restablecer`).

El frontend ya implementa este contrato en modo simulado (`frontend/src/api/mock.js`). Si el backend respeta
rutas, campos y códigos de estado, basta con poner `VITE_MOCK=false` en `frontend/.env.development`.

Esta versión sigue el documento de Requerimientos v3.

## Reglas generales

- **Registro libre:** cualquier correo `@pucp.edu.pe` crea una cuenta `ACTIVA` con rol `ESTUDIANTE`; no hay aprobación del administrador (RF-01.4).
  El administrador puede asignar otro rol o desactivar la cuenta. Una cuenta desactivada no inicia sesión y conserva su historial.
- **Anonimato:** ni los estudiantes ni los profesores reciben jamás el autor de una incidencia ni quién la confirmó.
  El campo `autor` solo llega al `DIRECTOR` y al `ADMIN`; las confirmaciones con identidad solo las ve el `ADMIN` (auditoría).
  El backend calcula `esMia` para el usuario actual.
- **Correo:** solo el `ADMIN` lo recibe, en la gestión de usuarios y en el detalle de auditoría.
- **Errores:** cuerpo `{ "mensaje": "texto para mostrar al usuario" }`.
  `400` validación · `401` no autenticado o credenciales inválidas · `403` sin permiso · `404` no existe · `409` conflicto.
- **JWT:** el payload debe incluir `sub` (id), `usuario` y `rol` (`ESTUDIANTE` | `PROFESOR` | `DIRECTOR` | `ADMIN`).
  El cliente lee el rol del token, así que un cambio de rol se aplica en el siguiente inicio de sesión.
- **Fechas:** ISO-8601 (`2026-10-25T19:30:00`). Los filtros `desde` y `hasta` usan `YYYY-MM-DD` y se aplican a la fecha de registro.
- **Periodo académico:** `YYYY-1` o `YYYY-2` (por ejemplo `2026-2`).
- No se valida matrícula por curso: cualquier estudiante puede reportar en cualquier canal activo.

## Estados de una incidencia

```
PENDIENTE -> OBSERVADA -> (el estudiante edita y reenvía) -> PENDIENTE -> PUBLICADA o RECHAZADA
PUBLICADA -> EN_ATENCION -> RESUELTA        (también PUBLICADA -> RESUELTA directo)
```

`PUBLICADA` es el estado "Aprobada" del documento de requerimientos (se conserva el nombre técnico).
Visibles para los suscritos del canal: `PUBLICADA`, `EN_ATENCION` y `RESUELTA`. Se puede confirmar, reaccionar y comentar en `PUBLICADA` y `EN_ATENCION`.

**Prioridad** según confirmaciones: 0 o 1 `BAJA` · 2 a 4 `MEDIA` · 5 o más `ALTA`.

**Categorías:** `Evaluaciones y notas`, `Horarios y aulas`, `Infraestructura y equipos`, `Material y contenidos`,
`Comunicación y avisos`, `Otro`. La lista vigente la entrega `GET /categorias`.

## Objetos

**Canal**
```json
{ "id": 1, "nombre": "INF263 · Algoritmia", "descripcion": "…", "periodo": "2026-2", "activo": true,
  "curso": { "id": 1, "codigo": "INF263", "nombre": "Algoritmia" },
  "suscrito": true, "totalSuscritos": 12, "totalPublicadas": 3 }
```
Un canal vincula un curso con un periodo. Un canal desactivado conserva sus incidencias pero no admite suscripciones nuevas ni incidencias nuevas.

**Incidencia**
```json
{ "id": 7, "titulo": "…", "descripcion": "…", "categoria": "Evaluaciones y notas",
  "estado": "PUBLICADA", "fechaCreacion": "…",
  "canal": { "id": 1, "nombre": "INF263 · Algoritmia", "periodo": "2026-2" },
  "autor": null,
  "confirmaciones": 5, "prioridad": "ALTA", "confirmadaPorMi": false,
  "reacciones": { "APOYO": 2, "PREOCUPANTE": 0, "URGENTE": 1, "LO_SIGO": 0 }, "miReaccion": null,
  "totalComentarios": 3, "esMia": false,
  "puedeConfirmar": true, "puedeReaccionar": true, "puedeComentar": true,
  "puedeEditar": false, "puedeReenviar": false,
  "revision": null, "observacion": null, "solucion": null, "fechaResolucion": null }
```
- `autor`: `{ "usuario": "alumno2" }` solo para `DIRECTOR` y `ADMIN`; `null` para los demás.
- `revision` (solo si `esMia` o el usuario es `DIRECTOR`; si no, `null`):
  `{ "decision": "APROBADA|RECHAZADA", "motivo": "texto|null", "comentario": "texto|null", "fecha": "…" }`
- `observacion`: texto de la última observación mientras el estado es `OBSERVADA` (solo `esMia` o `DIRECTOR`).
- `solucion` y `fechaResolucion`: solo en `RESUELTA`.
- `puedeConfirmar` = (`PUBLICADA` o `EN_ATENCION`) y rol `ESTUDIANTE` y suscrito y no es suya
- `puedeReaccionar` y `puedeComentar` = (`PUBLICADA` o `EN_ATENCION`) y rol `ESTUDIANTE` y suscrito
- `puedeEditar` = `esMia` y estado `PENDIENTE` u `OBSERVADA`
- `puedeReenviar` = `esMia` y estado `OBSERVADA` y existe una versión posterior a la última observación
- En `GET /incidencias/{id}` se agregan:
  - `comentarios`: `[{ "id", "texto", "fecha" }]`
  - `observaciones`: `[{ "texto", "fecha", "version" }]` (solo `esMia`, `DIRECTOR`)
  - `historial`: `[{ "tipo", "estado", "fecha", "detalle", "responsable" }]` (solo `esMia` o `DIRECTOR`).
    Para el estudiante solo incluye los cambios de estado, con `detalle` y `responsable` en `null`.
    Para el director incluye también las ediciones, con responsable y detalle.
  - `versiones`: `[{ "n", "titulo", "descripcion", "categoria", "fecha" }]` (solo `DIRECTOR`)
- Tipos de evento del historial: `CREADA`, `EDITADA`, `OBSERVADA`, `REENVIADA`, `APROBADA`, `RECHAZADA`, `EN_ATENCION`, `RESUELTA`.

## Endpoints

### Auth (público)
| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/auth/registro` | `{ correo, usuario, password }` | `201 { mensaje }` · la cuenta queda `ACTIVA` como `ESTUDIANTE` |
| POST | `/auth/login` | `{ usuario, password }` | `{ token, usuario, rol }` · `401` credenciales · `403` cuenta desactivada |
| POST | `/auth/recuperar` | `{ correo }` | `200 { mensaje }` **siempre**, exista o no el correo. Si es de una cuenta activa, envía un correo con el enlace `{FRONTEND_URL}/restablecer?token=…` (vence a los 30 min; pedir otro invalida el anterior) |
| POST | `/auth/restablecer` | `{ token, password }` | `200 { mensaje }` · cambia la contraseña, consume el enlace (un solo uso) y cierra las sesiones abiertas · `400` si el enlace es desconocido, vencido o ya usado, o si el password no tiene de 6 a 64 caracteres |

Validaciones de registro: correo termina en `@pucp.edu.pe` y es único; usuario `^[a-zA-Z0-9._-]{3,15}$`, único; password ≥ 6 y como máximo 64 caracteres.

La pantalla del enlace del correo es `/restablecer?token=…`: lee el `token` de la URL y lo envía a `/auth/restablecer` junto con la contraseña nueva.

### Sesión (cualquier rol, con token)
| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/auth/me` | — | `{ usuario, rol }` del usuario del token (nunca el correo) |
| POST | `/auth/logout` | — | `204` · invalida **todos** los tokens del usuario, también los de otros dispositivos |
| PUT | `/auth/me/contrasena` | `{ contrasenaActual, contrasenaNueva }` | `{ token, usuario, rol }` · cierra las demás sesiones y devuelve un **token nuevo**: el cliente debe guardarlo · `400` si la actual es incorrecta (no `401`, para no confundirlo con una sesión vencida), si la nueva es igual a la actual o no tiene de 6 a 64 caracteres |

Un token deja de valer al cerrar sesión, cambiar o restablecer la contraseña, cambiar el rol o desactivar la cuenta: la siguiente petición responde `401` y el cliente debe volver al login.

### Estudiante y profesor
| Método | Ruta | Notas |
|---|---|---|
| GET | `/canales` | Lista de Canal (activos e inactivos, de todos los periodos) |
| GET | `/canales/{id}` | Canal |
| POST | `/canales/{id}/suscripcion` | Suscribe · devuelve Canal · `409` si el canal está desactivado |
| DELETE | `/canales/{id}/suscripcion` | Desuscribe · devuelve Canal |
| GET | `/feed?orden=recientes\|prioridad&canalId=&prioridad=ALTA\|MEDIA\|BAJA` | Pantalla de inicio: incidencias visibles de los canales suscritos (o solo `canalId`, que debe estar suscrito) |
| GET | `/feed/destacadas` | Panel "Lo más confirmado": hasta 5 incidencias `PUBLICADA` o `EN_ATENCION` de los canales suscritos con al menos 1 confirmación |
| GET | `/canales/{id}/incidencias?orden=prioridad\|recientes` | Solo visibles · `403` si no está suscrito |
| GET | `/incidencias/{id}` | No visibles: solo autor o director · visibles: suscritos, autor o director · `403` para el administrador (usa `/admin/reportes/{id}`) |
| GET | `/categorias` | `["Evaluaciones y notas", …]` (cualquier rol autenticado) |
| GET | `/comentarios-predefinidos` | `[{ id, texto }]` |
| GET | `/notificaciones` | `[{ id, incidenciaId, mensaje, leida, fecha }]` (también director) |
| PATCH | `/notificaciones/{id}/leida` | `204` |
| PATCH | `/notificaciones/leidas` | Marca todas · `204` |

### Solo estudiante
| Método | Ruta | Body | Notas |
|---|---|---|---|
| POST | `/incidencias` | `{ canalId, categoria, titulo, descripcion }` | Canal activo · categoría de la lista · título 5 a 100 · descripción 20 a 1000 · el autor queda suscrito · queda `PENDIENTE` · notifica a directores · `201` Incidencia |
| PUT | `/incidencias/{id}` | `{ categoria, titulo, descripcion }` | Solo el autor, en `PENDIENTE` u `OBSERVADA` · no cambia el canal · `400` si no hay cambios · crea una versión nueva · devuelve Incidencia con detalle |
| POST | `/incidencias/{id}/reenviar` | — | Solo el autor, desde `OBSERVADA`, y debe haber editado después de la última observación (`409` si no) · pasa a `PENDIENTE` · notifica a los directores |
| POST | `/incidencias/{id}/confirmacion` | — | Alterna · `{ confirmaciones, confirmadaPorMi }` |
| PUT | `/incidencias/{id}/reaccion` | `{ tipo }` o `{ tipo: null }` | Una por usuario, reemplaza la anterior · `{ reacciones, miReaccion }` |
| POST | `/incidencias/{id}/comentarios` | `{ comentarioPredefinidoId }` | `201 { id, texto, fecha }` |
| GET | `/mis-incidencias` | — | Incidencias del usuario, con `revision` y `observacion` |

### Director
| Método | Ruta | Body | Notas |
|---|---|---|---|
| GET | `/director/incidencias?estado=PENDIENTE\|OBSERVADA\|PUBLICADA\|EN_ATENCION\|RESUELTA` | — | `PENDIENTE`: más antiguas primero · otras: por prioridad |
| POST | `/director/incidencias/{id}/aprobar` | — | Solo desde `PENDIENTE` · pasa a `PUBLICADA` · notifica al autor y a los suscritos |
| POST | `/director/incidencias/{id}/observar` | `{ observacion }` | Solo desde `PENDIENTE` · 10 a 500 caracteres · pasa a `OBSERVADA` · notifica solo al autor |
| POST | `/director/incidencias/{id}/rechazar` | `{ motivoId, comentario }` | Solo desde `PENDIENTE` · al menos uno de los dos · pasa a `RECHAZADA` · notifica al autor con el motivo |
| POST | `/director/incidencias/{id}/atender` | — | Solo desde `PUBLICADA` · pasa a `EN_ATENCION` · notifica al autor y a los suscritos |
| POST | `/director/incidencias/{id}/resolver` | `{ solucion }` | Desde `PUBLICADA` o `EN_ATENCION` · solución obligatoria de 10 a 1000 caracteres · pasa a `RESUELTA` · notifica al autor y a los suscritos |
| GET | `/director/estadisticas?desde=&hasta=&periodo=` | — | Ver abajo · `400` si `desde` es posterior a `hasta` |
| GET | `/motivos-rechazo` | — | `[{ id, descripcion }]` |
| GET | `/periodos` | — | `["2026-2", "2026-1"]` (también administrador) |

Respuesta de `/director/estadisticas`:
```json
{ "total": 8, "resueltas": 2, "tiempoPromedioHoras": 65,
  "porCurso": [{ "nombre": "INF263 · Algoritmia", "total": 4 }],
  "porCategoria": [{ "nombre": "Evaluaciones y notas", "total": 2 }],
  "porEstado": [{ "estado": "PENDIENTE", "total": 1 }],
  "masConfirmadas": [{ "id": 1, "titulo": "…", "estado": "PUBLICADA", "canal": "INF263 · Algoritmia", "confirmaciones": 5, "prioridad": "ALTA" }] }
```
`tiempoPromedioHoras` considera solo las `RESUELTA`, desde la aprobación hasta la resolución (`null` si no hay ninguna). `masConfirmadas` trae como máximo 5.
Los filtros se aplican a la fecha de registro de la incidencia y al periodo de su canal.

### Administrador
| Método | Ruta | Body | Notas |
|---|---|---|---|
| GET | `/admin/usuarios?buscar=&rol=` | — | `[{ id, usuario, correo, rol, estado, fechaRegistro }]` · `buscar` filtra por usuario o correo · `estado`: `ACTIVA` o `DESACTIVADA` |
| PUT | `/admin/usuarios/{id}/rol` | `{ rol }` | Asigna el rol · `403` si es su propia cuenta |
| POST | `/admin/usuarios/{id}/desactivar` | — | `403` si es su propia cuenta · `409` si ya está desactivada |
| POST | `/admin/usuarios/{id}/reactivar` | — | La cuenta vuelve a `ACTIVA`; las sesiones anteriores no reviven (debe iniciar sesión) · `409` si ya está activa |
| GET | `/admin/cursos?periodo=` | — | `[{ id, codigo, nombre, tieneCanal }]` · `tieneCanal` indica si el curso ya tiene canal en ese periodo |
| POST | `/admin/cursos` | `{ codigo, nombre }` | Registra un curso nuevo · código 3 a 10 letras o números, único (`409`) · `201` |
| GET | `/admin/canales?periodo=` | — | Canal + `fechaCreacion`, `totalPendientes` · más recientes primero |
| POST | `/admin/canales` | `{ periodo, cursoIds: [] }` | Crea un canal por curso para el periodo · `409` si alguno ya tiene canal en ese periodo · nombre `"{codigo} · {nombre}"` · `201` lista de Canal |
| POST | `/admin/canales/{id}/desactivar` | — | `409` si ya está desactivado |
| GET | `/admin/reportes?desde=&hasta=&cursoId=&estado=&usuario=` | — | Lista de reportes para auditoría · `usuario` coincide con autor, confirmante o responsable de algún cambio |
| GET | `/admin/reportes/{id}` | — | Historial completo (ver abajo) |
| GET | `/admin/auditoria` | — | Registro general: `[{ id, accion, actor, incidenciaId, detalle, fecha }]`, más recientes primero |

Respuesta de `/admin/reportes/{id}`:
```json
{ "id": 1, "titulo": "…", "descripcion": "…", "categoria": "…", "estado": "PUBLICADA", "fechaCreacion": "…",
  "canal": { "id": 1, "nombre": "…", "periodo": "2026-2" },
  "autor": { "usuario": "alumno2", "correo": "alumno2@pucp.edu.pe" },
  "confirmaciones": 5, "prioridad": "ALTA", "solucion": null,
  "versiones": [{ "n": 1, "titulo": "…", "descripcion": "…", "categoria": "…", "fecha": "…" }],
  "eventos": [{ "tipo": "CREADA", "estado": "PENDIENTE", "fecha": "…", "detalle": null, "responsable": "alumno2" }] }
```
`eventos` incluye creación, ediciones, observaciones, reenvíos, decisiones, cambios de estado y cada confirmación (`tipo: "CONFIRMADA"`, con el usuario que confirmó), ordenados por fecha.
La auditoría es de solo lectura: el administrador no puede modificar ni eliminar reportes ni su historial.

Acciones del registro general: `CUENTA_REGISTRADA`, `CUENTA_DESACTIVADA`, `ROL_ASIGNADO`, `CURSO_REGISTRADO`, `CANAL_CREADO`, `CANAL_DESACTIVADO`,
`INCIDENCIA_REGISTRADA`, `INCIDENCIA_EDITADA`, `INCIDENCIA_OBSERVADA`, `INCIDENCIA_REENVIADA`, `INCIDENCIA_APROBADA`,
`INCIDENCIA_RECHAZADA`, `INCIDENCIA_EN_ATENCION`, `INCIDENCIA_RESUELTA`. El actor es el nombre de usuario de quien ejecutó la acción.

## Datos precargados recomendados

Usuarios `admin`, `director` y `profesor` (no se registran solos), catálogo de cursos, canales del periodo vigente, categorías, motivos de rechazo y
comentarios predefinidos. Los valores de ejemplo están en la función `semilla()` de `frontend/src/api/mock.js`.

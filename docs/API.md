# HelloFriend · Contrato de la API

Base: `/api` · JSON · Autenticación con `Authorization: Bearer <JWT>` (excepto `/api/auth/**`).

El frontend ya implementa este contrato en modo simulado (`frontend/src/api/mock.js`). Si el backend respeta
rutas, campos y códigos de estado, basta con poner `VITE_MOCK=false` en `frontend/.env.development`.

## Reglas generales

- **Correo:** solo se recibe en el registro. Ninguna respuesta lo devuelve, ni al administrador: él lo consulta en la base de datos para verificar en el campus que el solicitante es alumno de la facultad.
- **Anonimato:** ninguna respuesta incluye el autor de una incidencia (ni id, ni usuario, ni correo), ni siquiera
  para el director o el administrador. El backend calcula `esMia` para el usuario actual.
- **Errores:** cuerpo `{ "mensaje": "texto para mostrar al usuario" }`.
  `400` validación · `401` no autenticado o credenciales inválidas · `403` sin permiso · `404` no existe · `409` conflicto.
- **JWT:** el payload debe incluir `sub` (id), `usuario` y `rol` (`ESTUDIANTE` | `PROFESOR` | `DIRECTOR` | `ADMIN`).
- **Fechas:** ISO-8601 (`2026-10-25T19:30:00`).
- No se valida matrícula por curso: cualquier estudiante aprobado puede reportar en cualquier canal.

## Objetos

**Canal**
```json
{ "id": 1, "nombre": "INF263 · Algoritmia", "descripcion": "…",
  "curso": { "id": 1, "codigo": "INF263", "nombre": "Algoritmia" },
  "suscrito": true, "totalSuscritos": 12, "totalPublicadas": 3 }
```

**Incidencia**
```json
{ "id": 7, "titulo": "…", "descripcion": "…", "estado": "PUBLICADA", "fechaCreacion": "…",
  "canal": { "id": 1, "nombre": "INF263 · Algoritmia" },
  "confirmaciones": 5, "confirmadaPorMi": false,
  "reacciones": { "APOYO": 2, "PREOCUPANTE": 0, "URGENTE": 1, "LO_SIGO": 0 }, "miReaccion": null,
  "totalComentarios": 3, "esMia": false,
  "puedeConfirmar": true, "puedeReaccionar": true, "puedeComentar": true,
  "revision": null }
```
- `estado`: `PENDIENTE` | `PUBLICADA` | `RECHAZADA` | `RESUELTA`
- `revision` (solo si `esMia` o el usuario es DIRECTOR; si no, `null`):
  `{ "decision": "APROBADA|RECHAZADA", "motivo": "texto|null", "comentario": "texto|null", "fecha": "…" }`
- En `GET /incidencias/{id}` se agrega `"comentarios": [{ "id", "texto", "fecha" }]`.
- `puedeConfirmar` = PUBLICADA && rol ESTUDIANTE && suscrito && no es suya
- `puedeReaccionar` = PUBLICADA && rol ESTUDIANTE && suscrito
- `puedeComentar` = PUBLICADA && rol ESTUDIANTE && suscrito

## Endpoints

### Auth (público)
| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/auth/registro` | `{ correo, usuario, password }` | `201 { mensaje }` · cuenta queda `PENDIENTE` |
| POST | `/auth/login` | `{ usuario, password }` | `{ token, usuario, rol }` · `401` credenciales · `403` cuenta PENDIENTE/RECHAZADA |

Validaciones de registro: correo termina en `@pucp.edu.pe`; usuario `^[a-zA-Z0-9._-]{3,15}$`, único; password ≥ 6.

### Estudiante y profesor
| Método | Ruta | Notas |
|---|---|---|
| GET | `/canales` | Lista de Canal |
| GET | `/canales/{id}` | Canal |
| POST | `/canales/{id}/suscripcion` | Suscribe · devuelve Canal |
| DELETE | `/canales/{id}/suscripcion` | Desuscribe · devuelve Canal |
| GET | `/feed?orden=recientes\|prioridad&canalId=` | Pantalla de inicio: PUBLICADA y RESUELTA de los canales suscritos (o solo `canalId`, que debe estar suscrito) |
| GET | `/feed/destacadas` | Panel "Lo más confirmado": hasta 5 PUBLICADA de los canales suscritos con ≥1 confirmación, por prioridad |
| GET | `/canales/{id}/incidencias?orden=prioridad\|recientes` | Solo PUBLICADA y RESUELTA · `403` si no está suscrito · prioridad = confirmaciones desc |
| GET | `/incidencias/{id}` | PENDIENTE/RECHAZADA: solo autor o director. PUBLICADA/RESUELTA: suscritos, autor o director |
| GET | `/comentarios-predefinidos` | `[{ id, texto }]` |
| GET | `/notificaciones` | `[{ id, incidenciaId, mensaje, leida, fecha }]` (también director) |
| PATCH | `/notificaciones/{id}/leida` | `204` |
| PATCH | `/notificaciones/leidas` | Marca todas · `204` |

### Solo estudiante
| Método | Ruta | Body | Notas |
|---|---|---|---|
| POST | `/incidencias` | `{ canalId, titulo, descripcion }` | Cualquier canal · el autor queda suscrito al canal · título 5–100 · descripción 20–1000 · queda `PENDIENTE` · notifica a directores · `201` Incidencia |
| POST | `/incidencias/{id}/confirmacion` | — | Alterna · `{ confirmaciones, confirmadaPorMi }` |
| PUT | `/incidencias/{id}/reaccion` | `{ tipo }` o `{ tipo: null }` | Una por usuario, reemplaza la anterior · `{ reacciones, miReaccion }` |
| POST | `/incidencias/{id}/comentarios` | `{ comentarioPredefinidoId }` | `201 { id, texto, fecha }` |
| GET | `/mis-incidencias` | — | Incidencias del usuario, con `revision` |

### Director
| Método | Ruta | Body | Notas |
|---|---|---|---|
| GET | `/director/incidencias?estado=PENDIENTE\|PUBLICADA\|RESUELTA` | — | PENDIENTE: más antiguas primero · otras: por prioridad |
| POST | `/director/incidencias/{id}/aprobar` | — | → PUBLICADA · notifica al autor y a los suscritos |
| POST | `/director/incidencias/{id}/rechazar` | `{ motivoId, comentario }` | Al menos uno de los dos · → RECHAZADA · notifica al autor con el motivo |
| POST | `/director/incidencias/{id}/resolver` | — | Solo desde PUBLICADA · → RESUELTA · notifica autor y suscritos |
| GET | `/motivos-rechazo` | — | `[{ id, descripcion }]` |

### Administrador
| Método | Ruta | Body | Notas |
|---|---|---|---|
| GET | `/admin/usuarios?estado=PENDIENTE` | — | Estudiantes: `[{ id, usuario, rol, estado, fechaRegistro }]` (sin correo) |
| POST | `/admin/usuarios/{id}/aprobar` | — | PENDIENTE → ACTIVA |
| POST | `/admin/usuarios/{id}/rechazar` | — | PENDIENTE → RECHAZADA |
| GET | `/admin/cursos` | — | `[{ id, codigo, nombre, tieneCanal }]` |
| GET | `/admin/canales` | — | Canal + `fechaCreacion`, `totalPendientes` |
| POST | `/admin/canales` | `{ cursoId, descripcion }` | Un canal por curso (`409` si existe) · nombre = `"{codigo} · {nombre}"` |
| GET | `/admin/auditoria` | — | `[{ id, accion, actor, incidenciaId, detalle, fecha }]`, más recientes primero |

Acciones de auditoría: `CUENTA_REGISTRADA`, `CUENTA_APROBADA`, `CUENTA_RECHAZADA`, `CANAL_CREADO`,
`INCIDENCIA_REGISTRADA` (actor siempre `"Estudiante (anónimo)"`), `INCIDENCIA_APROBADA`,
`INCIDENCIA_RECHAZADA`, `INCIDENCIA_RESUELTA`. Las confirmaciones, reacciones y comentarios no se auditan
con actor, para no vincular alumnos con incidencias.

## Datos precargados recomendados

Usuarios `admin`, `director` y `profesor` (no se registran solos), catálogo de cursos, motivos de rechazo y
comentarios predefinidos. Los valores de ejemplo están en la función `semilla()` de `frontend/src/api/mock.js`.

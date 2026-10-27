# Backend de HelloFriend

API REST en **Java 21 + Spring Boot 4.1** con **MySQL 8.0**, Flyway, Spring Data JPA y Maven Wrapper. Formato con
Spotless; pruebas unitarias con Surefire (`*Tests`) y de integración con Failsafe (`*IT`).

El frontend consume esta API directamente. Los roles usan `ADMINISTRADOR` y los identificadores son UUID; la API expone `PUBLICADA` para el estado `APROBADA` guardado en MySQL.

## Cómo arrancar

1. MySQL local con las bases `hellofriend` y `hellofriend_test` (ver [`docs/guias/base-de-datos.md`](../docs/guias/base-de-datos.md)).
2. Copia `.env.example` como `.env` y completa `DB_PASSWORD` para la aplicación y `TEST_DB_PASSWORD` para tu MySQL local de pruebas (`.env` está ignorado por Git).
3. `.\mvnw.cmd spring-boot:run` arranca la API en <http://localhost:8080/api> (salud: `/api/actuator/health`).

## Configuración

Todo sale de variables de entorno (o de `backend/.env`, que las variables reales del entorno sobrescriben).
`.env.example` es solo una plantilla y no se carga al arrancar. Desde el IDE, el directorio de trabajo puede ser
`backend/` o la raíz del proyecto; ambos permiten cargar `backend/.env`. Si MySQL muestra otro usuario en un error,
revisa también las variables de ejecución `DB_USER` y `SPRING_DATASOURCE_USERNAME` del IDE, que prevalecen sobre `.env`.

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/hellofriend?...` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la base | `hellofriend` / contraseña vacía |
| `TEST_DB_URL` | Base de las pruebas de integración (debe ser local y terminar en `_test`) | `.../hellofriend_test?...` |
| `TEST_DB_USER` / `TEST_DB_PASSWORD` | Credenciales **locales** de las pruebas; no se reutilizan `DB_USER` ni `DB_PASSWORD` | usuario `hellofriend` / contraseña vacía |
| `FLYWAY_LOCATIONS` | Dónde buscar migraciones al arrancar | `classpath:db/migration` |
| `FLYWAY_URL` / `FLYWAY_USER` / `FLYWAY_PASSWORD` | Solo para `mvnw flyway:*` a mano (van en la terminal, no en `.env`) | sin valor |
| `PORT` | Puerto HTTP | `8080` |
| `CORS_ORIGINS` | Orígenes del frontend permitidos | `http://localhost:5173` |
| `JWT_SECRET` | Secreto que firma los tokens (mínimo 32 bytes). **Obligatorio fuera de local**: con el valor de desarrollo, que es público, cualquiera podría fabricar un token de administrador. Si la base de datos no es `localhost`, `127.0.0.1` ni `::1` y falta `JWT_SECRET`, la aplicación no arranca | valor de desarrollo (solo con base local) |
| `JWT_HORAS` | Horas de vigencia del token | `8` |
| `FRONTEND_URL` | Dónde abre el enlace del correo de recuperación (`/restablecer?token=...`) | `http://localhost:5173` |
| `RECUPERACION_MINUTOS` | Minutos de vigencia del enlace de recuperación | `30` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` / `MAIL_FROM` | Correo (en local, Mailpit) | `localhost` / `1025` |

Nunca se usan credenciales de RDS en un equipo de desarrollo ni en el CI.

## Estructura

```
src/main/java/com/hellofriend/
├── HelloFriendAplicacion.java
├── compartido/            # reloj (Clock America/Lima), seguridad (JWT, BCrypt, roles), errores y @Async; luego, paginación
└── {modulo}/              # uno por módulo: dominio, aplicacion, infraestructura, web
src/main/resources/
├── application.properties
└── db/migration/          # V{n}__{modulo}_{tema}.sql
src/test/java/com/hellofriend/
├── PruebaIntegracion.java # base de las *IT: contexto completo, transacción que se deshace, solo BD local _test
└── ...
```

Hora actual: siempre del bean `Clock`, nunca `LocalDate.now()` suelto. Las fechas se guardan en UTC.

## Base de datos

Convenciones, uso de Flyway y plan de migraciones: [`docs/guias/base-de-datos.md`](../docs/guias/base-de-datos.md).
Una migración ya subida nunca se edita.

| Migración | Qué crea |
| --- | --- |
| `V1__usuarios_cuentas.sql` | Tabla `usuarios_usuario` (correo, usuario de hasta 15 caracteres, contraseña cifrada, rol, activo) |
| `V2__modelo_incidencias.sql` | Columnas de sesión y recuperación en `usuarios_usuario`; tablas `academico_curso`, `canales_canal`, `canales_suscripcion`, `incidencias_categoria` (8 categorías cargadas), `incidencias_incidencia`, `incidencias_version`, `incidencias_observacion`, `incidencias_confirmacion`, `incidencias_historial_estado`, `notificaciones_notificacion` y la vista `auditoria_historial_incidencia` |
| `V3__usuarios_estado_cuenta.sql` | Columna `estado` (`PENDIENTE`, `ACTIVA`, `RECHAZADA`; por defecto `ACTIVA`) en `usuarios_usuario`. **Sin uso:** el contrato eliminó la aprobación de cuentas y la entidad no la mapea; la base la rellena con `ACTIVA` |
| `V4__incidencias_interaccion.sql` | Catálogos de motivos y comentarios, comentarios, reacciones, metadatos de revisión e historial, y eventos de auditoría |
| `V5__notificaciones_tipo_nueva.sql` | Agrega el tipo `NUEVA` a las notificaciones (aviso a los directores al registrarse una incidencia) |
| `V6__academico_ciclo_cursos.sql` | Añade `ciclo` al catálogo de cursos y etiqueta los cursos obligatorios de Informática de los ciclos 5 a 10 del plan 2026-2 |

Datos de prueba (usuarios demo con clave `Prueba#2026`, canales e incidencias en cada estado): `db/datos-prueba/R__datos_prueba.sql`. Solo se cargan si defines `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/datos-prueba` en tu `.env` local o en un servidor de pruebas; nunca en producción. Las pruebas de integración no los cargan.

## API

Prefijo `/api`. Errores en JSON `{ "mensaje": "..." }` (formato de `docs/API.md`). Todo exige `Authorization: Bearer <token>`
salvo lo marcado como público. La columna Rol indica el rol exigido con `@PreAuthorize`: sin token responde 401 y con otro
rol, 403 (`{ "mensaje": "No tienes permiso para realizar esta acción." }`).

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/actuator/health` | público | Estado de la aplicación |
| POST | `/auth/registro` | público | `{ correo, usuario, password }` → 201 `{ mensaje }`. La cuenta queda `ACTIVA` como ESTUDIANTE, sin aprobación del administrador. 400 si el correo no es `@pucp.edu.pe`, el usuario no cumple `^[a-zA-Z0-9._-]{3,15}$` o la contraseña no tiene de 6 a 64 caracteres; 409 si el usuario o el correo ya existen |
| POST | `/auth/login` | público | `{ usuario, password }` → `{ token, usuario, rol }`. 401 si las credenciales fallan (mismo mensaje exista o no el usuario); 403 si la cuenta está desactivada |
| POST | `/auth/recuperar` | público | `{ correo }` → 200 `{ mensaje }` siempre, exista o no el correo. Si es de una cuenta que puede entrar, envía un enlace `FRONTEND_URL/restablecer?token=...` (vence en `RECUPERACION_MINUTOS`, 30 por defecto). Pedir otro invalida el anterior |
| POST | `/auth/restablecer` | público | `{ token, password }` → 200 `{ mensaje }`. Cambia la contraseña, consume el enlace (un solo uso) y cierra todas las sesiones. 400 si el enlace es desconocido, vencido o usado (mismo mensaje) o si la contraseña no tiene de 6 a 64 caracteres |
| GET | `/auth/me` | autenticado | `{ usuario, rol }` del token; nunca devuelve el correo |
| PUT | `/auth/me/contrasena` | autenticado | `{ contrasenaActual, contrasenaNueva }` → `{ token, usuario, rol }`. Cierra todas las sesiones (suma 1 a `version_sesion`) y devuelve un token nuevo para seguir dentro; anula un enlace de recuperación pendiente. 400 si la actual es incorrecta (no 401, para no parecer sesión vencida), si la nueva es igual a la actual o no tiene de 6 a 64 caracteres |
| POST | `/auth/logout` | autenticado | 204; suma 1 a `version_sesion`, así que todos los tokens anteriores dejan de valer |
| GET | `/admin/usuarios` | ADMINISTRADOR | `[{ id, usuario, correo, rol, estado, fechaRegistro }]`, los más antiguos primero. `?buscar=` filtra por usuario o correo y `?rol=` por rol; sin filtros devuelve todos. `estado` es `ACTIVA` o `DESACTIVADA`. Es el único lugar donde se ve el correo. Un rol desconocido da 400 |
| PUT | `/admin/usuarios/{id}/rol` | ADMINISTRADOR | `{ rol }` → el usuario. Cierra las sesiones de esa persona; si el rol es el mismo no hace nada. 403 si es la propia cuenta; 400 si el valor no es un rol; 404 |
| POST | `/admin/usuarios/{id}/desactivar` | ADMINISTRADOR | La cuenta queda `DESACTIVADA`, se cierran sus sesiones y el login responde 403; conserva su historial. 403 si es la propia cuenta; 404; 409 si ya está desactivada |
| POST | `/admin/usuarios/{id}/reactivar` | ADMINISTRADOR | La cuenta vuelve a `ACTIVA`; las sesiones anteriores no reviven. No está en `docs/API.md`. 404; 409 si ya está activa |
| GET | `/notificaciones` | autenticado | Mi bandeja `[{ id, incidenciaId, tipo, titulo, mensaje, leida, fecha }]`, las más nuevas primero. `tipo` y `titulo` son campos extra del contrato. Cada persona ve solo las suyas, con cualquier rol |
| GET | `/notificaciones/no-leidas` | autenticado | `{ total }`: cuántas notificaciones sin leer tengo (el número de la campanita). Cuenta solo las mías; sin avisos devuelve 0 |
| PATCH | `/notificaciones/{id}/leida` | autenticado | 204. Marcar dos veces no cambia la fecha. 404 si no existe **o es de otra persona** (no se revela cuál) |
| PATCH | `/notificaciones/leidas[?hasta=]` | autenticado | 204. Marca mis no leídas. Con `?hasta=` (la `fecha` del aviso más nuevo que se mostró, tal cual la devuelve la API) solo las creadas hasta ese momento, para que lo que llegó mientras miraba el panel siga sin leer; sin él, todas. Una fecha inválida da 400 |
| POST | `/admin/cursos` | ADMINISTRADOR | Crea un curso `{ codigo, nombre, ciclo? }`. `ciclo` es 1 a 10 o `null` para Electivos. El id y `creadoEn` los genera el sistema. 201; 409 si el código ya existe |
| GET | `/admin/cursos` | ADMINISTRADOR | Lista todos los cursos con `ciclo`, ordenados por código; con `?nombre=` filtra por parte del nombre (sin importar mayúsculas ni tildes) |
| GET | `/admin/cursos/{id}` | ADMINISTRADOR | Busca un curso por id; 404 si no existe |
| PUT | `/admin/cursos/{id}` | ADMINISTRADOR | Modifica código y nombre; un ciclo numérico actualiza el ciclo y su omisión conserva el valor anterior (el id no cambia); 409 si el código es de otro curso |
| DELETE | `/admin/cursos/{id}` | ADMINISTRADOR | Elimina el curso; 204; 409 si tiene canales |
| GET | `/periodos` | DIRECTOR, ADMINISTRADOR | Periodos con canales registrados |
| GET | `/canales`, `/canales/{id}` | ESTUDIANTE, PROFESOR | Canales con suscripción y contadores del usuario |
| POST, DELETE | `/canales/{id}/suscripcion` | ESTUDIANTE, PROFESOR | Suscribirse o cancelar la suscripción |
| GET, POST | `/admin/canales` | ADMINISTRADOR | Listar o crear canales por curso y periodo |
| POST | `/admin/canales/{id}/desactivar` | ADMINISTRADOR | Desactiva un canal |
| GET | `/categorias`, `/comentarios-predefinidos` | autenticado | Catálogos de la base de datos |
| GET | `/motivos-rechazo` | DIRECTOR | Motivos disponibles para rechazar |
| GET | `/feed`, `/feed/destacadas`, `/canales/{id}/incidencias` | ESTUDIANTE, PROFESOR | Incidencias visibles de los canales suscritos |
| GET | `/mis-incidencias` | ESTUDIANTE | Incidencias propias en cualquier estado |
| GET | `/incidencias/{id}` | autenticado | Detalle según rol y visibilidad |
| POST | `/incidencias` | ESTUDIANTE | Registra una incidencia y su primera versión |
| PUT | `/incidencias/{id}` | ESTUDIANTE | Edita una incidencia propia pendiente u observada |
| POST | `/incidencias/{id}/reenviar` | ESTUDIANTE | Reenvía una observada después de editar |
| POST | `/incidencias/{id}/confirmacion`, `/incidencias/{id}/comentarios` | ESTUDIANTE | Confirma o comenta una incidencia publicada |
| PUT | `/incidencias/{id}/reaccion` | ESTUDIANTE | Agrega, cambia o quita la reacción |
| GET | `/director/incidencias`, `/director/estadisticas` | DIRECTOR | Bandeja y estadísticas de incidencias |
| POST | `/director/incidencias/{id}/{accion}` | DIRECTOR | `aprobar`, `observar`, `rechazar`, `atender` o `resolver` |
| GET | `/admin/reportes`, `/admin/reportes/{id}` | ADMINISTRADOR | Reportes de incidencias |
| GET | `/admin/auditoria` | ADMINISTRADOR | Eventos de auditoría (módulo `auditoria`) |

## Datos de demostración (solo local)

`src/main/resources/db/dev/R__datos_demo.sql` crea cuentas, un curso, un canal y tres incidencias para probar todos los
roles. Flyway solo lo ve si se lo pides; las pruebas, el CI y RDS no lo cargan. Es repetible: se puede volver a correr sin
duplicar nada.

```powershell
$env:FLYWAY_LOCATIONS = "classpath:db/migration,classpath:db/dev"
.\mvnw.cmd spring-boot:run
```

| Usuario | Rol |
| --- | --- |
| `admin` | ADMINISTRADOR |
| `director` | DIRECTOR |
| `profesor` | PROFESOR |
| `ana`, `luis` | ESTUDIANTE |

Contraseña de todas: `Prueba123`. `ana` tiene una incidencia pendiente, una publicada (confirmada por `luis`) y una
rechazada, en el canal INF263 2026-2.

## Formato y pruebas

- `.\mvnw.cmd spotless:apply` formatea el código; `verify` falla si algo no está formateado.
- `.\mvnw.cmd test`: solo pruebas unitarias (`*Tests`), sin base de datos.
- `.\mvnw.cmd verify`: formato + unitarias + integración (`*IT`) contra MySQL local `hellofriend_test`. Es lo que corre el CI.
- Una IT nueva extiende `PruebaIntegracion`. Para comprobar una regla de la base usa `rechazaPor("nombre_de_la_restriccion", () -> ...)`.

# Backend de HelloFriend

API REST en **Java 21 + Spring Boot 4.1** con **MySQL 8.0**, Flyway, Spring Data JPA y Maven Wrapper. Formato con
Spotless; pruebas unitarias con Surefire (`*Tests`) y de integración con Failsafe (`*IT`).

> Estado: están el catálogo de cursos y el módulo `usuarios` (registro con aprobación del administrador, login con JWT,
> cierre de sesión, recuperación y cambio de contraseña, protección por rol). Diseño y decisiones pendientes:
> [`docs/decisiones/2026-10-27-usuarios-y-seguridad.md`](../docs/decisiones/2026-10-27-usuarios-y-seguridad.md).
> **Falta acordar con el frontend (`docs/API.md`, en `origin/front/setup`):** rol `ADMIN` frente a `ADMINISTRADOR`, ids
> numéricos frente a UUID, y los endpoints que el contrato aún no incluye (`/auth/me`, `/auth/logout`, `/auth/recuperar`,
> `/auth/restablecer`, `/auth/me/contrasena`).

## Cómo arrancar

1. MySQL local con las bases `hellofriend` y `hellofriend_test` (ver [`docs/guias/base-de-datos.md`](../docs/guias/base-de-datos.md)).
2. Copia `.env.example` como `.env` y completa `DB_PASSWORD` (`.env` está ignorado por Git).
3. `.\mvnw.cmd spring-boot:run` arranca la API en <http://localhost:8080/api> (salud: `/api/actuator/health`).

## Configuración

Todo sale de variables de entorno (o de `backend/.env`, que las variables reales del entorno sobrescriben).

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/hellofriend?...` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la base | `hellofriend` / vacío |
| `TEST_DB_URL` | Base de las pruebas de integración (debe ser local y terminar en `_test`) | `.../hellofriend_test?...` |
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
| `V3__usuarios_estado_cuenta.sql` | Columna `estado` (`PENDIENTE`, `ACTIVA`, `RECHAZADA`; por defecto `ACTIVA`) en `usuarios_usuario` |

Datos de prueba (usuarios demo con clave `Prueba#2026`, canales e incidencias en cada estado): `db/datos-prueba/R__datos_prueba.sql`. Solo se cargan si defines `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/datos-prueba` en tu `.env` local o en un servidor de pruebas; nunca en producción. Las pruebas de integración no los cargan.

## API

Prefijo `/api`. Errores en JSON `{ "mensaje": "..." }` (formato de `docs/API.md`). Todo exige `Authorization: Bearer <token>`
salvo lo marcado como público. La columna Rol indica el rol exigido con `@PreAuthorize`: sin token responde 401 y con otro
rol, 403 (`{ "mensaje": "No tienes permiso para realizar esta acción." }`).

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/actuator/health` | público | Estado de la aplicación |
| POST | `/auth/registro` | público | `{ correo, usuario, password }` → 201 `{ mensaje }`. La cuenta queda `PENDIENTE` como ESTUDIANTE. 400 si el correo no es `@pucp.edu.pe`, el usuario no cumple `^[a-zA-Z0-9._-]{3,15}$` o la contraseña no tiene de 6 a 64 caracteres; 409 si el usuario o el correo ya existen |
| POST | `/auth/login` | público | `{ usuario, password }` → `{ token, usuario, rol }`. 401 si las credenciales fallan (mismo mensaje exista o no el usuario); 403 si la cuenta está `PENDIENTE`, `RECHAZADA` o desactivada |
| POST | `/auth/recuperar` | público | `{ correo }` → 200 `{ mensaje }` siempre, exista o no el correo. Si es de una cuenta que puede entrar, envía un enlace `FRONTEND_URL/restablecer?token=...` (vence en `RECUPERACION_MINUTOS`, 30 por defecto). Pedir otro invalida el anterior |
| POST | `/auth/restablecer` | público | `{ token, password }` → 200 `{ mensaje }`. Cambia la contraseña, consume el enlace (un solo uso) y cierra todas las sesiones. 400 si el enlace es desconocido, vencido o usado (mismo mensaje) o si la contraseña no tiene de 6 a 64 caracteres |
| GET | `/auth/me` | autenticado | `{ usuario, rol }` del token; nunca devuelve el correo |
| PUT | `/auth/me/contrasena` | autenticado | `{ contrasenaActual, contrasenaNueva }` → `{ token, usuario, rol }`. Cierra todas las sesiones (suma 1 a `version_sesion`) y devuelve un token nuevo para seguir dentro; anula un enlace de recuperación pendiente. 400 si la actual es incorrecta (no 401, para no parecer sesión vencida), si la nueva es igual a la actual o no tiene de 6 a 64 caracteres |
| POST | `/auth/logout` | autenticado | 204; suma 1 a `version_sesion`, así que todos los tokens anteriores dejan de valer |
| GET | `/admin/usuarios` | ADMINISTRADOR | `[{ id, usuario, rol, estado, activo, fechaRegistro }]`, los más antiguos primero, sin correo. Sin `?rol=` lista estudiantes (contrato); con `?rol=PROFESOR` etc., ese rol. `?estado=PENDIENTE\|ACTIVA\|RECHAZADA` filtra; un valor desconocido da 400 |
| POST | `/admin/usuarios/{id}/aprobar` | ADMINISTRADOR | `PENDIENTE` → `ACTIVA`; devuelve el usuario. 404 si no existe; 409 si ya no está pendiente |
| POST | `/admin/usuarios/{id}/rechazar` | ADMINISTRADOR | `PENDIENTE` → `RECHAZADA`; mismos errores |
| PUT | `/admin/usuarios/{id}/rol` | ADMINISTRADOR | `{ rol }` → el usuario. Solo cuentas aprobadas; cierra las sesiones de esa persona; mismo rol no hace nada. 400 si es el propio rol o el valor no es un rol; 404; 409 si la cuenta no está aprobada |
| POST | `/admin/usuarios/{id}/desactivar` | ADMINISTRADOR | `activo` pasa a `false` y se cierran sus sesiones; el login responde 403. 400 si es la propia cuenta; 404; 409 si ya está desactivada o no está aprobada |
| POST | `/admin/usuarios/{id}/reactivar` | ADMINISTRADOR | `activo` pasa a `true`; las sesiones anteriores no reviven. 404; 409 si ya está activa o no está aprobada |
| POST | `/admin/cursos` | ADMINISTRADOR | Crea un curso `{ codigo, nombre }`. El id y `creadoEn` los genera el sistema. 201; 409 si el código ya existe |
| GET | `/admin/cursos` | ADMINISTRADOR | Lista todos los cursos por código; con `?nombre=` filtra por parte del nombre (sin importar mayúsculas ni tildes) |
| GET | `/admin/cursos/{id}` | ADMINISTRADOR | Busca un curso por id; 404 si no existe |
| PUT | `/admin/cursos/{id}` | ADMINISTRADOR | Modifica código y nombre (el id no cambia); 409 si el código es de otro curso |
| DELETE | `/admin/cursos/{id}` | ADMINISTRADOR | Elimina el curso; 204; 409 si tiene canales |

## Formato y pruebas

- `.\mvnw.cmd spotless:apply` formatea el código; `verify` falla si algo no está formateado.
- `.\mvnw.cmd test`: solo pruebas unitarias (`*Tests`), sin base de datos.
- `.\mvnw.cmd verify`: formato + unitarias + integración (`*IT`) contra MySQL local `hellofriend_test`. Es lo que corre el CI.
- Una IT nueva extiende `PruebaIntegracion`. Para comprobar una regla de la base usa `rechazaPor("nombre_de_la_restriccion", () -> ...)`.

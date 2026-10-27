# Backend de HelloFriend

API REST en **Java 21 + Spring Boot 4.1** con **MySQL 8.0**, Flyway, Spring Data JPA y Maven Wrapper. Formato con
Spotless; pruebas unitarias con Surefire (`*Tests`) y de integración con Failsafe (`*IT`).

> Estado: es solo el esqueleto (aplicación, reloj, configuración, migración `V1` y pruebas). No hay Spring Security ni
> JWT todavía.
> **Siguiente tarea:** implementar el módulo `usuarios` (registro, login con JWT, roles). Antes, alinear con el
> frontend `docs/API.md` (en `origin/front/setup`), que hoy difiere de las historias de usuario: rol `ADMIN` frente a
> `ADMINISTRADOR`, cuentas `PENDIENTE/ACTIVA/RECHAZADA` aprobadas por el admin, ids numéricos, error `{ "mensaje" }`.

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
| `JWT_SECRET` | Secreto de los tokens (aún sin uso; mínimo 32 caracteres) | valor de desarrollo |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` / `MAIL_FROM` | Correo (en local, Mailpit) | `localhost` / `1025` |

Nunca se usan credenciales de RDS en un equipo de desarrollo ni en el CI.

## Estructura

```
src/main/java/com/hellofriend/
├── HelloFriendAplicacion.java
├── compartido/            # reloj (Clock America/Lima) y, más adelante, seguridad, errores y paginación
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

Datos de prueba (usuarios demo con clave `Prueba#2026`, canales e incidencias en cada estado): `db/datos-prueba/R__datos_prueba.sql`. Solo se cargan si defines `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/datos-prueba` en tu `.env` local o en un servidor de pruebas; nunca en producción. Las pruebas de integración no los cargan.

## API

Prefijo `/api`. Errores en JSON `{ "mensaje": "..." }` (formato de `docs/API.md`). Todo exige `Authorization: Bearer <token>`
salvo lo marcado como público. Los endpoints de `/admin/cursos` aún aceptan a cualquier usuario autenticado: la columna Rol
indica el rol que se exigirá cuando se agregue `@PreAuthorize`.

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/actuator/health` | público | Estado de la aplicación |
| POST | `/auth/login` | público | `{ usuario, password }` → `{ token, usuario, rol }`. 401 si las credenciales fallan (mismo mensaje exista o no el usuario); 403 si la cuenta no está activa |
| GET | `/auth/me` | autenticado | `{ usuario, rol }` del token; nunca devuelve el correo |
| POST | `/auth/logout` | autenticado | 204; suma 1 a `version_sesion`, así que todos los tokens anteriores dejan de valer |
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

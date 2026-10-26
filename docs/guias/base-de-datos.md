# Base de datos (MySQL 8.0 y Flyway)

La base real del proyecto es **MySQL en Amazon RDS** (versión por confirmar con `SELECT VERSION();`; asumimos 8.0).
**Nadie desarrolla ni prueba contra RDS**: cada persona usa su MySQL local y el CI usa uno desechable. Local y CI deben
tener la misma versión mayor (8.0).

## 1. MySQL local

Necesitas dos bases (`hellofriend` y `hellofriend_test`) y un usuario `hellofriend` con acceso a ambas.

### Con un MySQL 8.0 ya instalado (puerto 3306)

Desde un cliente con un usuario administrador (reemplaza la clave por una tuya; no la subas al repo):

```sql
CREATE DATABASE hellofriend      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE hellofriend_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'hellofriend'@'localhost' IDENTIFIED BY '<tu-clave-local>';
GRANT ALL PRIVILEGES ON hellofriend.*      TO 'hellofriend'@'localhost';
GRANT ALL PRIVILEGES ON hellofriend_test.* TO 'hellofriend'@'localhost';
```

Luego copia `backend/.env.example` como `backend/.env` y completa `DB_PASSWORD`.

### Con Docker (opcional)

> **Sin verificar en esta máquina:** `deploy/docker-compose.local.yml` y `deploy/mysql/init.sql` se escribieron en un
> equipo sin Docker. Pruébalos la primera vez y corrige lo que haga falta.

```powershell
docker compose --env-file backend/.env -f deploy/docker-compose.local.yml up -d
```

Levanta MySQL en `localhost:3307` (con ambas bases ya creadas) y Mailpit (correo de prueba en <http://localhost:8025>).
Con este MySQL, usa el puerto `3307` en `DB_URL` y `TEST_DB_URL` de tu `.env`.
`down -v` borra los datos locales.

## 2. Flyway: dos formas de aplicar migraciones

| Forma | Cuándo usarla | Comando | ¿Necesita `FLYWAY_*`? |
| --- | --- | --- | --- |
| **Automática** | Día a día: las migraciones pendientes se aplican al arrancar la app y al correr las pruebas | `.\mvnw.cmd spring-boot:run` / `.\mvnw.cmd verify` | No (usa `DB_*` / `TEST_DB_*` de tu `.env`) |
| **A mano (plugin de Maven)** | Ver el estado, validar o aplicar migraciones sin levantar la app | `.\mvnw.cmd flyway:info`, `flyway:validate`, `flyway:migrate` | Sí, en tu terminal |

El plugin no arranca Spring, por eso no lee `application.properties` ni `.env`. Define la conexión en tu terminal
(valores de tu base **local**; la contraseña no se escribe en ningún archivo):

```powershell
cd backend
$env:FLYWAY_URL="jdbc:mysql://localhost:3306/hellofriend?characterEncoding=UTF-8&connectionTimeZone=UTC&allowPublicKeyRetrieval=true"
$env:FLYWAY_USER="hellofriend"
$env:FLYWAY_PASSWORD="<tu-clave-local>"
.\mvnw.cmd flyway:info
```

Si faltan estas variables el plugin falla en vez de conectarse a otro lado.

> **Cuidado con `flyway:clean`.** Borra TODAS las tablas de la base a la que apunte `FLYWAY_URL`. Flyway lo trae
> bloqueado por defecto; solo es válido para tu base local, nunca para una base compartida ni RDS.

Si una migración falla a medias, MySQL no deshace el DDL (ver "Migraciones pequeñas"). En tu base local corrige el
problema y usa `flyway:repair`, o recrea la base.

## 3. Cómo se escriben las migraciones

- Archivo `V{n}__{modulo}_{tema}.sql` en `backend/src/main/resources/db/migration/`, por ejemplo `V2__usuarios_tokens.sql`.
- **Una migración ya subida (a `develop` o `main`) nunca se edita**: el cambio va en una migración nueva. Flyway guarda
  una huella de cada archivo y falla si cambia.
- **Pequeñas y de un solo tema.** En MySQL el DDL (`CREATE`, `ALTER`) no es transaccional: si falla a la mitad, los
  cambios anteriores de ese archivo quedan aplicados.
- **Numeración entre dos personas:** antes de crear una migración, avisa su número en el chat del equipo (o en un issue
  "Migraciones") y toma el siguiente libre. Si dos PR terminan con el mismo número, quien llegó después renumera su
  archivo antes de integrar (mientras no se haya subido a `develop`).

## 4. Convenciones

| Tema | Regla |
| --- | --- |
| Módulos | **Una sola base** `hellofriend`. Cada tabla lleva el módulo como prefijo: `{modulo}_{entidad}`, en singular y `snake_case` (`usuarios_usuario`, `incidencias_incidencia`). |
| Identificadores | `id CHAR(36) NOT NULL DEFAULT (UUID())`. Se lee fácil al depurar y JPA lo maneja como `String`/`UUID` sin conversores. `BINARY(16)` ocupa menos, pero solo valdría la pena si apareciera un problema de rendimiento medido. |
| Estados y roles | `VARCHAR` + `CHECK` en MAYÚSCULAS (`CHECK` se aplica desde MySQL 8.0.16). No `ENUM`: agregar un valor exige `ALTER` de la columna y la máquina de estados debe poder crecer. |
| Fechas | `DATETIME(6)` guardadas en **UTC**. La zona `America/Lima` se aplica en la aplicación con el bean `Clock` (`RelojConfig`). |
| Caracteres | `utf8mb4` y colación `utf8mb4_0900_ai_ci` (no distingue mayúsculas ni tildes: `Ana` y `ana` chocan en un `UNIQUE`). En la URL JDBC: `characterEncoding=UTF-8` (el conector lo traduce a utf8mb4) y `connectionTimeZone=UTC`. |
| Restricciones | Prefijos `pk_`, `uq_`, `ck_`, `fk_`, `ix_` seguidos de tabla y columna: `uq_usuarios_usuario_correo`. Las pruebas de integración comprueban las reglas por ese nombre. |
| Texto | Identificadores sin tildes ni ñ. |

## 5. Plan de migraciones (solo lista; hoy existe `V1`)

| N.º | Archivo | Tablas |
| --- | --- | --- |
| V1 | `usuarios_cuentas` ✅ | `usuarios_usuario` |
| V2 | `usuarios_tokens` | `usuarios_token_verificacion`, `usuarios_token_recuperacion`, `usuarios_sesion_revocada` (por `jti`) |
| V3 | `academico_cursos_periodos` | `academico_curso`, `academico_periodo` |
| V4 | `canales_canales` | `canales_canal` (único por curso y periodo) |
| V5 | `canales_suscripciones` | `canales_suscripcion` (única por usuario y canal) |
| V6 | `incidencias_categorias` | `incidencias_categoria` |
| V7 | `incidencias_incidencias` | `incidencias_incidencia` (código de seguimiento único y no secuencial; **sin índice FULLTEXT**) |
| V8 | `incidencias_versiones_observaciones` | `incidencias_version`, `incidencias_observacion` |
| V9 | `incidencias_confirmaciones` | `incidencias_confirmacion` (una por usuario e incidencia) |
| V10 | `incidencias_historial_estados` | `incidencias_historial_estado` |
| V11 | `notificaciones_bandeja` | `notificaciones_notificacion` |
| V12 | `auditoria_bitacora` | `auditoria_evento` (**sin triggers**) |

Las tablas y el orden pueden cambiar al implementar cada módulo, y el contrato que espera el frontend (`docs/API.md`)
puede mover algunas (por ejemplo, estados de cuenta o comentarios y reacciones). Lo que se sube a `develop` ya no se edita.

### Incidencias similares (RF-08.5): se calculan en la aplicación

No se usa `FULLTEXT` de MySQL. La aplicación normaliza el texto (minúsculas, sin tildes, sin palabras vacías) y compara
palabras con las incidencias aprobadas del mismo canal, que son pocas. Así no dependemos de la configuración de
`FULLTEXT` (palabras vacías en inglés, token mínimo) ni de que los datos estén confirmados para poder buscarlos.

### Auditoría de solo adición (HU-19): por la aplicación y por permisos

Sin triggers. Se garantiza así:
- La aplicación solo hace `INSERT` y `SELECT` sobre `auditoria_evento`: no hay código que la actualice o borre.
- El usuario de BD de la aplicación en RDS tendrá únicamente `INSERT` y `SELECT` sobre esa tabla, sin `UPDATE`,
  `DELETE`, `DROP` ni `TRUNCATE`. Las migraciones las aplica un usuario distinto, con más permisos.

Los triggers `BEFORE UPDATE/DELETE` quedan como mejora opcional: en RDS con binlog activo exigen
`log_bin_trust_function_creators` en el parameter group, que no administramos.

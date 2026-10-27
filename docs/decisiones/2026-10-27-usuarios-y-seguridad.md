# Usuarios y seguridad (migración V3)

- **Autor:** equipo (rama `feat/usuarios-seguridad`)
- **Fecha:** 2026-10-27
- **Tarea:** registro con aprobación, login con JWT, cierre de sesión, recuperación y cambio de contraseña, y protección por rol
- **Estado:** implementado en la rama; pendiente de revisión del equipo
- **Verificación:** `.\mvnw.cmd verify` en verde (135 pruebas: 17 unitarias y 118 de integración); nada se conectó a RDS

## Decisiones

- **JWT sin filtro propio.** Se usa `spring-boot-starter-oauth2-resource-server` (mismo enfoque que GalilEdu): un `JwtDecoder`
  HS256 valida firma, emisor y expiración, y `ServicioJwt` emite el token. Claims: `sub` (id), `usuario`, `rol` (los tres que
  pide `docs/API.md`), `ver` (versión de sesión), `iss`, `iat`, `exp`. Dura 8 horas (`JWT_HORAS`). No hay *refresh token*.
- **Revocación con `version_sesion`.** El decoder, en cada petición, comprueba que la cuenta pueda entrar y que `ver` sea igual
  a `usuarios_usuario.version_sesion`. Suman 1 a la versión: cerrar sesión, cambiar o restablecer la contraseña, desactivar
  la cuenta y cambiar el rol. Cuesta una lectura por clave primaria por petición.
- **`VerificadorSesion` es una interfaz en `compartido/seguridad`** y la implementa `usuarios`, para que la seguridad común no
  dependa de las entidades de un módulo.
- **Contraseñas con BCrypt (costo 12).** Largo de 6 a 64 caracteres y tope de 72 bytes (BCrypt ignora el resto). Los DTO con
  secretos sobrescriben `toString()` y ningún log ni respuesta los incluye.
- **Login por nombre de usuario, no por correo.** Credenciales incorrectas y usuario inexistente dan el mismo 401 (se compara
  contra un hash ficticio para que tarden lo mismo). El estado de la cuenta (403) solo se revela a quien acierta la clave.
- **V3: `usuarios_usuario.estado`** (`PENDIENTE`, `ACTIVA`, `RECHAZADA`, por defecto `ACTIVA`). El registro público crea la cuenta
  `PENDIENTE` como ESTUDIANTE; el administrador la aprueba o rechaza (solo desde `PENDIENTE`) sin ver el correo. Se conserva
  `activo` para desactivar una cuenta ya aprobada; para entrar hacen falta estado `ACTIVA` y `activo`.
- **Recuperación de contraseña.** Token de 256 bits (`SecureRandom`); en la base solo su SHA-256, vence a los 30 minutos
  (`RECUPERACION_MINUTOS`) y se consume al usarlo. `/auth/recuperar` responde siempre 200 y solo envía correo a cuentas que
  pueden entrar. El correo sale en segundo plano (`@Async`) y no se escribe el token en el log. Restablecer cierra todas las
  sesiones.
- **Cambio de contraseña con sesión.** Pide la actual (si falla, 400 y no 401, para no parecer sesión vencida), cierra todas las
  sesiones y devuelve un token nuevo. También anula un enlace de recuperación pendiente.
- **Administración de cuentas.** El administrador puede cambiar el rol, desactivar y reactivar cuentas aprobadas (`/admin/usuarios`).
  Cambiar el rol o desactivar cierra las sesiones de esa persona. Nadie puede cambiar su propio rol ni desactivarse, así siempre
  queda al menos un administrador que pueda entrar. `GET /admin/usuarios` acepta `?rol=` (sin él, estudiantes, como el contrato) y
  devuelve `activo`, un campo extra respecto de `docs/API.md`.
- **Rol del administrador: se mantiene `ADMINISTRADOR`** (es el valor del CHECK de V1); `docs/API.md` dice `ADMIN`.
- **401 y 403 salen como `{ "mensaje": "..." }` desde la cadena de seguridad**, no desde `ManejadorErrores`, porque ocurren antes
  de Spring MVC.

## A coordinar con el equipo

- Avisar que `V3` está tomada y que las siguientes migraciones empiezan en `V4`.
- Frontend: acordar `ADMIN` frente a `ADMINISTRADOR`, ids numéricos frente a UUID, y agregar a `docs/API.md` los endpoints que
  faltan (`/auth/me`, `/auth/logout`, `/auth/recuperar`, `/auth/restablecer`, `/auth/me/contrasena`) y la ruta
  `/restablecer?token=...` de la pantalla de recuperación.
- Las fechas de la API salen en UTC (sin zona), igual que `/admin/cursos`; `docs/API.md` muestra ISO-8601 sin offset.
- Aplicar la V3 en RDS es un paso aparte (ver `2026-10-26-equipo-despliegue-pendiente.md`).

## Riesgos conocidos y pendiente

- **`JWT_SECRET` tiene un valor por defecto de desarrollo.** Si un despliegue olvida definirlo, arranca con un secreto público y
  cualquiera puede fabricar un token de administrador. Conviene exigirlo fuera de local (fallar al arrancar si falta).
- **Sin límite de intentos ni de frecuencia** en login, cambio de contraseña y recuperación. Si se quiere bloqueo por cuenta, hace
  falta una migración (`V4`, columnas de intentos y bloqueo).
- **Cerrar sesión o cambiar la contraseña cierra todos los dispositivos**, porque hay una sola versión por usuario.
- **El registro devuelve 409 si el usuario o el correo ya existen**, lo que confirma que existen. Lo pide el contrato.
- Los datos demo (`R__datos_prueba.sql`) entran con `Prueba#2026`; `DatosPruebaTests` comprueba que el hash coincida.

# Usuarios y seguridad (migración V3)

- **Autor:** equipo (rama `feat/usuarios-seguridad`)
- **Fecha:** 2026-10-27
- **Tarea:** registro libre, login con JWT, cierre de sesión, recuperación y cambio de contraseña, y protección por rol
- **Estado:** implementado en la rama; pendiente de revisión del equipo
- **Verificación:** `.\mvnw.cmd verify` en verde (152 pruebas: 33 unitarias y 119 de integración); nada se conectó a RDS

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
- **Registro libre (RF-01.4).** El registro público crea la cuenta `ACTIVA` como ESTUDIANTE, sin aprobación. Primero se implementó
  con aprobación del administrador (V3, `usuarios_usuario.estado` con `PENDIENTE`, `ACTIVA`, `RECHAZADA`), pero `docs/API.md`
  (PR #5 de `front/setup`, 2026-10-27) pasó a registro libre; se alineó el backend y se quitaron `/aprobar` y `/rechazar`.
  **La columna `estado` de la V3 quedó sin uso:** la entidad no la mapea y la base la rellena con `ACTIVA`. No se editó la V3
  (ya está en la rama subida); si se quiere quitar, hace falta una migración nueva (`V4`). La API expone `estado` (`ACTIVA` o
  `DESACTIVADA`), que se deduce de `activo`.
- **Recuperación de contraseña.** Token de 256 bits (`SecureRandom`); en la base solo su SHA-256, vence a los 30 minutos
  (`RECUPERACION_MINUTOS`) y se consume al usarlo. `/auth/recuperar` responde siempre 200 y solo envía correo a cuentas que
  pueden entrar. El correo sale en segundo plano (`@Async`) y no se escribe el token en el log. Restablecer cierra todas las
  sesiones.
- **Cambio de contraseña con sesión.** Pide la actual (si falla, 400 y no 401, para no parecer sesión vencida), cierra todas las
  sesiones y devuelve un token nuevo. También anula un enlace de recuperación pendiente.
- **Administración de cuentas.** El administrador lista, cambia el rol, desactiva y reactiva cuentas (`/admin/usuarios`). Cambiar el
  rol o desactivar cierra las sesiones de esa persona (el contrato dice que el rol se aplica en el siguiente inicio de sesión;
  aquí además el token viejo deja de valer). Nadie puede cambiar su propio rol ni desactivarse (403, como el contrato), así
  siempre queda al menos un administrador que pueda entrar. `GET /admin/usuarios?buscar=&rol=` devuelve el correo, que solo el
  ADMIN ve; `buscar` escapa los comodines de LIKE. `/reactivar` no está en el contrato: es un añadido.
- **Rol del administrador: se mantiene `ADMINISTRADOR`** (es el valor del CHECK de V1); `docs/API.md` dice `ADMIN`.
- **`JWT_SECRET` obligatorio fuera de local.** `application.properties` conserva un valor de desarrollo para no pedir configuración
  en tu PC, pero ese valor es público. `ValidadorSecretoJwt` impide arrancar si se usa y la base de datos no es local (`localhost`,
  `127.0.0.1` o `::1`); en producción, donde la base es RDS, hay que definir `JWT_SECRET` en `deploy/.env`. El mensaje de error
  no incluye el secreto. Criterio: "local" se decide por el host de la base, igual que en `PruebaIntegracion`.
- **401 y 403 salen como `{ "mensaje": "..." }` desde la cadena de seguridad**, no desde `ManejadorErrores`, porque ocurren antes
  de Spring MVC.

## Estado de la integración (2026-10-27)

| Qué | Dónde | Estado |
| --- | --- | --- |
| Base del backend (CI, V1 y V2) | PR #7, de `chore/ci-backend` a `develop` | **Integrado** el 2026-10-27, con "Merge commit". Incluye el trabajo de royal-22 (V2) |
| Backend de usuarios, seguridad y notificaciones | PR #8, de `feat/usuarios-seguridad` a `develop` | **Integrado** el 2026-10-27, con "Merge commit" |
| Endpoints nuevos documentados en el contrato | PR #6, de `docs/api-usuarios-auth` a `front/setup` | **Integrado** el 2026-10-27 |
| Frontend (`front/setup`) | PR hacia `develop`, lo abre quien lleva el frontend | **Por abrir.** Habrá un conflicto trivial en `.gitignore` (ambas ramas lo crearon): se resuelve juntando las líneas |
| Rol del administrador: `ADMIN` (contrato) o `ADMINISTRADOR` (backend y V1) | — | **Sin decidir.** Hoy el login devuelve `ADMINISTRADOR`; el frontend debe leer ese valor o se cambia el backend |
| Ids: numéricos (contrato) o UUID (backend) | — | **Sin decidir.** Hoy son UUID de 36 caracteres |
| `develop` → `main` | `main` tiene 3 commits que `develop` no: el PR #3, su revert (`6c4fd3e`) y el PR #4 | **Pendiente.** No unir `main` dentro de `develop` (10 conflictos y borraría archivos). Primero un PR a `main` que revierta `6c4fd3e`; luego el PR `develop` → `main`, que sale sin conflictos (probado en una copia aparte) |

## A coordinar con el equipo

- Avisar que `V3` está tomada y que las siguientes migraciones empiezan en `V4`.
- Frontend: acordar `ADMIN` frente a `ADMINISTRADOR` y ids numéricos frente a UUID. Los endpoints `/auth/me`, `/auth/logout`,
  `/auth/recuperar`, `/auth/restablecer`, `/auth/me/contrasena` y `/admin/usuarios/{id}/reactivar`, y la ruta
  `/restablecer?token=...` de la pantalla de recuperación, ya están en `docs/API.md` de `front/setup` (PR #6).
- Las fechas de la API salen en UTC (sin zona), igual que `/admin/cursos`; `docs/API.md` muestra ISO-8601 sin offset.
- Aplicar la V3 en RDS es un paso aparte (ver `2026-10-26-equipo-despliegue-pendiente.md`).

## Riesgos conocidos y pendiente

- **Sin límite de intentos ni de frecuencia** en login, cambio de contraseña y recuperación. Si se quiere bloqueo por cuenta, hace
  falta una migración (`V4`, columnas de intentos y bloqueo).
- **Cerrar sesión o cambiar la contraseña cierra todos los dispositivos**, porque hay una sola versión por usuario.
- **El registro devuelve 409 si el usuario o el correo ya existen**, lo que confirma que existen. Lo pide el contrato.
- Los datos demo (`R__datos_prueba.sql`) entran con `Prueba#2026`; `DatosPruebaTests` comprueba que el hash coincida.

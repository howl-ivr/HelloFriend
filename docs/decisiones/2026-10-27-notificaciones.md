# Notificaciones: bandeja interna

- **Autor:** equipo (rama `feat/usuarios-seguridad`)
- **Fecha:** 2026-10-27
- **Tarea:** módulo `notificaciones`: bandeja de avisos de cada persona
- **Estado:** implementado en la rama; pendiente de revisión del equipo
- **Verificación:** `.\mvnw.cmd verify` en verde (182 pruebas: 38 unitarias y 144 de integración); nada se conectó a RDS

## Decisiones

- **Solo bandeja interna, sin correo.** Las notificaciones se guardan en `notificaciones_notificacion` (V2) y se leen con
  `GET /notificaciones`. No se envía ningún email: ni el contrato (`docs/API.md`) ni la tabla lo prevén, y el correo de los
  usuarios solo lo ve el administrador. Si se quisiera avisar por correo, es una decisión nueva (riesgo para el anonimato).
- **Tres endpoints del contrato, para cualquier rol con token:** listar, marcar una como leída y marcar todas. Cada persona ve
  y marca solo las suyas.
- **Una notificación ajena se trata como inexistente (404)**, para no revelar que existe.
- **Marcar como leída es idempotente:** si ya estaba leída no cambia la fecha de lectura.
- **Contador de no leídas:** `GET /notificaciones/no-leidas` devuelve `{ total }`, para el número de la campanita (no está en el
  contrato; hay que anotarlo en `docs/API.md`).
- **Flujo del panel, como en LinkedIn.** Al abrir el panel el frontend pide `GET /notificaciones` y resalta las de `leida: false`;
  al terminar de mirar llama a `PATCH /notificaciones/leidas?hasta=<fecha del aviso más nuevo mostrado>`. El backend no marca nada
  al listar. `hasta` evita perder avisos: lo que llegó mientras la persona miraba el panel sigue sin leer y cuenta en la campanita.
  Sin `hasta` marca todas (lo que define el contrato). La `fecha` que devuelve la API sirve tal cual como `hasta`.
- **Campos extra en la respuesta:** además de `id`, `incidenciaId`, `mensaje`, `leida` y `fecha`, se devuelven `tipo` y `titulo`.
  Son compatibles con el contrato; falta anotarlos en `docs/API.md` (rama del frontend).
- **`EmisorNotificaciones` es la puerta para los demás módulos.** El módulo de incidencias lo llamará al aprobar, observar, rechazar,
  reenviar, poner en atención o resolver. Se ejecuta dentro de la transacción de quien lo llama: el aviso y el cambio que lo origina
  se guardan juntos o ninguno. Crea un aviso por destinatario, ignora repetidos y vacíos, y recorta con "…" los textos que no cabrían
  (título 150, mensaje 500).
- **Los ids de usuario, incidencia y canal son texto**, no entidades: un módulo no usa las entidades de otro.
- **Anonimato (RF-17.3):** el título y el mensaje nunca deben incluir al autor de la incidencia ni al profesor. El emisor no puede
  comprobarlo; lo debe cumplir quien escribe el texto, y el módulo de incidencias lo probará.

## A coordinar con el equipo

- Anotar en `docs/API.md` (rama `front/setup` o `docs/api-usuarios-auth`): `tipo` y `titulo` en `GET /notificaciones`, el
  endpoint `GET /notificaciones/no-leidas` y el parámetro opcional `?hasta=` de `PATCH /notificaciones/leidas`.
- El contrato muestra ids numéricos y aquí son UUID (ver `2026-10-27-usuarios-y-seguridad.md`).

## Pendiente

- **Falta un tipo para "incidencia nueva".** El frontend avisa a los directores cuando se crea una incidencia ("Nueva incidencia
  pendiente en…"), pero el CHECK de la tabla solo permite `APROBADA`, `OBSERVADA`, `RECHAZADA`, `REENVIADA`, `EN_ATENCION` y
  `RESUELTA`. Agregarlo requiere una migración nueva (`V4`, quitar y volver a crear el CHECK con el tipo nuevo) que se hará junto con
  el módulo de incidencias, avisando el número antes de crearla. Hasta entonces el enum `TipoNotificacion` no lo incluye.
- **Los avisos con incidencia o canal no se prueban todavía:** las FK exigen filas reales de esos módulos. Se cubrirán cuando existan.
- Los avisos antiguos no se borran ni se archivan; si la bandeja crece, hará falta paginarla.

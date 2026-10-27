# Modelo de datos de incidencias (migración V2)

- **Autor:** equipo (rama `feat/bd-modelo-incidencias`)
- **Fecha:** 2026-10-26
- **Tarea:** esquema de cursos, canales, incidencias, notificaciones y auditoría
- **Estado:** implementado en la rama; pendiente de revisión del equipo
- **Verificación:** `.\mvnw.cmd verify` en verde (16 pruebas); `flyway:info` local con V1, V2 y `datos prueba` en `Success`

## Decisiones

- **Una sola migración, `V2__modelo_incidencias.sql`.** Es una excepción a "migraciones pequeñas y de un solo tema"
  (`docs/guias/base-de-datos.md`): como aún no se sube nada a `develop`, si falla a medias se recrea la base local. Desde
  la V3 se vuelve a una migración por tema. El plan de la sección 5 de la guía queda reemplazado por esta V2.
- **El periodo no es una tabla.** Son las columnas `anio` y `semestre` de `canales_canal`; el canal es único por curso y periodo.
  Falta decidir si se admite el ciclo de verano (`semestre = 0`).
- **No se guarda al docente en el canal ni en la incidencia.** Los profesores se relacionan con el canal solo por su suscripción.
- **Recuperación de contraseña y cierre de sesión viven en `usuarios_usuario`:** `token_recuperacion_hash`,
  `token_recuperacion_expira_en` (hash, nunca el token en claro; un solo uso) y `version_sesion` (el JWT la lleva y al cerrar sesión
  se suma 1). No hay tablas de tokens ni de sesiones revocadas.
- **La auditoría es una vista** (`auditoria_historial_incidencia`) sobre versiones, observaciones, confirmaciones y cambios de estado,
  sin tabla propia. Solo la consulta el administrador.
- **Tablas de solo adición** (`incidencias_version`, `incidencias_historial_estado`): se protegen en el servicio, sin triggers.
- **La prioridad se calcula, no se guarda:** sale de `incidencias_incidencia.total_confirmaciones` y de umbrales de configuración.
- **Categorías como datos de referencia en la propia V2** (las necesita también producción). Los datos demo van aparte, en
  `db/datos-prueba/R__datos_prueba.sql`, y solo se cargan con `FLYWAY_LOCATIONS`; las pruebas de integración fijan sus
  ubicaciones y no los cargan.

## A coordinar con el equipo

- Avisar que `V2` está tomada (rama `feat/bd-modelo-incidencias`) y que las siguientes migraciones empiezan en `V3`.
- Frontend: confirmar contra `docs/API.md` (rama `front/setup`) que estados, campos y códigos de seguimiento coinciden.
- Aplicar la V2 en RDS es un paso aparte (ver `2026-10-26-equipo-despliegue-pendiente.md`); aquí nada se conectó a RDS.

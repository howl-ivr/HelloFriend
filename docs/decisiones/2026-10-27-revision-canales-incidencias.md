# Revisión de canales, incidencias y auditoría (rama `codex/integrar-avances`)

Fecha: 2026-10-27

## Contexto

La rama `codex/integrar-avances` trajo 28 endpoints nuevos (canales, incidencias, reportes, auditoría) con el SQL dentro
de los controladores y sin pruebas. Esta revisión los deja con pruebas y con las capas del resto del backend.

## Decisiones

- **Pruebas primero.** Se escribieron pruebas de integración que fijan el comportamiento actual (canales, incidencias,
  interacción, reportes, auditoría) y después se refactorizó con ellas en verde.
- **Capas.** Los controladores solo traducen HTTP. Las reglas están en servicios (`ServicioCanales`,
  `ServicioIncidencias`, `ConsultasIncidencias`) y el SQL en repositorios (`CanalRepositorio`, `IncidenciaRepositorio`).
  Los servicios reciben un `UsuarioActual` (id y roles), no el `Authentication` de Spring.
- **Módulo `auditoria`.** `ServicioAuditoria` es el único que escribe y lee los eventos; los demás módulos lo llaman.
  `GET /admin/auditoria` vive ahí. Se audita el cambio de rol solo cuando el rol cambia realmente.
- **Joins de lectura aceptados.** Las consultas de lectura (feed, reportes, detalle) unen tablas de otros módulos
  (usuarios, cursos). Es una excepción consciente a "un módulo no usa entidades de otro": son solo lecturas, sin
  entidades ni repositorios ajenos. Las escrituras entre módulos pasan por el servicio del dueño.
- **Migración V5.** Agrega el tipo `NUEVA` a las notificaciones para avisar a los directores activos cuando se registra
  una incidencia. La V4 es de CrisGold22; el siguiente número libre es **V6**. El texto no incluye autor (RF-17.3).
- **`DB_USER`.** Vuelve a tener valor por defecto `hellofriend`, como antes de la rama.

## Para quien cambie de rama

La base `hellofriend_test` quedó con V4 y V5 aplicadas. Si vuelves a una rama sin ellas, Flyway rechazará el arranque;
recrea la base de pruebas.

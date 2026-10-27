# Integración del frontend con MySQL

Fecha: 2026-10-27.

El frontend consulta siempre la API Spring Boot. Se elimina el adaptador de datos simulados y las cuentas de demostración del navegador. El token JWT sigue en `localStorage`; los datos de negocio se leen y escriben en MySQL mediante el backend.

Los identificadores expuestos en la API son UUID de texto y el rol administrador se llama `ADMINISTRADOR`, como en la base. El estado almacenado `APROBADA` se presenta como `PUBLICADA` en la API para conservar las pantallas existentes. Las categorías de V2 y los motivos y comentarios de V4 son datos de referencia en la base; los cursos y canales se gestionan desde la pantalla de administración.

V4 agrega comentarios, reacciones, catálogos de interacción y auditoría sin modificar las migraciones anteriores. El backend aplica V4 con Flyway al iniciarse. Las pruebas locales no deben apuntar a RDS, según `AGENTS.md`.

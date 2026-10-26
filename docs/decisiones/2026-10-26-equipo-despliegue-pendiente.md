# Despliegue del backend: plan y puntos abiertos

- **Autor:** equipo
- **Fecha:** 2026-10-26
- **Tarea:** estructura del repo y CI del backend (fase 1); despliegue en fase 2
- **Estado:** por confirmar

## Qué se hizo y qué no

La fase 1 dejó el CI (`.github/workflows/ci.yml`) y la configuración del backend por variables de entorno.
**No existen todavía** `backend/Dockerfile`, el `docker-compose.yml` de producción, `deploy/instalar-servidor.sh`,
`deploy/actualizar.sh`, `.github/workflows/docker.yml` ni `.github/workflows/deploy.yml`. Se hacen en la fase 2.

## Flujo previsto

1. Se integra un PR a `develop`/`main` con el job `Backend` en verde.
2. Un workflow `deploy.yml` se conecta por SSH a un servidor EC2 de AWS.
3. En el servidor, `deploy/actualizar.sh` baja el código y reconstruye con `docker compose`.
4. El `docker-compose.yml` de producción lleva **solo el servicio `backend`**, que se conecta al MySQL de **RDS**
   (no hay base de datos en el servidor).

La configuración ya está lista para esto: el backend lee todo de variables de entorno (`DB_URL`, `DB_USER`,
`DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`, `FLYWAY_LOCATIONS`, `PORT`, `MAIL_*`).

## Puntos abiertos

- **Red:** el EC2 debe estar en la misma cuenta y red (VPC) que el RDS.
- **Grupo de seguridad de RDS:** debe permitir el puerto 3306 desde el EC2 (por el grupo de seguridad del EC2, no abierto a internet).
- **Learner Lab:** apaga el servidor al terminar la sesión y quizá también el RDS; hay que saber qué se conserva
  (disco, IP pública, datos) y cómo se reinicia todo.
- **Versión de MySQL en RDS:** confirmar con `SELECT VERSION();`. El proyecto asume 8.0; local y CI usan `mysql:8.0`.
- **Quién aplica las migraciones en RDS:** (a) la aplicación al arrancar (Flyway automático) o (b) un paso manual con
  `flyway:migrate`. Propuesta: (b) la primera vez y cuando una migración sea delicada, (a) si el equipo prefiere
  simplicidad. Decidir también qué usuario de BD migra: debe tener permisos de DDL; el usuario de la aplicación no.
- **Usuario de BD de la aplicación:** permisos mínimos; en `auditoria_evento` solo `INSERT` y `SELECT`
  (ver `docs/guias/base-de-datos.md`).
- **Secretos en GitHub** (Settings → Secrets and variables → Actions): `SERVIDOR_HOST`, `SERVIDOR_USUARIO`,
  `SERVIDOR_CLAVE_SSH`.
- **`deploy/.env` en el servidor** (no está en el repo): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
  `CORS_ORIGINS`, `MAIL_*` y `FLYWAY_LOCATIONS` si aplica.
- **Frontend:** cómo se sirve (otro servidor, S3 u otro) y su job de CI quedan por definir con la pareja del frontend.

## Por reevaluar

Cuando se conozca la versión de RDS y las restricciones del Learner Lab, revisar este documento antes de escribir los
archivos de despliegue.

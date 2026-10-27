# Despliegue: Vercel (frontend) y una EC2 con Docker (backend + MySQL)

- **Fecha:** 2026-10-27
- **Estado:** alternativa para más adelante. Por ahora el backend corre en una Mac del equipo con ngrok
  (ver "Opción actual" en `docs/guias/despliegue.md`). Reemplaza el plan EC2 + RDS de
  `2026-10-26-equipo-despliegue-pendiente.md`

## Decisión

- El frontend se publica en **Vercel** (raíz `frontend/`).
- El backend y MySQL 8.0 corren en **una sola EC2** (cuenta AWS personal, `t3.small`) con
  `deploy/docker-compose.prod.yml`. MySQL no se expone a internet.
- Vercel reenvía `/api/*` a la EC2 (`frontend/vercel.json`): el navegador solo usa HTTPS contra Vercel, la EC2 no
  necesita dominio ni certificado y no hay CORS entre dominios.

## Por qué

- El Learner Lab apaga las instancias al terminar cada sesión; se necesita que la aplicación siga activa.
- RDS es una segunda máquina que consume crédito y agrega configuración de red. Para el alcance del curso, MySQL en
  la misma EC2 es suficiente.

## Consecuencias

- Los respaldos de la base son responsabilidad del equipo (ver `docs/guias/despliegue.md`).
- La regla "nada se conecta a RDS" sigue vigente; este despliegue no usa RDS.
- El `Dockerfile` del backend no se pudo probar al escribirlo (sin acceso a Maven Central); verificar en el primer
  despliegue.

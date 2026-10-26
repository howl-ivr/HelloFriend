# HelloFriend: instrucciones para asistentes de IA (y personas)

Monorepo con `backend/` (Spring Boot 4, Java 21, MySQL 8.0, Flyway) y `frontend/` (React + Vite). El equipo escribe en
español: responde y documenta en español; identificadores sin tildes ni ñ.

## Antes de cambiar algo

1. Lee [`README.md`](README.md), [`backend/README.md`](backend/README.md) y las guías de `docs/guias/`.
2. Revisa las decisiones en `docs/decisiones/`; no contradigas una sin decirlo.
3. El contrato con el frontend está en `docs/API.md` (rama `front/setup`); si algo no coincide, se acuerda con esa pareja.

## Reglas

- **BD:** una sola base `hellofriend`; tablas `{modulo}_{entidad}`. Solo migraciones **nuevas** en
  `backend/src/main/resources/db/migration/` (`V{n}__{modulo}_{tema}.sql`); nunca se edita una ya subida. Avisa el
  número antes de crearla. Ver [`docs/guias/base-de-datos.md`](docs/guias/base-de-datos.md).
- **Nada se conecta a RDS:** ni comandos, ni pruebas, ni scripts, ni el CI. Local usa MySQL local; el CI, uno desechable.
- **Secretos:** no subas contraseñas ni tokens. La configuración local va en `backend/.env` (ignorado); no lo leas ni
  lo imprimas. Solo `.env.example` se versiona.
- Un módulo no usa entidades ni repositorios de otro. La hora sale del bean `Clock`; las fechas en BD, en UTC.
- Ramas y PR: [`docs/guias/contribuir.md`](docs/guias/contribuir.md). No hagas push sin que te lo pidan.

## Antes de dar algo por terminado

- En `backend/`: `.\mvnw.cmd verify` (usa `spotless:apply` si falla por formato).
- Si agregaste endpoints, actualiza la tabla de `backend/README.md`; si tomaste una decisión que afecta a otros,
  agrega una entrada en `docs/decisiones/`.

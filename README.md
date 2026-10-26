# HelloFriend

Plataforma donde los estudiantes de Ingeniería Informática (PUCP) reportan incidencias académicas asociadas a un
curso, sin exponer el nombre del profesor, y el director de carrera las evalúa y les da seguimiento.
Proyecto del curso Ética y RSU.

| Carpeta | Qué hay |
| --- | --- |
| `backend/` | API en Spring Boot 4 (Java 21) con MySQL 8.0 y Flyway. Ver [`backend/README.md`](backend/README.md). |
| `frontend/` | Aplicación React + Vite. |
| `deploy/` | Apoyo para desarrollo local (MySQL y Mailpit con Docker). |
| `docs/` | Guías y decisiones del equipo. |

## Levantar el backend en local

Necesitas Java 21 y un MySQL 8.0 local (instalado o con Docker).

1. Crea las bases `hellofriend` y `hellofriend_test` y el usuario `hellofriend` (ver [`docs/guias/base-de-datos.md`](docs/guias/base-de-datos.md)).
2. Copia `backend/.env.example` como `backend/.env` y pon tu contraseña local.
3. En `backend/`: `.\mvnw.cmd verify` corre el formato y todas las pruebas; `.\mvnw.cmd spring-boot:run` arranca la API en <http://localhost:8080/api>.

La base real del proyecto está en Amazon RDS, pero **nadie desarrolla ni prueba contra ella**: cada persona usa su MySQL
local y el CI usa uno desechable.

## Integración continua

Cada pull request hacia `main` o `develop` corre `Backend` en GitHub Actions
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)). Cómo trabajar con ramas y PR: [`docs/guias/contribuir.md`](docs/guias/contribuir.md).

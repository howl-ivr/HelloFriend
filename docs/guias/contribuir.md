# Cómo contribuir

## Ramas

| Rama | Para qué | Reglas |
| --- | --- | --- |
| `main` | Lo que se entrega | Protegida: solo entra por pull request con `Backend` en verde. |
| `develop` | Integración del trabajo en curso | Protegida igual que `main`. |
| `feat/<modulo>-<tema>` | Trabajo nuevo, ej. `feat/usuarios-login` | Sale de `develop`. |
| `fix/<tema>`, `docs/<tema>`, `chore/<tema>` | Correcciones, documentación, configuración | Igual. |

Una rama por tarea, cortas: mejor dos PR pequeños que uno gigante. `develop` se integra a `main` con otro PR.
(`chore/ci-backend` es la excepción: sale de `main` por ser la base del repo.)

## Commits

Formato `tipo(alcance): qué cambió`, en español e imperativo. Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`.

## Pull request

1. Antes de abrirlo, en `backend/`: `.\mvnw.cmd verify` (formatea con `spotless:apply` si falla por formato).
2. `git fetch origin` y `git merge origin/develop` en tu rama; vuelve a correr las pruebas.
3. Abre el PR hacia `develop` y completa el checklist de la plantilla.
4. GitHub Actions corre `Backend`. Si sale en rojo, abre el paso que falló, corrige y vuelve a subir.
5. Se integra con el CI en verde y al menos una aprobación. Borra la rama después.

Si tocas la base de datos: migración **nueva**, nunca edites una ya subida, y avisa el número antes
(ver [`base-de-datos.md`](base-de-datos.md)).

## Proteger `main` y `develop` (una vez, lo hace quien administra el repositorio)

1. En GitHub: **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.
2. Nombre `proteger-ramas`, *Enforcement status*: **Active**.
3. En **Target branches** agrega `main` y `develop` (*Add target → Include by pattern*).
4. Activa **Require a pull request before merging** y pon **Required approvals: 1**.
5. Activa **Require status checks to pass**, pulsa **Add checks** y agrega **`Backend`**.
   El check solo aparece en la lista después de que el workflow haya corrido al menos una vez.
6. Opcional: **Require branches to be up to date before merging** y **Block force pushes**.
7. **Create**.

## Secretos

No subas contraseñas ni tokens. La configuración local va en `backend/.env` (ignorado); en el repo solo hay
`.env.example` con valores de ejemplo. Nada del repo, del CI ni de las pruebas se conecta a RDS.

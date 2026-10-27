# Pruebas del backend

Todas las pruebas corren con `.\mvnw.cmd verify` en `backend/`, que es también lo que ejecuta el CI en cada pull request.
Cada PR trae las pruebas de lo que agrega, y `verify` debe pasar antes de abrirlo.

## Tipos y nombres

| Tipo | Nombre del archivo | Qué prueba | Usa MySQL | Comando |
| --- | --- | --- | --- | --- |
| Unitaria | `*Tests.java` | Reglas de dominio puras | No | `.\mvnw.cmd test` (y `verify`) |
| Integración | `*IT.java` | API y base de datos reales | Sí, `hellofriend_test` | `.\mvnw.cmd verify` |

Surefire corre `*Tests`; Failsafe corre `*IT`. Un archivo que no termine en `Tests` ni en `IT` **no se ejecuta**.
`PruebaIntegracion` es la clase base de las `*IT` y por eso no lleva ninguno de los dos sufijos.

Primera vez: necesitas las bases `hellofriend` y `hellofriend_test` y tu `backend/.env`
(ver [`base-de-datos.md`](base-de-datos.md)). Flyway aplica las migraciones solo al correr las pruebas.
Configura `TEST_DB_URL`, `TEST_DB_USER` y `TEST_DB_PASSWORD` para tu MySQL local. Estas variables son independientes
de `DB_URL`, `DB_USER` y `DB_PASSWORD`: la aplicación puede usar otra base sin que `verify` intente autenticar las
pruebas con sus credenciales. No uses una base remota para las pruebas.

## Dónde van

Un paquete de pruebas por módulo, igual que en el código principal. Ejemplo con `usuarios`:

```
backend/src/test/java/com/hellofriend/usuarios/
├── dominio/UsuarioTests.java        # reglas sin Spring ni base de datos
├── EsquemaUsuariosIT.java           # restricciones de la BD (ya existe)
└── UsuarioControladorIT.java        # endpoints: registro, login...
```

## Qué lleva cada feature

1. **Dominio (`*Tests`)**: una prueba por regla, con fechas fijas y sin base de datos. Por ejemplo: la máquina de
   estados de las incidencias (saltos y retrocesos prohibidos), el cálculo de prioridad por confirmaciones o la
   normalización de texto para detectar incidencias similares. Son rápidas, así que se escriben muchas.
2. **Esquema (`Esquema{Modulo}IT`)**: por cada migración nueva, una prueba por restricción (`UNIQUE`, `CHECK`, `FK`)
   y un caso feliz con una fila válida. Usa `rechazaPor`, que exige que la BD rechace el dato **por esa restricción**
   y no por otro motivo:

   ```java
   @Test
   void correoNoSePuedeRepetir() {
   	insertar("ana@pucp.edu.pe", "ana", "ESTUDIANTE");
   	rechazaPor("uq_usuarios_usuario_correo", () -> insertar("ANA@pucp.edu.pe", "ana2", "ESTUDIANTE"));
   }
   ```

3. **API (`*ControladorIT`)**, por cada endpoint:
   - el caso feliz;
   - cada validación (400, con su mensaje);
   - el conflicto por estado (409);
   - el recurso inexistente o ajeno (404);
   - sin sesión (401) y rol no permitido (403), cuando exista la seguridad.
4. **Transversales** (cuando existan los módulos):
   - **Anonimato:** una prueba que falle si una respuesta, notificación o log incluye datos del autor o del profesor.
   - **Permisos:** una prueba parametrizada de cada endpoint contra cada rol.
   - **Auditoría:** comprueba que la aplicación nunca actualiza ni borra las tablas de solo adición
     (`incidencias_version`, `incidencias_historial_estado`) y que la vista `auditoria_historial_incidencia` refleja cada
     cambio de estado, observación y confirmación.

## Una prueba de integración nueva

```java
class CanalIT extends PruebaIntegracion {

	@Test
	void noPermiteDosCanalesParaElMismoCursoYPeriodo() {
		// preparar los datos con jdbc.sql(...), luego comprobar la regla
		rechazaPor("uq_canales_canal_curso_anio_semestre", () -> crearCanal(cursoId, 2026, 2));
	}
}
```

`PruebaIntegracion` ya trae `jdbc` (consultas SQL directas), el contexto completo de la aplicación y la transacción
que se deshace al terminar cada prueba.

## Reglas para que `verify` sea estable

- **Cada prueba es independiente**: crea lo que necesita, no deja datos y no depende del orden.
- **La base se deshace sola** (transacción con rollback). Si una prueba necesita que MySQL confirme los datos
  (`commit`), debe borrar lo que creó al terminar.
- **Fechas fijas**: el código usa el bean `Clock`; la prueba inyecta `Clock.fixed(...)` y no depende de la hora real.
- **Solo base local de pruebas**: `PruebaIntegracion` falla si la conexión no es a `localhost` o si la base no termina
  en `_test`. Ninguna prueba se conecta a RDS ni a servicios externos; el correo se prueba con un doble, no con SMTP real.
- **Nombres en español**, que digan qué se garantiza: `correoNoSePuedeRepetir`, no `test1`.
- **No desactives pruebas** (`@Disabled`) para que pase el CI: se corrigen o se eliminan con una razón en el PR.

## Seguridad y correo en las pruebas

- **Llamar un endpoint con un rol:** guarda un `Usuario` con ese rol y emite su token con `ServicioJwt.emitir(id, usuario,
  rol, versionSesion)`; envíalo en `Authorization: Bearer ...`. Ejemplos: `CursoAccesoIT`, `AdminUsuarioIT`. Si la prueba
  no trata de seguridad, basta `@WithMockUser(roles = "ADMINISTRADOR")` en la clase (como `CursoControladorIT`).
- **Cada endpoint protegido** lleva una prueba sin token (401) y otra con un rol no permitido (403).
- **Contraseñas:** en `application-test.properties` BCrypt usa costo 4 para que las pruebas no tarden.
- **Correo:** se reemplaza el notificador con `@MockitoBean` y se lee el token que recibe (`RecuperacionIT`); el contenido
  del mensaje se comprueba aparte con un `JavaMailSender` simulado (`NotificadorRecuperacionTests`).
- **No uses `jdbc` para leer algo que cambió una entidad sin antes `repositorio.flush()`**: JDBC no dispara el volcado
  de Hibernate y verías el valor anterior.

## Si el CI sale en rojo

1. En el PR, abre **Details** del job `Backend`.
2. Entra al paso en rojo y busca el primer `ERROR` o `FAIL`.
3. Corre `.\mvnw.cmd verify` en tu PC, corrige y vuelve a subir.
4. Si falla por formato, ejecuta `.\mvnw.cmd spotless:apply`.

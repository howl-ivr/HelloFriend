package com.hellofriend.incidencias.web;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.notificaciones.aplicacion.EmisorNotificaciones;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;

/** Casos de uso de incidencias sobre el esquema versionado por Flyway. */
@RestController
public class IncidenciasController {

	private static final String BASE = """
			SELECT i.*, k.nombre AS categoria, c.anio, c.semestre, a.codigo AS curso_codigo,
			       a.nombre AS curso_nombre, u.usuario AS autor_usuario, u.correo AS autor_correo
			FROM incidencias_incidencia i
			JOIN incidencias_categoria k ON k.id = i.categoria_id
			JOIN canales_canal c ON c.id = i.canal_id
			JOIN academico_curso a ON a.id = c.curso_id
			JOIN usuarios_usuario u ON u.id = i.autor_id
			""";

	private final JdbcTemplate jdbc;
	private final Clock reloj;
	private final EmisorNotificaciones avisos;

	public IncidenciasController(JdbcTemplate jdbc, Clock reloj, EmisorNotificaciones avisos) {
		this.jdbc = jdbc;
		this.reloj = reloj;
		this.avisos = avisos;
	}

	@GetMapping("/categorias")
	public List<String> categorias() {
		return jdbc.queryForList("SELECT nombre FROM incidencias_categoria ORDER BY nombre", String.class);
	}

	@GetMapping("/comentarios-predefinidos")
	public List<Map<String, Object>> comentariosPredefinidos() {
		return jdbc.queryForList("SELECT id, texto FROM incidencias_comentario_predefinido ORDER BY texto");
	}

	@GetMapping("/motivos-rechazo")
	@PreAuthorize("hasRole('DIRECTOR')")
	public List<Map<String, Object>> motivos() {
		return jdbc.queryForList("SELECT id, descripcion FROM incidencias_motivo_rechazo ORDER BY descripcion");
	}

	@GetMapping("/feed")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<Map<String, Object>> feed(@RequestParam(required = false) String canalId,
			@RequestParam(defaultValue = "recientes") String orden, @RequestParam(required = false) String prioridad,
			Authentication auth) {
		String usuario = ServicioJwt.usuarioActual(auth);
		if (canalId != null && !existe("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?",
				canalId, usuario)) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para ver sus incidencias.");
		}
		List<Object> parametros = new ArrayList<>();
		parametros.add(usuario);
		String sql = BASE + " WHERE i.estado IN ('APROBADA','EN_ATENCION','RESUELTA')"
				+ " AND EXISTS (SELECT 1 FROM canales_suscripcion s WHERE s.canal_id=i.canal_id AND s.usuario_id=?)";
		if (canalId != null) {
			sql += " AND i.canal_id = ?";
			parametros.add(canalId);
		}
		List<Map<String, Object>> lista = jdbc.queryForList(sql, parametros.toArray()).stream()
				.map(i -> respuesta(i, auth, false)).toList();
		return ordenar(lista.stream().filter(i -> prioridad == null || prioridad.isBlank()
				|| prioridad.equals(i.get("prioridad"))).toList(), orden);
	}

	@GetMapping("/feed/destacadas")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<Map<String, Object>> destacadas(Authentication auth) {
		String sql = BASE + " WHERE i.estado IN ('APROBADA','EN_ATENCION') AND i.total_confirmaciones > 0"
				+ " AND EXISTS (SELECT 1 FROM canales_suscripcion s WHERE s.canal_id=i.canal_id AND s.usuario_id=?)";
		return ordenar(jdbc.queryForList(sql, ServicioJwt.usuarioActual(auth)).stream()
				.map(i -> respuesta(i, auth, false)).toList(), "prioridad").stream().limit(5).toList();
	}

	@GetMapping("/canales/{canalId}/incidencias")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<Map<String, Object>> porCanal(@PathVariable String canalId,
			@RequestParam(defaultValue = "prioridad") String orden, Authentication auth) {
		String usuario = ServicioJwt.usuarioActual(auth);
		if (!existe("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?", canalId,
				usuario)) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para ver sus incidencias.");
		}
		return ordenar(jdbc.queryForList(BASE
				+ " WHERE i.canal_id = ? AND i.estado IN ('APROBADA','EN_ATENCION','RESUELTA')", canalId).stream()
				.map(i -> respuesta(i, auth, false)).toList(), orden);
	}

	@GetMapping("/mis-incidencias")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public List<Map<String, Object>> mias(Authentication auth) {
		return jdbc.queryForList(BASE + " WHERE i.autor_id = ? ORDER BY i.creado_en DESC",
				ServicioJwt.usuarioActual(auth)).stream().map(i -> respuesta(i, auth, false)).toList();
	}

	@GetMapping("/director/incidencias")
	@PreAuthorize("hasRole('DIRECTOR')")
	public List<Map<String, Object>> director(@RequestParam(defaultValue = "PENDIENTE") String estado,
			Authentication auth) {
		String db = estadoBd(estado);
		if (!List.of("PENDIENTE", "OBSERVADA", "APROBADA", "EN_ATENCION", "RESUELTA", "RECHAZADA").contains(db)) {
			throw ExcepcionNegocio.invalido("Estado desconocido.");
		}
		return ordenar(jdbc.queryForList(BASE + " WHERE i.estado = ?", db).stream()
				.map(i -> respuesta(i, auth, false)).toList(), "PENDIENTE".equals(db) ? "antiguas" : "prioridad");
	}

	@GetMapping("/incidencias/{id}")
	public Map<String, Object> detalle(@PathVariable String id, Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		boolean mia = actor.equals(i.get("autor_id"));
		boolean director = rol(auth, "DIRECTOR");
		if (rol(auth, "ADMINISTRADOR")) {
			throw ExcepcionNegocio.prohibido("El administrador consulta incidencias desde la auditoría.");
		}
		if (!visible((String) i.get("estado")) && !mia && !director) {
			throw ExcepcionNegocio.noEncontrado("La incidencia no existe.");
		}
		if (visible((String) i.get("estado")) && !mia && !director && !suscrito((String) i.get("canal_id"), actor)) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para ver esta incidencia.");
		}
		return respuesta(i, auth, true);
	}

	@PostMapping("/incidencias")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> crear(@RequestBody Contenido body, Authentication auth) {
		String actor = ServicioJwt.usuarioActual(auth);
		String categoria = validarContenido(body);
		String canalId = body.canalId();
		if (canalId == null) {
			throw ExcepcionNegocio.invalido("Elige un canal.");
		}
		String estadoCanal;
		try {
			estadoCanal = jdbc.queryForObject("SELECT estado FROM canales_canal WHERE id = ?", String.class, canalId);
		} catch (EmptyResultDataAccessException e) {
			throw ExcepcionNegocio.noEncontrado("El canal no existe.");
		}
		if (!"ACTIVO".equals(estadoCanal)) {
			throw ExcepcionNegocio.conflicto("Este canal está desactivado.");
		}
		String id = UUID.randomUUID().toString();
		LocalDateTime fecha = ahora();
		jdbc.update(
				"""
						INSERT INTO incidencias_incidencia
						(id, codigo_seguimiento, canal_id, autor_id, categoria_id, titulo, descripcion, estado, creado_en, actualizado_en)
						VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?)
						""",
				id, id.replace("-", "").substring(0, 16), canalId, actor, categoria, body.titulo().strip(),
				body.descripcion().strip(), fecha, fecha);
		version(id, 1, body, categoria, fecha);
		historial(id, null, "PENDIENTE", "CREADA", actor, null);
		jdbc.update("INSERT IGNORE INTO canales_suscripcion (id, canal_id, usuario_id, creada_en) VALUES (?, ?, ?, ?)",
				UUID.randomUUID().toString(), canalId, actor, fecha);
		auditar("INCIDENCIA_REGISTRADA", actor, id, null);
		return respuesta(buscar(id), auth, false);
	}

	@PutMapping("/incidencias/{id}")
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> editar(@PathVariable String id, @RequestBody Contenido body, Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		if (!actor.equals(i.get("autor_id"))) {
			throw ExcepcionNegocio.prohibido("Solo el autor puede editar su incidencia.");
		}
		if (!List.of("PENDIENTE", "OBSERVADA").contains(i.get("estado"))) {
			throw ExcepcionNegocio.conflicto("Solo se puede editar una incidencia pendiente u observada.");
		}
		String categoria = validarContenido(body);
		if (Objects.equals(body.titulo().strip(), i.get("titulo"))
				&& Objects.equals(body.descripcion().strip(), i.get("descripcion"))
				&& Objects.equals(categoria, i.get("categoria_id"))) {
			throw ExcepcionNegocio.invalido("No hay cambios que guardar.");
		}
		int numero = jdbc.queryForObject(
				"SELECT COALESCE(MAX(numero), 0) + 1 FROM incidencias_version WHERE incidencia_id = ?",
				Integer.class, id);
		jdbc.update(
				"UPDATE incidencias_incidencia SET titulo=?, descripcion=?, categoria_id=?, actualizado_en=? WHERE id=?",
				body.titulo().strip(), body.descripcion().strip(), categoria, ahora(), id);
		version(id, numero, body, categoria, ahora());
		historial(id, (String) i.get("estado"), (String) i.get("estado"), "EDITADA", actor,
				"Versión " + numero);
		auditar("INCIDENCIA_EDITADA", actor, id, "Versión " + numero);
		return respuesta(buscar(id), auth, true);
	}

	@PostMapping("/incidencias/{id}/reenviar")
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> reenviar(@PathVariable String id, Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		if (!actor.equals(i.get("autor_id"))) {
			throw ExcepcionNegocio.prohibido("Solo el autor puede reenviar su incidencia.");
		}
		if (!"OBSERVADA".equals(i.get("estado"))) {
			throw ExcepcionNegocio.conflicto("Solo se puede reenviar una incidencia observada.");
		}
		if (!puedeReenviar(id)) {
			throw ExcepcionNegocio.conflicto("Edita la incidencia antes de reenviarla.");
		}
		jdbc.update("UPDATE incidencias_incidencia SET estado='PENDIENTE', actualizado_en=? WHERE id=?", ahora(), id);
		historial(id, "OBSERVADA", "PENDIENTE", "REENVIADA", actor, null);
		auditar("INCIDENCIA_REENVIADA", actor, id, null);
		avisos.notificar(directores(), TipoNotificacion.REENVIADA, id, (String) i.get("canal_id"),
				"Incidencia reenviada", "Una incidencia observada volvió a revisión.");
		return respuesta(buscar(id), auth, true);
	}

	@PostMapping("/incidencias/{id}/confirmacion")
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> confirmar(@PathVariable String id, Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		if (!List.of("APROBADA", "EN_ATENCION").contains(i.get("estado")) || actor.equals(i.get("autor_id"))
				|| !suscrito((String) i.get("canal_id"), actor)) {
			throw ExcepcionNegocio.prohibido("No puedes confirmar esta incidencia.");
		}
		boolean anterior = existe(
				"SELECT COUNT(*) FROM incidencias_confirmacion WHERE incidencia_id=? AND estudiante_id=?",
				id, actor);
		if (anterior) {
			jdbc.update("DELETE FROM incidencias_confirmacion WHERE incidencia_id=? AND estudiante_id=?", id, actor);
		} else {
			jdbc.update(
					"INSERT INTO incidencias_confirmacion (id, incidencia_id, estudiante_id, creada_en) VALUES (?, ?, ?, ?)",
					UUID.randomUUID().toString(), id, actor, ahora());
		}
		int total = jdbc.queryForObject("SELECT COUNT(*) FROM incidencias_confirmacion WHERE incidencia_id=?",
				Integer.class,
				id);
		jdbc.update("UPDATE incidencias_incidencia SET total_confirmaciones=? WHERE id=?", total, id);
		auditar(anterior ? "INCIDENCIA_CONFIRMACION_RETIRADA" : "INCIDENCIA_CONFIRMADA", actor, id, null);
		return Map.of("confirmaciones", total, "confirmadaPorMi", !anterior);
	}

	@PutMapping("/incidencias/{id}/reaccion")
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> reaccionar(@PathVariable String id, @RequestBody Map<String, String> body,
			Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		if (!List.of("APROBADA", "EN_ATENCION").contains(i.get("estado"))
				|| !suscrito((String) i.get("canal_id"), actor)) {
			throw ExcepcionNegocio.prohibido("No puedes reaccionar a esta incidencia.");
		}
		String tipo = body.get("tipo");
		if (tipo != null && !List.of("APOYO", "PREOCUPANTE", "URGENTE", "LO_SIGO").contains(tipo)) {
			throw ExcepcionNegocio.invalido("Tipo de reacción desconocido.");
		}
		jdbc.update("DELETE FROM incidencias_reaccion WHERE incidencia_id=? AND usuario_id=?", id, actor);
		if (tipo != null) {
			jdbc.update("INSERT INTO incidencias_reaccion (id, incidencia_id, usuario_id, tipo) VALUES (?, ?, ?, ?)",
					UUID.randomUUID().toString(), id, actor, tipo);
		}
		Map<String, Object> salida = new LinkedHashMap<>();
		salida.put("reacciones", respuesta(buscar(id), auth, false).get("reacciones"));
		salida.put("miReaccion", tipo);
		return salida;
	}

	@PostMapping("/incidencias/{id}/comentarios")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public Map<String, Object> comentar(@PathVariable String id, @RequestBody Map<String, String> body,
			Authentication auth) {
		Map<String, Object> i = buscar(id);
		String actor = ServicioJwt.usuarioActual(auth);
		if (!List.of("APROBADA", "EN_ATENCION").contains(i.get("estado"))
				|| !suscrito((String) i.get("canal_id"), actor)) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para comentar.");
		}
		String opcion = body.get("comentarioPredefinidoId");
		List<String> textos = jdbc.queryForList("SELECT texto FROM incidencias_comentario_predefinido WHERE id=?",
				String.class, opcion);
		if (textos.isEmpty()) {
			throw ExcepcionNegocio.invalido("Elige un comentario de la lista.");
		}
		String nuevo = UUID.randomUUID().toString();
		LocalDateTime fecha = ahora();
		jdbc.update("""
				INSERT INTO incidencias_comentario (id, incidencia_id, usuario_id, predefinido_id, creada_en)
				VALUES (?, ?, ?, ?, ?)
				""", nuevo, id, actor, opcion, fecha);
		return Map.of("id", nuevo, "texto", textos.getFirst(), "fecha", fecha);
	}

	@PostMapping("/director/incidencias/{id}/{accion}")
	@Transactional
	@PreAuthorize("hasRole('DIRECTOR')")
	public Map<String, Object> decidir(@PathVariable String id, @PathVariable String accion,
			@RequestBody(required = false) Map<String, String> cuerpo, Authentication auth) {
		Map<String, Object> i = buscar(id);
		String anterior = (String) i.get("estado");
		String actor = ServicioJwt.usuarioActual(auth);
		Map<String, String> body = cuerpo == null ? Map.of() : cuerpo;
		String nuevo;
		String detalle = null;
		TipoNotificacion tipo;
		switch (accion) {
			case "aprobar" -> {
				exigirEstado(anterior, "PENDIENTE");
				nuevo = "APROBADA";
				tipo = TipoNotificacion.APROBADA;
				jdbc.update("UPDATE incidencias_incidencia SET estado=?, aprobada_en=?, actualizado_en=? WHERE id=?",
						nuevo, ahora(), ahora(), id);
			}
			case "observar" -> {
				exigirEstado(anterior, "PENDIENTE");
				detalle = texto(body.get("observacion"), 10, 500, "La observación");
				nuevo = "OBSERVADA";
				tipo = TipoNotificacion.OBSERVADA;
				String version = jdbc.queryForObject("""
						SELECT id FROM incidencias_version WHERE incidencia_id=? ORDER BY numero DESC LIMIT 1
						""", String.class, id);
				jdbc.update(
						"""
								INSERT INTO incidencias_observacion (id, incidencia_id, version_id, texto, creada_por, creada_en)
								VALUES (?, ?, ?, ?, ?, ?)
								""",
						UUID.randomUUID().toString(), id, version, detalle, actor, ahora());
				jdbc.update("UPDATE incidencias_incidencia SET estado=?, actualizado_en=? WHERE id=?", nuevo, ahora(),
						id);
			}
			case "rechazar" -> {
				exigirEstado(anterior, "PENDIENTE");
				String motivoId = body.get("motivoId");
				List<String> motivos = motivoId == null
						? List.of()
						: jdbc.queryForList(
								"SELECT descripcion FROM incidencias_motivo_rechazo WHERE id=?", String.class,
								motivoId);
				String comentario = body.get("comentario") == null ? "" : body.get("comentario").strip();
				if (motivos.isEmpty() && comentario.isEmpty()) {
					throw ExcepcionNegocio.invalido("Elige un motivo o escribe un comentario.");
				}
				if (comentario.length() > 500) {
					throw ExcepcionNegocio.invalido("El comentario no puede superar los 500 caracteres.");
				}
				detalle = motivos.isEmpty()
						? comentario
						: motivos.getFirst() + (comentario.isEmpty() ? "" : " — " + comentario);
				nuevo = "RECHAZADA";
				tipo = TipoNotificacion.RECHAZADA;
				jdbc.update("""
						UPDATE incidencias_incidencia SET estado=?, motivo_rechazo=?, revision_motivo_id=?,
						       revision_comentario=?, actualizado_en=? WHERE id=?
						""", nuevo, detalle, motivos.isEmpty() ? null : motivoId,
						comentario.isEmpty() ? null : comentario, ahora(), id);
			}
			case "atender" -> {
				exigirEstado(anterior, "APROBADA");
				nuevo = "EN_ATENCION";
				tipo = TipoNotificacion.EN_ATENCION;
				jdbc.update("UPDATE incidencias_incidencia SET estado=?, actualizado_en=? WHERE id=?", nuevo, ahora(),
						id);
			}
			case "resolver" -> {
				if (!List.of("APROBADA", "EN_ATENCION").contains(anterior)) {
					throw ExcepcionNegocio.conflicto("Solo se pueden resolver incidencias aprobadas.");
				}
				detalle = texto(body.get("solucion"), 10, 1000, "La solución");
				nuevo = "RESUELTA";
				tipo = TipoNotificacion.RESUELTA;
				jdbc.update(
						"UPDATE incidencias_incidencia SET estado=?, solucion=?, resuelta_en=?, actualizado_en=? WHERE id=?",
						nuevo, detalle, ahora(), ahora(), id);
			}
			default -> throw ExcepcionNegocio.noEncontrado("Acción desconocida.");
		}
		historial(id, anterior, nuevo, nuevo.equals("APROBADA") ? "APROBADA" : nuevo, actor, detalle);
		auditar("INCIDENCIA_" + ("APROBADA".equals(nuevo) ? "APROBADA" : nuevo), actor, id, detalle);
		avisos.notificar("OBSERVADA".equals(nuevo) || "RECHAZADA".equals(nuevo)
				? List.of((String) i.get("autor_id"))
				: destinatarios(i), tipo, id, (String) i.get("canal_id"),
				"Actualización de incidencia",
				"La incidencia «" + i.get("titulo") + "» está " + estadoApi(nuevo) + ".");
		return respuesta(buscar(id), auth, false);
	}

	private static void exigirEstado(String actual, String esperado) {
		if (!esperado.equals(actual)) {
			throw ExcepcionNegocio.conflicto("La incidencia ya cambió de estado.");
		}
	}

	private static String texto(String valor, int minimo, int maximo, String campo) {
		String limpio = valor == null ? "" : valor.strip();
		if (limpio.length() < minimo || limpio.length() > maximo) {
			throw ExcepcionNegocio.invalido(campo + " debe tener entre " + minimo + " y " + maximo + " caracteres.");
		}
		return limpio;
	}

	@GetMapping("/director/estadisticas")
	@PreAuthorize("hasRole('DIRECTOR')")
	public Map<String, Object> estadisticas(@RequestParam(required = false) String desde,
			@RequestParam(required = false) String hasta, @RequestParam(required = false) String periodo,
			Authentication auth) {
		LocalDate inicio = parseFecha(desde);
		LocalDate fin = parseFecha(hasta);
		if (inicio != null && fin != null && inicio.isAfter(fin)) {
			throw ExcepcionNegocio.invalido("La fecha inicial no puede ser posterior a la final.");
		}
		List<Map<String, Object>> filas = jdbc.queryForList(BASE).stream().filter(i -> filtraFecha(i, inicio, fin))
				.filter(i -> periodo == null || periodo.isBlank()
						|| periodo.equals(i.get("anio") + "-" + i.get("semestre")))
				.toList();
		List<Map<String, Object>> reportes = filas.stream().map(i -> respuesta(i, auth, false)).toList();
		Map<String, Long> cursos = new LinkedHashMap<>();
		Map<String, Long> categorias = new LinkedHashMap<>();
		Map<String, Long> estados = new LinkedHashMap<>();
		for (String nombre : this.categorias()) {
			categorias.put(nombre, 0L);
		}
		for (String estado : List.of("PENDIENTE", "OBSERVADA", "PUBLICADA", "EN_ATENCION", "RESUELTA", "RECHAZADA")) {
			estados.put(estado, 0L);
		}
		for (Map<String, Object> i : reportes) {
			Map<?, ?> canal = (Map<?, ?>) i.get("canal");
			cursos.merge((String) canal.get("nombre"), 1L, Long::sum);
			categorias.merge((String) i.get("categoria"), 1L, Long::sum);
			estados.merge((String) i.get("estado"), 1L, Long::sum);
		}
		List<Double> horas = filas.stream().filter(i -> i.get("resuelta_en") != null && i.get("aprobada_en") != null)
				.map(i -> java.time.Duration.between(fecha(i.get("aprobada_en")), fecha(i.get("resuelta_en")))
						.toMinutes() / 60.0)
				.toList();
		Map<String, Object> salida = new LinkedHashMap<>();
		salida.put("total", filas.size());
		salida.put("resueltas", horas.size());
		salida.put("tiempoPromedioHoras",
				horas.isEmpty()
						? null
						: horas.stream().mapToDouble(Double::doubleValue).average()
								.orElse(0));
		salida.put("porCurso", conteos(cursos, "nombre"));
		salida.put("porCategoria", conteos(categorias, "nombre"));
		salida.put("porEstado", conteos(estados, "estado"));
		salida.put("masConfirmadas", ordenar(reportes.stream()
				.filter(i -> visible(estadoBd((String) i.get("estado"))) && (int) i.get("confirmaciones") > 0).toList(),
				"prioridad").stream().limit(5).map(i -> {
					Map<?, ?> canal = (Map<?, ?>) i.get("canal");
					return Map.<String, Object>of("id", i.get("id"), "titulo", i.get("titulo"), "estado",
							i.get("estado"),
							"canal", canal.get("nombre"), "confirmaciones", i.get("confirmaciones"), "prioridad",
							i.get("prioridad"));
				}).toList());
		return salida;
	}

	@GetMapping("/admin/reportes")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<Map<String, Object>> reportes(@RequestParam(required = false) String desde,
			@RequestParam(required = false) String hasta, @RequestParam(required = false) String cursoId,
			@RequestParam(required = false) String estado, @RequestParam(required = false) String usuario,
			Authentication auth) {
		LocalDate inicio = parseFecha(desde);
		LocalDate fin = parseFecha(hasta);
		String estadoReal = estado == null ? null : estadoBd(estado);
		return jdbc.queryForList(BASE).stream().filter(i -> filtraFecha(i, inicio, fin))
				.filter(i -> cursoId == null || cursoId.isBlank() || existe(
						"SELECT COUNT(*) FROM canales_canal WHERE id=? AND curso_id=?", i.get("canal_id"), cursoId))
				.filter(i -> estadoReal == null || estadoReal.isBlank() || estadoReal.equals(i.get("estado")))
				.filter(i -> involucrado((String) i.get("id"), (String) i.get("autor_usuario"), usuario))
				.sorted(Comparator.comparing((Map<String, Object> i) -> fecha(i.get("creado_en"))).reversed())
				.map(i -> {
					Map<String, Object> r = respuesta(i, auth, false);
					Map<String, Object> fila = new LinkedHashMap<>();
					for (String clave : List.of("id", "titulo", "categoria", "estado", "fechaCreacion", "canal",
							"confirmaciones", "prioridad")) {
						fila.put(clave, r.get(clave));
					}
					fila.put("autor", i.get("autor_usuario"));
					fila.put("versiones", versiones((String) i.get("id")).size());
					return fila;
				}).toList();
	}

	@GetMapping("/admin/reportes/{id}")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public Map<String, Object> reporte(@PathVariable String id, Authentication auth) {
		Map<String, Object> i = buscar(id);
		Map<String, Object> r = respuesta(i, auth, true);
		r.put("autor", Map.of("usuario", i.get("autor_usuario"), "correo", i.get("autor_correo")));
		List<Map<String, Object>> eventos = new ArrayList<>(historialPara(id, true, true));
		for (Map<String, Object> c : jdbc.queryForList("""
				SELECT e.fecha, e.accion, u.usuario FROM auditoria_evento e
				JOIN usuarios_usuario u ON u.id=e.actor_id WHERE e.incidencia_id=?
				AND e.accion IN ('INCIDENCIA_CONFIRMADA', 'INCIDENCIA_CONFIRMACION_RETIRADA')
				""", id)) {
			Map<String, Object> e = new LinkedHashMap<>();
			e.put("tipo", "INCIDENCIA_CONFIRMADA".equals(c.get("accion")) ? "CONFIRMADA" : "CONFIRMACION_RETIRADA");
			e.put("estado", null);
			e.put("fecha", fecha(c.get("fecha")));
			e.put("detalle", null);
			e.put("responsable", c.get("usuario"));
			eventos.add(e);
		}
		eventos.sort(Comparator.comparing(e -> (LocalDateTime) e.get("fecha")));
		r.put("eventos", eventos);
		return r;
	}

	@GetMapping("/admin/auditoria")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<Map<String, Object>> auditoria() {
		return jdbc.queryForList("""
				SELECT e.id, e.accion, u.usuario AS actor, e.incidencia_id, e.detalle, e.fecha
				FROM auditoria_evento e LEFT JOIN usuarios_usuario u ON u.id=e.actor_id ORDER BY e.fecha DESC
				""").stream().map(r -> {
			Map<String, Object> fila = new LinkedHashMap<>();
			fila.put("id", r.get("id"));
			fila.put("accion", r.get("accion"));
			fila.put("actor", r.get("actor") == null ? "Sistema" : r.get("actor"));
			fila.put("incidenciaId", r.get("incidencia_id"));
			fila.put("detalle", r.get("detalle"));
			fila.put("fecha", fecha(r.get("fecha")));
			return fila;
		}).toList();
	}

	private boolean involucrado(String id, String autor, String busqueda) {
		if (busqueda == null || busqueda.isBlank()) {
			return true;
		}
		String texto = busqueda.strip().toLowerCase();
		if (autor.toLowerCase().contains(texto)) {
			return true;
		}
		return jdbc.queryForList("""
				SELECT u.usuario FROM incidencias_confirmacion c JOIN usuarios_usuario u ON u.id=c.estudiante_id
				WHERE c.incidencia_id=? UNION ALL
				SELECT u.usuario FROM incidencias_historial_estado h JOIN usuarios_usuario u ON u.id=h.cambiado_por
				WHERE h.incidencia_id=?
				""", id, id).stream().anyMatch(r -> ((String) r.get("usuario")).toLowerCase().contains(texto));
	}

	private static LocalDate parseFecha(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(valor);
		} catch (java.time.format.DateTimeParseException e) {
			throw ExcepcionNegocio.invalido("La fecha debe tener el formato AAAA-MM-DD.");
		}
	}

	private static boolean filtraFecha(Map<String, Object> i, LocalDate inicio, LocalDate fin) {
		LocalDate dia = fecha(i.get("creado_en")).toLocalDate();
		return (inicio == null || !dia.isBefore(inicio)) && (fin == null || !dia.isAfter(fin));
	}

	private static List<Map<String, Object>> conteos(Map<String, Long> valores, String clave) {
		return valores.entrySet().stream().map(e -> Map.<String, Object>of(clave, e.getKey(), "total", e.getValue()))
				.sorted(Comparator.comparing((Map<String, Object> r) -> (Long) r.get("total")).reversed()).toList();
	}

	private Map<String, Object> respuesta(Map<String, Object> i, Authentication auth, boolean detalle) {
		String id = (String) i.get("id");
		String usuario = ServicioJwt.usuarioActual(auth);
		String estado = (String) i.get("estado");
		boolean mia = usuario.equals(i.get("autor_id"));
		boolean director = rol(auth, "DIRECTOR");
		boolean administrador = rol(auth, "ADMINISTRADOR");
		boolean estudiante = rol(auth, "ESTUDIANTE");
		boolean suscrito = suscrito((String) i.get("canal_id"), usuario);
		boolean interactuable = List.of("APROBADA", "EN_ATENCION").contains(estado);
		int confirmaciones = ((Number) i.get("total_confirmaciones")).intValue();

		Map<String, Object> canal = new LinkedHashMap<>();
		canal.put("id", i.get("canal_id"));
		canal.put("nombre", i.get("curso_codigo") + " · " + i.get("curso_nombre"));
		canal.put("periodo", i.get("anio") + "-" + i.get("semestre"));
		Map<String, Integer> reacciones = new LinkedHashMap<>();
		for (String tipo : List.of("APOYO", "PREOCUPANTE", "URGENTE", "LO_SIGO")) {
			reacciones.put(tipo, 0);
		}
		for (Map<String, Object> r : jdbc.queryForList(
				"SELECT tipo, COUNT(*) AS total FROM incidencias_reaccion WHERE incidencia_id=? GROUP BY tipo", id)) {
			reacciones.put((String) r.get("tipo"), ((Number) r.get("total")).intValue());
		}
		List<String> propias = jdbc.queryForList(
				"SELECT tipo FROM incidencias_reaccion WHERE incidencia_id=? AND usuario_id=?", String.class, id,
				usuario);
		Map<String, Object> salida = new LinkedHashMap<>();
		salida.put("id", id);
		salida.put("titulo", i.get("titulo"));
		salida.put("descripcion", i.get("descripcion"));
		salida.put("categoria", i.get("categoria"));
		salida.put("estado", estadoApi(estado));
		salida.put("fechaCreacion", fecha(i.get("creado_en")));
		salida.put("canal", canal);
		salida.put("autor", director || administrador ? Map.of("usuario", i.get("autor_usuario")) : null);
		salida.put("confirmaciones", confirmaciones);
		salida.put("prioridad", prioridad(confirmaciones));
		salida.put("confirmadaPorMi", existe(
				"SELECT COUNT(*) FROM incidencias_confirmacion WHERE incidencia_id=? AND estudiante_id=?", id,
				usuario));
		salida.put("reacciones", reacciones);
		salida.put("miReaccion", propias.isEmpty() ? null : propias.getFirst());
		salida.put("totalComentarios",
				jdbc.queryForObject("SELECT COUNT(*) FROM incidencias_comentario WHERE incidencia_id=?", Long.class,
						id));
		salida.put("esMia", mia);
		salida.put("puedeConfirmar", interactuable && estudiante && suscrito && !mia);
		salida.put("puedeReaccionar", interactuable && estudiante && suscrito);
		salida.put("puedeComentar", interactuable && estudiante && suscrito);
		salida.put("puedeEditar", mia && List.of("PENDIENTE", "OBSERVADA").contains(estado));
		salida.put("puedeReenviar", mia && "OBSERVADA".equals(estado) && puedeReenviar(id));
		salida.put("revision", mia || director || administrador ? revision(i) : null);
		List<Map<String, Object>> observaciones = observaciones(id);
		salida.put("observacion", (mia || director) && "OBSERVADA".equals(estado) && !observaciones.isEmpty()
				? observaciones.getLast().get("texto")
				: null);
		salida.put("solucion", visible(estado) || mia || director || administrador ? i.get("solucion") : null);
		salida.put("fechaResolucion", fecha(i.get("resuelta_en")));
		if (detalle) {
			if (mia || director) {
				salida.put("observaciones", observaciones);
			}
			if (director || administrador) {
				salida.put("versiones", versiones(id));
			}
			salida.put("historial", historialPara(id, mia, director || administrador));
			salida.put("comentarios", jdbc.queryForList("""
					SELECT c.id, p.texto, c.creada_en AS fecha FROM incidencias_comentario c
					JOIN incidencias_comentario_predefinido p ON p.id=c.predefinido_id
					WHERE c.incidencia_id=? ORDER BY c.creada_en
					""", id).stream().map(c -> {
				Map<String, Object> fila = new LinkedHashMap<>(c);
				fila.put("fecha", fecha(c.get("fecha")));
				return fila;
			}).toList());
		}
		return salida;
	}

	private Map<String, Object> revision(Map<String, Object> i) {
		String estado = (String) i.get("estado");
		if ("RECHAZADA".equals(estado)) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("decision", "RECHAZADA");
			List<String> motivos = i.get("revision_motivo_id") == null
					? List.of()
					: jdbc.queryForList(
							"SELECT descripcion FROM incidencias_motivo_rechazo WHERE id=?", String.class,
							i.get("revision_motivo_id"));
			r.put("motivo", motivos.isEmpty() ? i.get("motivo_rechazo") : motivos.getFirst());
			r.put("comentario", i.get("revision_comentario"));
			r.put("fecha", ultimaFechaEstado((String) i.get("id"), "RECHAZADA"));
			return r;
		}
		if (i.get("aprobada_en") != null) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("decision", "APROBADA");
			r.put("motivo", null);
			r.put("comentario", null);
			r.put("fecha", fecha(i.get("aprobada_en")));
			return r;
		}
		return null;
	}

	private LocalDateTime ultimaFechaEstado(String id, String estado) {
		return fecha(jdbc.queryForObject(
				"SELECT MAX(cambiado_en) FROM incidencias_historial_estado WHERE incidencia_id=? AND estado_nuevo=?",
				Object.class, id, estado));
	}

	private List<Map<String, Object>> observaciones(String id) {
		return jdbc.queryForList("""
				SELECT o.texto, o.creada_en AS fecha, v.numero AS version FROM incidencias_observacion o
				JOIN incidencias_version v ON v.id=o.version_id WHERE o.incidencia_id=? ORDER BY o.creada_en
				""", id).stream().map(r -> {
			Map<String, Object> fila = new LinkedHashMap<>(r);
			fila.put("fecha", fecha(r.get("fecha")));
			return fila;
		}).toList();
	}

	private List<Map<String, Object>> versiones(String id) {
		return jdbc.queryForList("""
				SELECT v.numero AS n, v.titulo, v.descripcion, k.nombre AS categoria, v.creada_en AS fecha
				FROM incidencias_version v JOIN incidencias_categoria k ON k.id=v.categoria_id
				WHERE v.incidencia_id=? ORDER BY v.numero
				""", id).stream().map(r -> {
			Map<String, Object> fila = new LinkedHashMap<>(r);
			fila.put("fecha", fecha(r.get("fecha")));
			return fila;
		}).toList();
	}

	private List<Map<String, Object>> historialPara(String id, boolean mia, boolean director) {
		if (!mia && !director) {
			return List.of();
		}
		return jdbc.queryForList("""
				SELECT h.tipo_evento, h.estado_nuevo, h.cambiado_en, h.detalle, u.usuario
				FROM incidencias_historial_estado h JOIN usuarios_usuario u ON u.id=h.cambiado_por
				WHERE h.incidencia_id=? ORDER BY h.cambiado_en
				""", id).stream().filter(r -> director || !"EDITADA".equals(r.get("tipo_evento"))).map(r -> {
			Map<String, Object> fila = new LinkedHashMap<>();
			String estado = (String) r.get("estado_nuevo");
			fila.put("tipo", r.get("tipo_evento") == null ? eventoEstado(estado) : r.get("tipo_evento"));
			fila.put("estado", estadoApi(estado));
			fila.put("fecha", fecha(r.get("cambiado_en")));
			fila.put("detalle", director ? r.get("detalle") : null);
			fila.put("responsable", director ? r.get("usuario") : null);
			return fila;
		}).toList();
	}

	private static String eventoEstado(String estado) {
		return switch (estado) {
			case "PENDIENTE" -> "CREADA";
			case "APROBADA" -> "APROBADA";
			default -> estado;
		};
	}

	private boolean puedeReenviar(String id) {
		Integer actual = jdbc.queryForObject(
				"SELECT COALESCE(MAX(numero), 0) FROM incidencias_version WHERE incidencia_id=?", Integer.class, id);
		Integer observada = jdbc.queryForObject("""
				SELECT COALESCE(MAX(v.numero), 0) FROM incidencias_observacion o
				JOIN incidencias_version v ON v.id=o.version_id WHERE o.incidencia_id=?
				""", Integer.class, id);
		return actual > observada;
	}

	private static boolean visible(String estado) {
		return List.of("APROBADA", "EN_ATENCION", "RESUELTA").contains(estado);
	}

	private static String estadoApi(String estado) {
		return "APROBADA".equals(estado) ? "PUBLICADA" : estado;
	}

	private static String estadoBd(String estado) {
		return "PUBLICADA".equals(estado) ? "APROBADA" : estado;
	}

	private static boolean rol(Authentication auth, String nombre) {
		return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + nombre));
	}

	private static LocalDateTime fecha(Object valor) {
		return valor instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) valor;
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

	private boolean existe(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros) > 0;
	}

	private boolean suscrito(String canal, String usuario) {
		return existe("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id=? AND usuario_id=?", canal, usuario);
	}

	private Map<String, Object> buscar(String id) {
		try {
			return jdbc.queryForMap(BASE + " WHERE i.id = ?", id);
		} catch (EmptyResultDataAccessException e) {
			throw ExcepcionNegocio.noEncontrado("La incidencia no existe.");
		}
	}

	private String validarContenido(Contenido body) {
		if (body.titulo() == null || body.titulo().strip().length() < 5 || body.titulo().strip().length() > 100) {
			throw ExcepcionNegocio.invalido("El título debe tener de 5 a 100 caracteres.");
		}
		if (body.descripcion() == null || body.descripcion().strip().length() < 20
				|| body.descripcion().strip().length() > 1000) {
			throw ExcepcionNegocio.invalido("La descripción debe tener de 20 a 1000 caracteres.");
		}
		try {
			return jdbc.queryForObject("SELECT id FROM incidencias_categoria WHERE nombre = ?", String.class,
					body.categoria());
		} catch (EmptyResultDataAccessException e) {
			throw ExcepcionNegocio.invalido("Elige una categoría de la lista.");
		}
	}

	private void version(String id, int numero, Contenido body, String categoria, LocalDateTime fecha) {
		jdbc.update(
				"""
						INSERT INTO incidencias_version (id, incidencia_id, numero, titulo, descripcion, categoria_id, creada_en)
						VALUES (?, ?, ?, ?, ?, ?, ?)
						""",
				UUID.randomUUID().toString(), id, numero, body.titulo().strip(), body.descripcion().strip(),
				categoria, fecha);
	}

	private void historial(String id, String anterior, String nuevo, String tipo, String actor, String detalle) {
		jdbc.update("""
				INSERT INTO incidencias_historial_estado
				(id, incidencia_id, estado_anterior, estado_nuevo, cambiado_por, cambiado_en, tipo_evento, detalle)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""", UUID.randomUUID().toString(), id, anterior, nuevo, actor, ahora(), tipo, detalle);
	}

	private void auditar(String accion, String actor, String incidencia, String detalle) {
		jdbc.update(
				"INSERT INTO auditoria_evento (id, accion, actor_id, incidencia_id, detalle, fecha) VALUES (?, ?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), accion, actor, incidencia, detalle, ahora());
	}

	private List<String> directores() {
		return jdbc.queryForList("SELECT id FROM usuarios_usuario WHERE rol='DIRECTOR' AND activo=TRUE", String.class);
	}

	private List<String> destinatarios(Map<String, Object> i) {
		List<String> ids = new ArrayList<>(jdbc.queryForList(
				"SELECT usuario_id FROM canales_suscripcion WHERE canal_id=?", String.class, i.get("canal_id")));
		ids.add((String) i.get("autor_id"));
		return ids;
	}

	private static String prioridad(int total) {
		return total >= 5 ? "ALTA" : total >= 2 ? "MEDIA" : "BAJA";
	}

	private static List<Map<String, Object>> ordenar(List<Map<String, Object>> entrada, String orden) {
		Comparator<Map<String, Object>> recientes = Comparator.comparing(
				(i) -> (LocalDateTime) i.get("fechaCreacion"));
		Comparator<Map<String, Object>> comparador = switch (orden) {
			case "prioridad" -> Comparator.<Map<String, Object>>comparingInt(i -> (int) i.get("confirmaciones"))
					.thenComparing(recientes).reversed();
			case "antiguas" -> recientes;
			default -> recientes.reversed();
		};
		return entrada.stream().sorted(comparador).toList();
	}

	public record Contenido(String canalId, String categoria, String titulo, String descripcion) {
	}

}

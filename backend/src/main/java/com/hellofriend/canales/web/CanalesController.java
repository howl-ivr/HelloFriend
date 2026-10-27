package com.hellofriend.canales.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.compartido.web.ExcepcionNegocio;

/** API de canales respaldada por las tablas de V2; los ids son UUID. */
@RestController
public class CanalesController {

	private static final String CANALES = """
			SELECT c.id, c.curso_id, a.codigo, a.nombre AS curso_nombre, c.anio, c.semestre, c.estado,
			       c.creado_en, (SELECT COUNT(*) FROM canales_suscripcion s WHERE s.canal_id = c.id) AS suscritos,
			       (SELECT COUNT(*) FROM incidencias_incidencia i WHERE i.canal_id = c.id
			          AND i.estado IN ('APROBADA', 'EN_ATENCION', 'RESUELTA')) AS publicadas,
			       (SELECT COUNT(*) FROM incidencias_incidencia i WHERE i.canal_id = c.id
			          AND i.estado = 'PENDIENTE') AS pendientes
			FROM canales_canal c JOIN academico_curso a ON a.id = c.curso_id
			""";

	private final JdbcTemplate jdbc;
	private final Clock reloj;

	public CanalesController(JdbcTemplate jdbc, Clock reloj) {
		this.jdbc = jdbc;
		this.reloj = reloj;
	}

	@GetMapping("/canales")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<Map<String, Object>> listar(Authentication auth) {
		return jdbc.queryForList(CANALES + " ORDER BY c.anio DESC, c.semestre DESC, a.codigo", new Object[0]).stream()
				.map(c -> respuesta(c, ServicioJwt.usuarioActual(auth), false)).toList();
	}

	@GetMapping("/canales/{id}")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public Map<String, Object> detalle(@PathVariable String id, Authentication auth) {
		return respuesta(canal(id), ServicioJwt.usuarioActual(auth), false);
	}

	@PostMapping("/canales/{id}/suscripcion")
	@Transactional
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public Map<String, Object> suscribir(@PathVariable String id, Authentication auth) {
		Map<String, Object> canal = canal(id);
		if (!"ACTIVO".equals(canal.get("estado"))) {
			throw ExcepcionNegocio.conflicto("Este canal está desactivado.");
		}
		String usuario = ServicioJwt.usuarioActual(auth);
		jdbc.update("INSERT IGNORE INTO canales_suscripcion (id, canal_id, usuario_id, creada_en) VALUES (?, ?, ?, ?)",
				UUID.randomUUID().toString(), id, usuario, ahora());
		return respuesta(canal(id), usuario, false);
	}

	@DeleteMapping("/canales/{id}/suscripcion")
	@Transactional
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public Map<String, Object> desuscribir(@PathVariable String id, Authentication auth) {
		canal(id);
		String usuario = ServicioJwt.usuarioActual(auth);
		jdbc.update("DELETE FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?", id, usuario);
		return respuesta(canal(id), usuario, false);
	}

	@GetMapping("/periodos")
	@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR')")
	public List<String> periodos() {
		return jdbc.queryForList(
				"SELECT DISTINCT CONCAT(anio, '-', semestre) FROM canales_canal ORDER BY 1 DESC", String.class);
	}

	@GetMapping("/admin/canales")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<Map<String, Object>> listarAdmin(@RequestParam(required = false) String periodo, Authentication auth) {
		String sql = CANALES;
		List<Object> parametros = new ArrayList<>();
		if (periodo != null && !periodo.isBlank()) {
			int[] partes = periodo(periodo);
			sql += " WHERE c.anio = ? AND c.semestre = ?";
			parametros.add(partes[0]);
			parametros.add(partes[1]);
		}
		return jdbc.queryForList(sql + " ORDER BY c.creado_en DESC", parametros.toArray()).stream()
				.map(c -> respuesta(c, ServicioJwt.usuarioActual(auth), true)).toList();
	}

	@PostMapping("/admin/canales")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<Map<String, Object>> crear(@RequestBody SolicitudCanales solicitud, Authentication auth) {
		int[] partes = periodo(solicitud.periodo());
		if (solicitud.cursoIds() == null || solicitud.cursoIds().isEmpty()) {
			throw ExcepcionNegocio.invalido("Elige al menos un curso.");
		}
		String actor = ServicioJwt.usuarioActual(auth);
		List<Map<String, Object>> creados = new ArrayList<>();
		for (String cursoId : solicitud.cursoIds().stream().distinct().toList()) {
			if (cursoId == null || !existe("SELECT COUNT(*) FROM academico_curso WHERE id = ?", cursoId)) {
				throw ExcepcionNegocio.invalido("Uno de los cursos no existe.");
			}
			try {
				String id = UUID.randomUUID().toString();
				jdbc.update("""
						INSERT INTO canales_canal (id, curso_id, anio, semestre, estado, creado_por, creado_en)
						VALUES (?, ?, ?, ?, 'ACTIVO', ?, ?)
						""", id, cursoId, partes[0], partes[1], actor, ahora());
				jdbc.update(
						"INSERT INTO auditoria_evento (id, accion, actor_id, detalle, fecha) VALUES (?, ?, ?, ?, ?)",
						UUID.randomUUID().toString(), "CANAL_CREADO", actor, id, ahora());
				creados.add(respuesta(canal(id), actor, true));
			} catch (DataIntegrityViolationException e) {
				throw ExcepcionNegocio.conflicto("Ya existe un canal para ese curso y periodo.");
			}
		}
		return creados;
	}

	@PostMapping("/admin/canales/{id}/desactivar")
	@Transactional
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public Map<String, Object> desactivar(@PathVariable String id, Authentication auth) {
		Map<String, Object> canal = canal(id);
		if (!"ACTIVO".equals(canal.get("estado"))) {
			throw ExcepcionNegocio.conflicto("El canal ya está desactivado.");
		}
		jdbc.update("UPDATE canales_canal SET estado = 'DESACTIVADO', desactivado_en = ? WHERE id = ?", ahora(), id);
		jdbc.update("INSERT INTO auditoria_evento (id, accion, actor_id, detalle, fecha) VALUES (?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), "CANAL_DESACTIVADO", ServicioJwt.usuarioActual(auth), id, ahora());
		return respuesta(canal(id), ServicioJwt.usuarioActual(auth), true);
	}

	private Map<String, Object> canal(String id) {
		try {
			return jdbc.queryForMap(CANALES + " WHERE c.id = ?", id);
		} catch (EmptyResultDataAccessException e) {
			throw ExcepcionNegocio.noEncontrado("El canal no existe.");
		}
	}

	private Map<String, Object> respuesta(Map<String, Object> c, String usuario, boolean admin) {
		String id = (String) c.get("id");
		String nombre = c.get("codigo") + " · " + c.get("curso_nombre");
		String periodo = c.get("anio") + "-" + c.get("semestre");
		Map<String, Object> salida = new LinkedHashMap<>();
		salida.put("id", id);
		salida.put("nombre", nombre);
		salida.put("descripcion", "Incidencias del curso de " + c.get("curso_nombre") + " (" + periodo + ")");
		salida.put("periodo", periodo);
		salida.put("activo", "ACTIVO".equals(c.get("estado")));
		Map<String, Object> curso = new HashMap<>();
		curso.put("id", c.get("curso_id"));
		curso.put("codigo", c.get("codigo"));
		curso.put("nombre", c.get("curso_nombre"));
		salida.put("curso", curso);
		salida.put("suscrito",
				existe("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?", id,
						usuario));
		salida.put("totalSuscritos", c.get("suscritos"));
		salida.put("totalPublicadas", c.get("publicadas"));
		if (admin) {
			salida.put("totalPendientes", c.get("pendientes"));
			salida.put("fechaCreacion", c.get("creado_en"));
		}
		return salida;
	}

	private boolean existe(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros) > 0;
	}

	private static int[] periodo(String valor) {
		if (valor == null || !valor.matches("\\d{4}-[12]")) {
			throw ExcepcionNegocio.invalido("El periodo debe tener el formato año-semestre, por ejemplo 2026-2.");
		}
		int anio = Integer.parseInt(valor.substring(0, 4));
		if (anio < 2000 || anio > 2100) {
			throw ExcepcionNegocio.invalido("El año del periodo debe estar entre 2000 y 2100.");
		}
		return new int[]{anio, Integer.parseInt(valor.substring(5))};
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

	public record SolicitudCanales(String periodo, List<String> cursoIds) {
	}

}

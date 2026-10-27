package com.hellofriend.academico.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.academico.aplicacion.CursoDto;
import com.hellofriend.academico.aplicacion.CursoServicio;
import com.hellofriend.academico.aplicacion.CursoSolicitud;
import com.hellofriend.compartido.seguridad.ServicioJwt;

/** Catalogo de cursos (ruta del contrato con el frontend: /admin/cursos). Solo para el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/cursos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CursoControlador {

	private final CursoServicio servicio;
	private final JdbcTemplate jdbc;
	private final Clock reloj;

	public CursoControlador(CursoServicio servicio, JdbcTemplate jdbc, Clock reloj) {
		this.servicio = servicio;
		this.jdbc = jdbc;
		this.reloj = reloj;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public CursoDto crear(@Valid @RequestBody CursoSolicitud solicitud, Authentication auth) {
		CursoDto curso = servicio.crear(solicitud);
		String actor = ServicioJwt.usuarioActual(auth);
		Long valido = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios_usuario WHERE id=?", Long.class, actor);
		jdbc.update("INSERT INTO auditoria_evento (id, accion, actor_id, detalle, fecha) VALUES (?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), "CURSO_REGISTRADO", valido != null && valido > 0 ? actor : null,
				curso.codigo(),
				LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
		return curso;
	}

	/** Lista todos los cursos, o solo los que contienen {@code nombre} si se envia ese parametro. */
	@GetMapping
	public List<Map<String, Object>> listar(@RequestParam(required = false) String nombre,
			@RequestParam(required = false) String periodo) {
		return servicio.porNombre(nombre).stream().map(c -> {
			boolean tieneCanal = false;
			if (periodo != null && periodo.matches("\\d{4}-[12]")) {
				Long total = jdbc.queryForObject(
						"SELECT COUNT(*) FROM canales_canal WHERE curso_id = ? AND anio = ? AND semestre = ?",
						Long.class,
						c.id(), Integer.parseInt(periodo.substring(0, 4)), Integer.parseInt(periodo.substring(5)));
				tieneCanal = total != null && total > 0;
			}
			return Map.<String, Object>of("id", c.id(), "codigo", c.codigo(), "nombre", c.nombre(),
					"creadoEn", c.creadoEn(), "tieneCanal", tieneCanal);
		}).toList();
	}

	@GetMapping("/{id}")
	public CursoDto porId(@PathVariable String id) {
		return servicio.porId(id);
	}

	@PutMapping("/{id}")
	public CursoDto modificar(@PathVariable String id, @Valid @RequestBody CursoSolicitud solicitud) {
		return servicio.modificar(id, solicitud);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable String id) {
		servicio.eliminar(id);
	}

}

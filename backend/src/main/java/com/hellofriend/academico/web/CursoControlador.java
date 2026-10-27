package com.hellofriend.academico.web;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.hellofriend.academico.aplicacion.CursoConCanalDto;
import com.hellofriend.academico.aplicacion.CursoDto;
import com.hellofriend.academico.aplicacion.CursoServicio;
import com.hellofriend.academico.aplicacion.CursoSolicitud;
import com.hellofriend.canales.aplicacion.ServicioCanales;
import com.hellofriend.compartido.seguridad.ServicioJwt;

/** Catalogo de cursos (ruta del contrato con el frontend: /admin/cursos). Solo para el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/cursos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CursoControlador {

	private final CursoServicio servicio;
	private final ServicioCanales canales;

	public CursoControlador(CursoServicio servicio, ServicioCanales canales) {
		this.servicio = servicio;
		this.canales = canales;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CursoDto crear(@Valid @RequestBody CursoSolicitud solicitud, Authentication auth) {
		return servicio.crear(solicitud, ServicioJwt.usuarioActual(auth));
	}

	/**
	 * Lista todos los cursos, o solo los que contienen {@code nombre}. Con {@code periodo} (por ejemplo 2026-2),
	 * {@code tieneCanal} indica qué cursos ya tienen canal en ese periodo.
	 */
	@GetMapping
	public List<CursoConCanalDto> listar(@RequestParam(required = false) String nombre,
			@RequestParam(required = false) String periodo) {
		Set<String> conCanal = canales.idsDeCursosConCanal(periodo);
		return servicio.porNombre(nombre).stream().map(c -> CursoConCanalDto.de(c, conCanal.contains(c.id()))).toList();
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

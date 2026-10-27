package com.hellofriend.academico.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Catalogo de cursos (ruta del contrato con el frontend: /admin/cursos). Solo para el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/cursos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CursoControlador {

	private final CursoServicio servicio;

	public CursoControlador(CursoServicio servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CursoDto crear(@Valid @RequestBody CursoSolicitud solicitud) {
		return servicio.crear(solicitud);
	}

	/** Lista todos los cursos, o solo los que contienen {@code nombre} si se envia ese parametro. */
	@GetMapping
	public List<CursoDto> listar(@RequestParam(required = false) String nombre) {
		return servicio.porNombre(nombre);
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

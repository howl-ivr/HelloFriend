package com.hellofriend.canales.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.canales.aplicacion.CanalDto;
import com.hellofriend.canales.aplicacion.ServicioCanales;
import com.hellofriend.canales.aplicacion.SolicitudCanales;
import com.hellofriend.compartido.seguridad.ServicioJwt;

/** Canales (docs/API.md): estudiantes y profesores los consultan y se suscriben; el administrador los crea y baja. */
@RestController
public class CanalesController {

	private final ServicioCanales servicio;

	public CanalesController(ServicioCanales servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/canales")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<CanalDto> listar(Authentication auth) {
		return servicio.listar(ServicioJwt.usuarioActual(auth));
	}

	@GetMapping("/canales/{id}")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public CanalDto detalle(@PathVariable String id, Authentication auth) {
		return servicio.detalle(id, ServicioJwt.usuarioActual(auth));
	}

	@PostMapping("/canales/{id}/suscripcion")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public CanalDto suscribir(@PathVariable String id, Authentication auth) {
		return servicio.suscribir(id, ServicioJwt.usuarioActual(auth));
	}

	@DeleteMapping("/canales/{id}/suscripcion")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public CanalDto desuscribir(@PathVariable String id, Authentication auth) {
		return servicio.desuscribir(id, ServicioJwt.usuarioActual(auth));
	}

	/** Periodos con canales (por ejemplo "2026-2"), del más nuevo al más viejo. */
	@GetMapping("/periodos")
	@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR')")
	public List<String> periodos() {
		return servicio.periodos();
	}

	@GetMapping("/admin/canales")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<CanalDto> listarAdmin(@RequestParam(required = false) String periodo, Authentication auth) {
		return servicio.listarAdmin(periodo, ServicioJwt.usuarioActual(auth));
	}

	@PostMapping("/admin/canales")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<CanalDto> crear(@RequestBody SolicitudCanales solicitud, Authentication auth) {
		return servicio.crear(solicitud, ServicioJwt.usuarioActual(auth));
	}

	@PostMapping("/admin/canales/{id}/desactivar")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public CanalDto desactivar(@PathVariable String id, Authentication auth) {
		return servicio.desactivar(id, ServicioJwt.usuarioActual(auth));
	}

}

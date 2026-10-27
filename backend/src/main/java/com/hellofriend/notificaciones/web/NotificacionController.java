package com.hellofriend.notificaciones.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.notificaciones.aplicacion.NotificacionDto;
import com.hellofriend.notificaciones.aplicacion.ServicioNotificaciones;

/**
 * Bandeja de notificaciones (docs/API.md). Cualquier rol con token; cada persona ve y marca solo las suyas.
 */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

	private final ServicioNotificaciones servicio;

	public NotificacionController(ServicioNotificaciones servicio) {
		this.servicio = servicio;
	}

	/** GET /notificaciones → mis avisos, los más nuevos primero. */
	@GetMapping
	public List<NotificacionDto> listar(Authentication autenticacion) {
		return servicio.listar(ServicioJwt.usuarioActual(autenticacion));
	}

	/** PATCH /notificaciones/{id}/leida → 204. 404 si no existe o es de otra persona. */
	@PatchMapping("/{id}/leida")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void marcarLeida(@PathVariable String id, Authentication autenticacion) {
		servicio.marcarLeida(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/** PATCH /notificaciones/leidas → 204. Marca todas las mías. */
	@PatchMapping("/leidas")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void marcarTodasLeidas(Authentication autenticacion) {
		servicio.marcarTodasLeidas(ServicioJwt.usuarioActual(autenticacion));
	}

}

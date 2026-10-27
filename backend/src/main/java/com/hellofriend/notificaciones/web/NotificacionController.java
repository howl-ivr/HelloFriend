package com.hellofriend.notificaciones.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.notificaciones.aplicacion.NoLeidasDto;
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

	/** GET /notificaciones/no-leidas → {total}: el número de la campanita. */
	@GetMapping("/no-leidas")
	public NoLeidasDto noLeidas(Authentication autenticacion) {
		return servicio.contarNoLeidas(ServicioJwt.usuarioActual(autenticacion));
	}

	/** PATCH /notificaciones/{id}/leida → 204. 404 si no existe o es de otra persona. */
	@PatchMapping("/{id}/leida")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void marcarLeida(@PathVariable String id, Authentication autenticacion) {
		servicio.marcarLeida(ServicioJwt.usuarioActual(autenticacion), id);
	}

	/**
	 * PATCH /notificaciones/leidas[?hasta=] → 204. Marca las mías. Con {@code hasta} (la {@code fecha} del aviso más
	 * nuevo que se mostró) solo las creadas hasta ese momento; sin él, todas.
	 */
	@PatchMapping("/leidas")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void marcarTodasLeidas(Authentication autenticacion,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
		servicio.marcarTodasLeidas(ServicioJwt.usuarioActual(autenticacion), hasta);
	}

}

package com.hellofriend.auditoria.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.auditoria.aplicacion.EventoAuditoriaDto;
import com.hellofriend.auditoria.aplicacion.ServicioAuditoria;

/** Auditoría general (docs/API.md > Administrador). Solo el rol ADMINISTRADOR. */
@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

	private final ServicioAuditoria servicio;

	public AuditoriaController(ServicioAuditoria servicio) {
		this.servicio = servicio;
	}

	/** GET /admin/auditoria → los eventos, del más nuevo al más viejo. */
	@GetMapping("/admin/auditoria")
	public List<EventoAuditoriaDto> listar() {
		return servicio.listar();
	}

}

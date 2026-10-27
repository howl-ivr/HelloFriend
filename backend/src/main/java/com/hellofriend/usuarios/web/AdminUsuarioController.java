package com.hellofriend.usuarios.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.usuarios.aplicacion.ServicioUsuarios;
import com.hellofriend.usuarios.aplicacion.UsuarioResumenDto;
import com.hellofriend.usuarios.dominio.EstadoCuenta;

/** Revisión de solicitudes de registro (docs/API.md > Administrador). Solo el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

	private final ServicioUsuarios servicio;

	public AdminUsuarioController(ServicioUsuarios servicio) {
		this.servicio = servicio;
	}

	/** GET /admin/usuarios?estado=PENDIENTE: estudiantes, los más antiguos primero. Sin {@code estado}, todos. */
	@GetMapping
	public List<UsuarioResumenDto> listar(@RequestParam(required = false) EstadoCuenta estado) {
		return servicio.listarEstudiantes(estado);
	}

	/** PENDIENTE → ACTIVA. 409 si la cuenta ya no está pendiente. */
	@PostMapping("/{id}/aprobar")
	public UsuarioResumenDto aprobar(@PathVariable String id) {
		return servicio.aprobar(id);
	}

	/** PENDIENTE → RECHAZADA. 409 si la cuenta ya no está pendiente. */
	@PostMapping("/{id}/rechazar")
	public UsuarioResumenDto rechazar(@PathVariable String id) {
		return servicio.rechazar(id);
	}

}

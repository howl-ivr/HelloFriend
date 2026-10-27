package com.hellofriend.usuarios.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.aplicacion.ServicioUsuarios;
import com.hellofriend.usuarios.aplicacion.SolicitudCambioRol;
import com.hellofriend.usuarios.aplicacion.UsuarioResumenDto;
import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;

/** Administración de cuentas (docs/API.md > Administrador). Solo el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

	private final ServicioUsuarios servicio;

	public AdminUsuarioController(ServicioUsuarios servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /admin/usuarios?estado=PENDIENTE&rol=PROFESOR: los más antiguos primero. Sin {@code rol}, estudiantes (el
	 * contrato); sin {@code estado}, de todos los estados.
	 */
	@GetMapping
	public List<UsuarioResumenDto> listar(@RequestParam(required = false) EstadoCuenta estado,
			@RequestParam(required = false) Rol rol) {
		return servicio.listar(rol, estado);
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

	/**
	 * PUT /admin/usuarios/{id}/rol {rol}. Cierra las sesiones del usuario. 400 si es el propio; 409 si no está
	 * aprobada.
	 */
	@PutMapping("/{id}/rol")
	public UsuarioResumenDto cambiarRol(@PathVariable String id, @Valid @RequestBody SolicitudCambioRol solicitud,
			Authentication autenticacion) {
		return servicio.cambiarRol(id, solicitud.rol(), ServicioJwt.usuarioActual(autenticacion));
	}

	/** Deja la cuenta sin acceso y cierra sus sesiones. 400 si es la propia; 409 si ya está desactivada. */
	@PostMapping("/{id}/desactivar")
	public UsuarioResumenDto desactivar(@PathVariable String id, Authentication autenticacion) {
		return servicio.desactivar(id, ServicioJwt.usuarioActual(autenticacion));
	}

	/** Devuelve el acceso a una cuenta desactivada. Las sesiones anteriores no reviven: hay que iniciar sesión. */
	@PostMapping("/{id}/reactivar")
	public UsuarioResumenDto reactivar(@PathVariable String id) {
		return servicio.reactivar(id);
	}

}

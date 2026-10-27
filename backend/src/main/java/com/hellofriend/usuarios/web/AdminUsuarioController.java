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
import com.hellofriend.usuarios.dominio.Rol;

/** Gestión de usuarios (docs/API.md > Administrador). Solo el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

	private final ServicioUsuarios servicio;

	public AdminUsuarioController(ServicioUsuarios servicio) {
		this.servicio = servicio;
	}

	/**
	 * GET /admin/usuarios?buscar=&rol=: los más antiguos primero. {@code buscar} filtra por usuario o correo y
	 * {@code rol} por rol; sin filtros devuelve todos. Es el único lugar donde se ve el correo.
	 */
	@GetMapping
	public List<UsuarioResumenDto> listar(@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Rol rol) {
		return servicio.listar(rol, buscar);
	}

	/** PUT /admin/usuarios/{id}/rol {rol}. Cierra las sesiones de ese usuario. 403 si es el propio. */
	@PutMapping("/{id}/rol")
	public UsuarioResumenDto cambiarRol(@PathVariable String id, @Valid @RequestBody SolicitudCambioRol solicitud,
			Authentication autenticacion) {
		return servicio.cambiarRol(id, solicitud.rol(), ServicioJwt.usuarioActual(autenticacion));
	}

	/** Deja la cuenta sin acceso y cierra sus sesiones. 403 si es la propia; 409 si ya está desactivada. */
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

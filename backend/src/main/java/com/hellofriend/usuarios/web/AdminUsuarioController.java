package com.hellofriend.usuarios.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
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
	private final JdbcTemplate jdbc;
	private final Clock reloj;

	public AdminUsuarioController(ServicioUsuarios servicio, JdbcTemplate jdbc, Clock reloj) {
		this.servicio = servicio;
		this.jdbc = jdbc;
		this.reloj = reloj;
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
	@Transactional
	public UsuarioResumenDto cambiarRol(@PathVariable String id, @Valid @RequestBody SolicitudCambioRol solicitud,
			Authentication autenticacion) {
		UsuarioResumenDto usuario = servicio.cambiarRol(id, solicitud.rol(), ServicioJwt.usuarioActual(autenticacion));
		auditar("ROL_ASIGNADO", ServicioJwt.usuarioActual(autenticacion), id);
		return usuario;
	}

	/** Deja la cuenta sin acceso y cierra sus sesiones. 403 si es la propia; 409 si ya está desactivada. */
	@PostMapping("/{id}/desactivar")
	@Transactional
	public UsuarioResumenDto desactivar(@PathVariable String id, Authentication autenticacion) {
		UsuarioResumenDto usuario = servicio.desactivar(id, ServicioJwt.usuarioActual(autenticacion));
		auditar("CUENTA_DESACTIVADA", ServicioJwt.usuarioActual(autenticacion), id);
		return usuario;
	}

	/** Devuelve el acceso a una cuenta desactivada. Las sesiones anteriores no reviven: hay que iniciar sesión. */
	@PostMapping("/{id}/reactivar")
	@Transactional
	public UsuarioResumenDto reactivar(@PathVariable String id, Authentication autenticacion) {
		UsuarioResumenDto usuario = servicio.reactivar(id);
		auditar("CUENTA_REACTIVADA", ServicioJwt.usuarioActual(autenticacion), id);
		return usuario;
	}

	private void auditar(String accion, String actor, String detalle) {
		Long valido = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios_usuario WHERE id=?", Long.class, actor);
		jdbc.update("INSERT INTO auditoria_evento (id, accion, actor_id, detalle, fecha) VALUES (?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), accion, valido != null && valido > 0 ? actor : null, detalle,
				LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
	}

}

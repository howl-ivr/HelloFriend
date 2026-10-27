package com.hellofriend.usuarios.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/**
 * Gestión de cuentas por el administrador: aprobar o rechazar registros, cambiar el rol, desactivar y reactivar.
 * Nadie puede cambiar su propio rol ni desactivarse, así siempre queda un administrador que pueda entrar.
 */
@Service
@Transactional
public class ServicioUsuarios {

	private final UsuarioRepositorio repositorio;

	public ServicioUsuarios(UsuarioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	/** Usuarios del rol indicado (estudiantes si es null), los más antiguos primero; con {@code estado}, solo ese. */
	@Transactional(readOnly = true)
	public List<UsuarioResumenDto> listar(Rol rol, EstadoCuenta estado) {
		Rol buscado = rol == null ? Rol.ESTUDIANTE : rol;
		List<Usuario> usuarios = estado == null
				? repositorio.findByRolOrderByCreadoEnAsc(buscado)
				: repositorio.findByRolAndEstadoOrderByCreadoEnAsc(buscado, estado);
		return usuarios.stream().map(UsuarioResumenDto::de).toList();
	}

	public UsuarioResumenDto aprobar(String id) {
		Usuario usuario = pendiente(id, "aprobar");
		usuario.aprobar();
		return UsuarioResumenDto.de(usuario);
	}

	public UsuarioResumenDto rechazar(String id) {
		Usuario usuario = pendiente(id, "rechazar");
		usuario.rechazar();
		return UsuarioResumenDto.de(usuario);
	}

	/** Cambia el rol de una cuenta aprobada y cierra sus sesiones (el token lleva el rol). Mismo rol: no hace nada. */
	public UsuarioResumenDto cambiarRol(String id, Rol rol, String actorId) {
		Usuario usuario = buscar(id);
		exigirOtraPersona(usuario, actorId, "cambiar tu propio rol");
		exigirAprobada(usuario, "cambiar el rol de");
		if (usuario.getRol() != rol) {
			usuario.asignarRol(rol);
		}
		return UsuarioResumenDto.de(usuario);
	}

	/** La cuenta deja de poder entrar y sus sesiones abiertas se cierran. */
	public UsuarioResumenDto desactivar(String id, String actorId) {
		Usuario usuario = buscar(id);
		exigirOtraPersona(usuario, actorId, "desactivar tu propia cuenta");
		exigirAprobada(usuario, "desactivar");
		if (!usuario.isActivo()) {
			throw ExcepcionNegocio.conflicto("La cuenta ya está desactivada.");
		}
		usuario.desactivar();
		return UsuarioResumenDto.de(usuario);
	}

	public UsuarioResumenDto reactivar(String id) {
		Usuario usuario = buscar(id);
		exigirAprobada(usuario, "reactivar");
		if (usuario.isActivo()) {
			throw ExcepcionNegocio.conflicto("La cuenta ya está activa.");
		}
		usuario.reactivar();
		return UsuarioResumenDto.de(usuario);
	}

	private Usuario buscar(String id) {
		return repositorio.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe."));
	}

	private Usuario pendiente(String id, String accion) {
		Usuario usuario = buscar(id);
		if (usuario.getEstado() != EstadoCuenta.PENDIENTE) {
			throw ExcepcionNegocio.conflicto("Solo se puede " + accion + " una cuenta pendiente.");
		}
		return usuario;
	}

	private static void exigirAprobada(Usuario usuario, String accion) {
		if (usuario.getEstado() != EstadoCuenta.ACTIVA) {
			throw ExcepcionNegocio.conflicto("Solo se puede " + accion + " una cuenta aprobada.");
		}
	}

	private static void exigirOtraPersona(Usuario usuario, String actorId, String accion) {
		if (usuario.getId().equals(actorId)) {
			throw ExcepcionNegocio.invalido("No puedes " + accion + ".");
		}
	}

}

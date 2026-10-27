package com.hellofriend.usuarios.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.auditoria.aplicacion.ServicioAuditoria;
import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/**
 * Gestión de cuentas por el administrador: listar, cambiar el rol, desactivar y reactivar. Nadie puede cambiar su rol
 * ni desactivarse a sí mismo (403, como pide docs/API.md): así siempre queda un administrador que pueda entrar. Cada
 * cambio deja su huella en la auditoría, dentro de la misma transacción.
 */
@Service
@Transactional
public class ServicioUsuarios {

	private final UsuarioRepositorio repositorio;
	private final ServicioAuditoria auditoria;

	public ServicioUsuarios(UsuarioRepositorio repositorio, ServicioAuditoria auditoria) {
		this.repositorio = repositorio;
		this.auditoria = auditoria;
	}

	/** Ids de las cuentas activas con ese rol (por ejemplo, los directores a quienes avisar). Para otros módulos. */
	@Transactional(readOnly = true)
	public List<String> idsActivosConRol(Rol rol) {
		return repositorio.idsActivosConRol(rol);
	}

	/**
	 * Todos los usuarios, los más antiguos primero; con {@code rol} y/o {@code buscar} (usuario o correo) se filtra.
	 */
	@Transactional(readOnly = true)
	public List<UsuarioResumenDto> listar(Rol rol, String buscar) {
		String texto = buscar == null || buscar.isBlank() ? null : UsuarioRepositorio.escaparBusqueda(buscar.strip());
		return repositorio.buscar(rol, texto).stream().map(UsuarioResumenDto::de).toList();
	}

	/** Cambia el rol y cierra las sesiones de esa persona (el token lleva el rol). Mismo rol: no hace nada. */
	public UsuarioResumenDto cambiarRol(String id, Rol rol, String actorId) {
		Usuario usuario = buscar(id);
		exigirOtraPersona(usuario, actorId, "cambiar tu propio rol");
		if (usuario.getRol() != rol) {
			usuario.asignarRol(rol);
			auditoria.registrar("ROL_ASIGNADO", actorId, id);
		}
		return UsuarioResumenDto.de(usuario);
	}

	/** La cuenta deja de poder entrar y sus sesiones abiertas se cierran. Conserva su historial. */
	public UsuarioResumenDto desactivar(String id, String actorId) {
		Usuario usuario = buscar(id);
		exigirOtraPersona(usuario, actorId, "desactivar tu propia cuenta");
		if (!usuario.isActivo()) {
			throw ExcepcionNegocio.conflicto("La cuenta ya está desactivada.");
		}
		usuario.desactivar();
		auditoria.registrar("CUENTA_DESACTIVADA", actorId, id);
		return UsuarioResumenDto.de(usuario);
	}

	public UsuarioResumenDto reactivar(String id, String actorId) {
		Usuario usuario = buscar(id);
		if (usuario.isActivo()) {
			throw ExcepcionNegocio.conflicto("La cuenta ya está activa.");
		}
		usuario.reactivar();
		auditoria.registrar("CUENTA_REACTIVADA", actorId, id);
		return UsuarioResumenDto.de(usuario);
	}

	private Usuario buscar(String id) {
		return repositorio.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe."));
	}

	private static void exigirOtraPersona(Usuario usuario, String actorId, String accion) {
		if (usuario.getId().equals(actorId)) {
			throw ExcepcionNegocio.prohibido("No puedes " + accion + ".");
		}
	}

}

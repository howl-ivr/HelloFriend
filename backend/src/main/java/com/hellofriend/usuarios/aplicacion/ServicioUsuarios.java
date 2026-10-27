package com.hellofriend.usuarios.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** Gestión de cuentas por el administrador: revisar las solicitudes de registro y aprobarlas o rechazarlas. */
@Service
@Transactional
public class ServicioUsuarios {

	private final UsuarioRepositorio repositorio;

	public ServicioUsuarios(UsuarioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	/** Estudiantes, los más antiguos primero. Con {@code estado} solo los de ese estado. */
	@Transactional(readOnly = true)
	public List<UsuarioResumenDto> listarEstudiantes(EstadoCuenta estado) {
		List<Usuario> usuarios = estado == null
				? repositorio.findByRolOrderByCreadoEnAsc(Rol.ESTUDIANTE)
				: repositorio.findByRolAndEstadoOrderByCreadoEnAsc(Rol.ESTUDIANTE, estado);
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

	private Usuario pendiente(String id, String accion) {
		Usuario usuario = repositorio.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe."));
		if (usuario.getEstado() != EstadoCuenta.PENDIENTE) {
			throw ExcepcionNegocio.conflicto("Solo se puede " + accion + " una cuenta pendiente.");
		}
		return usuario;
	}

}

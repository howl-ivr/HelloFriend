package com.hellofriend.usuarios.aplicacion;

import java.time.LocalDateTime;

import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/**
 * Fila de la lista del administrador (docs/API.md), sin correo. {@code activo} es un campo extra: indica si una cuenta
 * aprobada fue desactivada.
 */
public record UsuarioResumenDto(String id, String usuario, Rol rol, EstadoCuenta estado, boolean activo,
		LocalDateTime fechaRegistro) {

	public static UsuarioResumenDto de(Usuario u) {
		return new UsuarioResumenDto(u.getId(), u.getUsuario(), u.getRol(), u.getEstado(), u.isActivo(),
				u.getCreadoEn());
	}

}

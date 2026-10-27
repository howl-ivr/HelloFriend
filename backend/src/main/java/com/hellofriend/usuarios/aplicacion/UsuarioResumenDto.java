package com.hellofriend.usuarios.aplicacion;

import java.time.LocalDateTime;

import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/** Fila de la gestión de usuarios (docs/API.md). Solo el ADMIN la recibe, y es el único que ve el correo. */
public record UsuarioResumenDto(String id, String usuario, String correo, Rol rol, EstadoCuenta estado,
		LocalDateTime fechaRegistro) {

	public static UsuarioResumenDto de(Usuario u) {
		return new UsuarioResumenDto(u.getId(), u.getUsuario(), u.getCorreo(), u.getRol(), u.getEstado(),
				u.getCreadoEn());
	}

}

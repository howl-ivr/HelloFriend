package com.hellofriend.usuarios.aplicacion;

import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/** Quién es el usuario autenticado. Sin correo (regla de docs/API.md) y sin datos de seguridad. */
public record PerfilDto(String usuario, Rol rol) {

	public static PerfilDto de(Usuario u) {
		return new PerfilDto(u.getUsuario(), u.getRol());
	}

}

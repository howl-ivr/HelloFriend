package com.hellofriend.usuarios.aplicacion;

import java.nio.charset.StandardCharsets;

import com.hellofriend.compartido.web.ExcepcionNegocio;

/** Reglas de la contraseña que no caben en una anotación de validación. */
final class ReglasContrasena {

	/** BCrypt solo mira los primeros 72 bytes de la contraseña. */
	private static final int MAX_BYTES = 72;

	private ReglasContrasena() {
	}

	static void exigirLargoSoportado(String contrasena) {
		if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			throw ExcepcionNegocio.invalido("La contraseña es demasiado larga.");
		}
	}

}

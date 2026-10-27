package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token del enlace del correo y la contraseña nueva. Ambos son secretos: no salen en un toString. */
public record SolicitudRestablecer(@NotBlank(message = "El enlace de recuperación no es válido.") String token,
		@NotBlank(message = "Ingresa una contraseña.") @Size(min = 6, max = 64, message = "La contraseña debe tener de 6 a 64 caracteres.") String password) {

	@Override
	public String toString() {
		return "SolicitudRestablecer[]";
	}

}

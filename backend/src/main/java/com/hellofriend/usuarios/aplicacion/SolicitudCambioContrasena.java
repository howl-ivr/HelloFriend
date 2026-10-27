package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña estando con sesión iniciada. Ambas son secretos: no salen en un toString. */
public record SolicitudCambioContrasena(
		@NotBlank(message = "Ingresa tu contraseña actual.") String contrasenaActual,
		@NotBlank(message = "Ingresa la nueva contraseña.") @Size(min = 6, max = 64, message = "La contraseña debe tener de 6 a 64 caracteres.") String contrasenaNueva) {

	@Override
	public String toString() {
		return "SolicitudCambioContrasena[]";
	}

}

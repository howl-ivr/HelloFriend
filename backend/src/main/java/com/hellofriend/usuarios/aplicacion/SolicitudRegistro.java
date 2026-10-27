package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del registro (reglas de docs/API.md). La contraseña nunca se imprime ni se devuelve. */
public record SolicitudRegistro(
		@NotBlank(message = "Ingresa tu correo.") @Pattern(regexp = "^[^@\\s]+@pucp\\.edu\\.pe$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "El correo debe terminar en @pucp.edu.pe.") String correo,
		@NotBlank(message = "Ingresa un usuario.") @Pattern(regexp = "^[a-zA-Z0-9._-]{3,15}$", message = "El usuario debe tener de 3 a 15 caracteres: letras, números, punto, guion o guion bajo.") String usuario,
		@NotBlank(message = "Ingresa una contraseña.") @Size(min = 6, max = 64, message = "La contraseña debe tener de 6 a 64 caracteres.") String password) {

	@Override
	public String toString() {
		return "SolicitudRegistro[usuario=" + usuario + "]";
	}

}

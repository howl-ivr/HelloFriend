package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;

/** Credenciales del login (campos del contrato docs/API.md). Se entra con el nombre de usuario, no con el correo. */
public record SolicitudLogin(@NotBlank(message = "Ingresa tu usuario.") String usuario,
		@NotBlank(message = "Ingresa tu contraseña.") String password) {

	/** Evita que la contraseña aparezca en un log si alguien imprime la solicitud. */
	@Override
	public String toString() {
		return "SolicitudLogin[usuario=" + usuario + "]";
	}

}

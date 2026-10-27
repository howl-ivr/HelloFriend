package com.hellofriend.compartido.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Impide arrancar con un secreto de JWT inseguro. El valor por defecto de application.properties es público (está en
 * el repositorio) y sirve solo para desarrollo: si la base de datos no es local, se exige un {@code JWT_SECRET} propio,
 * porque con el secreto público cualquiera podría fabricar un token de administrador.
 */
final class ValidadorSecretoJwt {

	/** Debe ser igual al valor por defecto de {@code hellofriend.jwt.secreto} (lo comprueba una prueba). */
	static final String SECRETO_DESARROLLO = "desarrollo-local-cambiar-este-secreto-de-32+caracteres";

	private static final int MIN_BYTES = 32;

	/** Solo localhost, 127.0.0.1 o ::1 como host; "localhost.evil.com" o "?x=localhost" no cuentan. */
	private static final Pattern BASE_LOCAL = Pattern.compile(
			"^jdbc:mysql://(localhost|127\\.0\\.0\\.1|\\[::1\\])([:/?].*)?$",
			Pattern.CASE_INSENSITIVE);

	private ValidadorSecretoJwt() {
	}

	/** Lanza {@link IllegalStateException} sin incluir el secreto en el mensaje. */
	static void exigirSeguro(String secreto, String urlBaseDeDatos) {
		if (secreto.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
			throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_BYTES + " bytes.");
		}
		if (SECRETO_DESARROLLO.equals(secreto) && !esBaseLocal(urlBaseDeDatos)) {
			throw new IllegalStateException(
					"JWT_SECRET no está definido: se está usando el secreto público de desarrollo "
							+ "con una base de datos que no es local. Define JWT_SECRET con un valor propio, largo y aleatorio.");
		}
	}

	static boolean esBaseLocal(String urlBaseDeDatos) {
		return urlBaseDeDatos != null && BASE_LOCAL.matcher(urlBaseDeDatos).matches();
	}

}

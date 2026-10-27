package com.hellofriend.usuarios.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de recuperación de contraseña: 32 bytes aleatorios (256 bits) que viajan en el enlace del correo. En la base
 * solo se guarda su SHA-256: con tanta entropía no hace falta un hash lento, y quien lea la tabla no puede usar los
 * enlaces pendientes.
 */
public final class TokenRecuperacion {

	private static final SecureRandom AZAR = new SecureRandom();

	private TokenRecuperacion() {
	}

	/** Token nuevo, seguro para ir en una URL (Base64 sin relleno, 43 caracteres). */
	public static String generar() {
		byte[] bytes = new byte[32];
		AZAR.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256 en hexadecimal (64 caracteres), lo que se guarda en token_recuperacion_hash. */
	public static String hash(String token) {
		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible", e);
		}
	}

}

package com.hellofriend.compartido.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite el token del login. Claims: "sub" = id del usuario, "usuario", "rol" (los tres que pide docs/API.md), "ver" =
 * versión de sesión con la que se emitió ({@link VerificadorSesion}), más emisor y vigencia.
 */
@Service
public class ServicioJwt {

	static final String EMISOR = "hellofriend";
	static final String CLAIM_USUARIO = "usuario";
	static final String CLAIM_ROL = "rol";
	static final String CLAIM_VERSION = "ver";

	private final JwtEncoder codificador;
	private final Clock reloj;
	private final Duration vigencia;

	public ServicioJwt(JwtEncoder codificador, Clock reloj, @Value("${hellofriend.jwt.duracion-horas}") long horas) {
		this.codificador = codificador;
		this.reloj = reloj;
		this.vigencia = Duration.ofHours(horas);
	}

	public String emitir(String usuarioId, String nombreUsuario, String rol, int versionSesion) {
		Instant ahora = reloj.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(EMISOR).issuedAt(ahora).expiresAt(ahora.plus(vigencia))
				.subject(usuarioId).claim(CLAIM_USUARIO, nombreUsuario).claim(CLAIM_ROL, rol)
				.claim(CLAIM_VERSION, versionSesion).build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}

	/** Id del usuario autenticado en la petición actual (el "sub" del token). */
	public static String usuarioActual(Authentication autenticacion) {
		return autenticacion.getName();
	}

}

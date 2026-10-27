package com.hellofriend.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Emisión del token: contenido, vigencia y rechazo de firmas ajenas. Sin Spring ni base de datos. */
class ServicioJwtTests {

	private static final String ID = "7c1a2b9e-0000-0000-0000-000000000001";

	private static SecretKey clave(String secreto) {
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	private static final SecretKey CLAVE = clave("secreto-de-pruebas-con-mas-de-32-caracteres");

	private static ServicioJwt servicio(SecretKey clave, Clock reloj, long horas) {
		return new ServicioJwt(new NimbusJwtEncoder(new ImmutableSecret<>(clave)), reloj, horas);
	}

	private static JwtDecoder decodificador(SecretKey clave) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256)
				.build();
		decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(ServicioJwt.EMISOR));
		return decodificador;
	}

	@Test
	void elTokenLlevaIdRolVersionYVigencia() {
		Clock ahora = Clock.fixed(Instant.now(), ZoneOffset.UTC);

		Jwt jwt = decodificador(CLAVE).decode(servicio(CLAVE, ahora, 8).emitir(ID, "ESTUDIANTE", 3));

		assertThat(jwt.getSubject()).isEqualTo(ID);
		assertThat(jwt.<String>getClaim("rol")).isEqualTo("ESTUDIANTE");
		assertThat(((Number) jwt.getClaim("ver")).intValue()).isEqualTo(3);
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("hellofriend");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
	}

	@Test
	void rechazaUnTokenFirmadoConOtraClave() {
		String ajeno = servicio(clave("otro-secreto-distinto-con-mas-de-32-caracteres"), Clock.systemUTC(), 8)
				.emitir(ID, "ADMINISTRADOR", 0);

		assertThatThrownBy(() -> decodificador(CLAVE).decode(ajeno)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaUnTokenVencido() {
		Clock haceDiezHoras = Clock.fixed(Instant.now().minus(Duration.ofHours(10)), ZoneOffset.UTC);
		String vencido = servicio(CLAVE, haceDiezHoras, 8).emitir(ID, "ESTUDIANTE", 0);

		assertThatThrownBy(() -> decodificador(CLAVE).decode(vencido)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaUnTokenManipulado() {
		String token = servicio(CLAVE, Clock.systemUTC(), 8).emitir(ID, "ESTUDIANTE", 0);
		String[] partes = token.split("\\.");
		String carga = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(
						"{\"sub\":\"otro\",\"rol\":\"ADMINISTRADOR\",\"ver\":0}".getBytes(StandardCharsets.UTF_8));
		String falsificado = partes[0] + "." + carga + "." + partes[2];

		assertThatThrownBy(() -> decodificador(CLAVE).decode(falsificado)).isInstanceOf(JwtException.class);
	}

}

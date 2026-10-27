package com.hellofriend.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/** El secreto público de desarrollo solo se acepta con una base de datos local. */
class ValidadorSecretoJwtTests {

	private static final String PROPIO = "un-secreto-propio-largo-y-aleatorio-9f3k2m1x7q";
	private static final String RDS = "jdbc:mysql://mi-base.abc123.us-east-1.rds.amazonaws.com:3306/hellofriend";

	@ParameterizedTest
	@ValueSource(strings = {"jdbc:mysql://localhost:3306/hellofriend?characterEncoding=UTF-8",
			"jdbc:mysql://localhost:3307/hellofriend", "jdbc:mysql://127.0.0.1:3306/hellofriend",
			"jdbc:mysql://[::1]:3306/hellofriend", "jdbc:mysql://localhost/hellofriend",
			"JDBC:MYSQL://LOCALHOST:3306/hellofriend"})
	void elSecretoDeDesarrolloSirveConUnaBaseLocal(String url) {
		assertThatCode(() -> ValidadorSecretoJwt.exigirSeguro(ValidadorSecretoJwt.SECRETO_DESARROLLO, url))
				.doesNotThrowAnyException();
	}

	@Test
	void elSecretoDeDesarrolloNoSirveConUnaBaseQueNoEsLocal() {
		assertThatThrownBy(() -> ValidadorSecretoJwt.exigirSeguro(ValidadorSecretoJwt.SECRETO_DESARROLLO, RDS))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET")
				.hasMessageNotContaining(ValidadorSecretoJwt.SECRETO_DESARROLLO);
	}

	@ParameterizedTest
	@ValueSource(strings = {"jdbc:mysql://localhost.evil.com:3306/hellofriend",
			"jdbc:mysql://localhost@evil.com/hellofriend",
			"jdbc:mysql://evil.com/hellofriend?host=localhost", "jdbc:mysql://mysql:3306/hellofriend", ""})
	void unHostQueSoloPareceLocalNoCuenta(String url) {
		assertThat(ValidadorSecretoJwt.esBaseLocal(url)).isFalse();
		assertThatThrownBy(() -> ValidadorSecretoJwt.exigirSeguro(ValidadorSecretoJwt.SECRETO_DESARROLLO, url))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void unaUrlNulaNoEsLocal() {
		assertThat(ValidadorSecretoJwt.esBaseLocal(null)).isFalse();
	}

	@Test
	void unSecretoPropioSirveConCualquierBase() {
		assertThatCode(() -> ValidadorSecretoJwt.exigirSeguro(PROPIO, RDS)).doesNotThrowAnyException();
		assertThatCode(() -> ValidadorSecretoJwt.exigirSeguro(PROPIO, "jdbc:mysql://localhost:3306/hellofriend"))
				.doesNotThrowAnyException();
	}

	@Test
	void rechazaUnSecretoDeMenosDe32Bytes() {
		assertThatThrownBy(() -> ValidadorSecretoJwt.exigirSeguro("corto", "jdbc:mysql://localhost:3306/hellofriend"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
	}

	@Test
	void elValorPorDefectoDeApplicationPropertiesEsElQueElValidadorConsideraPublico() throws IOException {
		String propiedades = new ClassPathResource("application.properties").getContentAsString(StandardCharsets.UTF_8);

		assertThat(propiedades).contains("${JWT_SECRET:" + ValidadorSecretoJwt.SECRETO_DESARROLLO + "}");
	}

}

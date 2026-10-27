package com.hellofriend.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenRecuperacionTests {

	@Test
	void generaTokensDe43CaracteresSegurosParaUnaUrl() {
		String token = TokenRecuperacion.generar();

		assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void dosTokensNuncaSeRepiten() {
		assertThat(TokenRecuperacion.generar()).isNotEqualTo(TokenRecuperacion.generar());
	}

	@Test
	void elHashEsDeterministaDe64CaracteresYDistintoDelToken() {
		String token = TokenRecuperacion.generar();

		assertThat(TokenRecuperacion.hash(token)).isEqualTo(TokenRecuperacion.hash(token)).hasSize(64)
				.matches("[0-9a-f]+").isNotEqualTo(token);
		assertThat(TokenRecuperacion.hash(token)).isNotEqualTo(TokenRecuperacion.hash(token + "x"));
	}

}

package com.hellofriend;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class SoloBaseLocalTests {

	private static final String URL_LOCAL = "jdbc:mysql://localhost:3306/hellofriend_test?connectionTimeZone=UTC";

	@Test
	void aceptaBaseDePruebasLocal() {
		var contexto = contexto(URL_LOCAL, null);
		assertThatCode(() -> new PruebaIntegracion.SoloBaseLocal().initialize(contexto)).doesNotThrowAnyException();
	}

	@Test
	void rechazaBaseRemotaAntesDeArrancarFlyway() {
		var contexto = contexto("jdbc:mysql://servidor.example:3306/hellofriend_test", null);
		assertThatThrownBy(() -> new PruebaIntegracion.SoloBaseLocal().initialize(contexto))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.datasource.url");
	}

	@Test
	void rechazaBaseLocalQueNoEsDePruebas() {
		var contexto = contexto("jdbc:mysql://localhost:3306/hellofriend", null);
		assertThatThrownBy(() -> new PruebaIntegracion.SoloBaseLocal().initialize(contexto))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.datasource.url");
	}

	@Test
	void rechazaFlywayRemotoAunqueDatasourceSeaLocal() {
		var contexto = contexto(URL_LOCAL, "jdbc:mysql://servidor.example:3306/hellofriend_test");
		assertThatThrownBy(() -> new PruebaIntegracion.SoloBaseLocal().initialize(contexto))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.flyway.url");
	}

	private static GenericApplicationContext contexto(String datasource, String flyway) {
		var entorno = new MockEnvironment().withProperty("spring.datasource.url", datasource);
		if (flyway != null) {
			entorno.withProperty("spring.flyway.url", flyway);
		}
		var contexto = new GenericApplicationContext();
		contexto.setEnvironment(entorno);
		return contexto;
	}
}

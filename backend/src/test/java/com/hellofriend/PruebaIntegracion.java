package com.hellofriend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas de integracion: arranca la aplicacion contra MySQL ({@code hellofriend_test}, Flyway aplica las
 * migraciones) y deshace cada prueba al terminar. Antes de arrancar Flyway y antes de cada prueba comprueba que la
 * conexion sea LOCAL y a una base cuyo nombre termine en {@code _test}, para no tocar la base de desarrollo ni RDS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = PruebaIntegracion.SoloBaseLocal.class)
@Transactional
public abstract class PruebaIntegracion {

	/** Detiene el arranque antes de Flyway si una propiedad intenta apuntar fuera de MySQL local _test. */
	public static final class SoloBaseLocal implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		private static final Pattern URL_LOCAL = Pattern.compile(
				"^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/[^/?]+_test(?:\\?.*)?$",
				Pattern.CASE_INSENSITIVE);

		@Override
		public void initialize(ConfigurableApplicationContext contexto) {
			validar("spring.datasource.url", contexto.getEnvironment().getProperty("spring.datasource.url"));
			String flyway = contexto.getEnvironment().getProperty("spring.flyway.url");
			if (flyway != null && !flyway.isBlank()) {
				validar("spring.flyway.url", flyway);
			}
		}

		private static void validar(String propiedad, String url) {
			if (url == null || !URL_LOCAL.matcher(url).matches()) {
				throw new IllegalStateException(propiedad
						+ " debe apuntar a MySQL local y a una base terminada en _test; configura TEST_DB_URL.");
			}
		}
	}

	@Autowired
	protected JdbcClient jdbc;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void soloBaseDePruebasLocal() throws Exception {
		try (var conexion = dataSource.getConnection()) {
			String url = conexion.getMetaData().getURL();
			assertThat(url).as("Las pruebas solo corren contra un MySQL local")
					.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1)[:/].*");
			assertThat(conexion.getCatalog()).as("La base de pruebas debe terminar en _test").endsWith("_test");
		}
	}

	/** Exige que la BD rechace la operacion por la restriccion indicada (y no por otro motivo). */
	protected void rechazaPor(String restriccion, Executable operacion) {
		assertThatThrownBy(operacion::execute).isInstanceOf(DataAccessException.class)
				.hasMessageContaining(restriccion);
	}

}

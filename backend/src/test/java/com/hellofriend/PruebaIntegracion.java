package com.hellofriend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas de integracion: arranca la aplicacion contra MySQL ({@code hellofriend_test}, Flyway aplica las
 * migraciones) y deshace cada prueba al terminar. Antes de cada prueba comprueba que la conexion sea LOCAL y a una base
 * cuyo nombre termine en {@code _test}, para no tocar nunca la base de desarrollo ni RDS.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class PruebaIntegracion {

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
		assertThatThrownBy(operacion::execute).isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining(restriccion);
	}

}

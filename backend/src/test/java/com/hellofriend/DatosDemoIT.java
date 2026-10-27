package com.hellofriend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** El script de datos de demostracion (db/dev) debe ser valido, repetible y dejar entrar a cada rol. */
class DatosDemoIT extends PruebaIntegracion {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private MockMvc mvc;

	private void cargar() {
		ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource),
				new ClassPathResource("db/dev/R__datos_demo.sql"));
	}

	@ParameterizedTest
	@CsvSource({"admin,ADMINISTRADOR", "director,DIRECTOR", "profesor,PROFESOR", "ana,ESTUDIANTE",
			"luis,ESTUDIANTE"})
	void cadaCuentaDeDemostracionPuedeIniciarSesion(String usuario, String rol) throws Exception {
		cargar();
		mvc.perform(MockMvcRequestBuilders.post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"" + usuario + "\",\"password\":\"Prueba123\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value(rol));
	}

	@Test
	void sePuedeEjecutarDosVecesSinDuplicar() {
		cargar();
		cargar();
		assertThat(jdbc.sql("SELECT COUNT(*) FROM incidencias_incidencia WHERE codigo_seguimiento LIKE 'DEMO-%'")
				.query(Integer.class).single()).isEqualTo(3);
		assertThat(jdbc.sql("SELECT COUNT(*) FROM usuarios_usuario WHERE correo LIKE '%@pucp.edu.pe'")
				.query(Integer.class).single()).isGreaterThanOrEqualTo(5);
	}

}

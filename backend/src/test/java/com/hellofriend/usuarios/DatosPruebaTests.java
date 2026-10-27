package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Los usuarios demo (db/datos-prueba) deben poder entrar con la clave que documenta el README. */
class DatosPruebaTests {

	private static final String CLAVE_DEMO = "Prueba#2026";

	@Test
	void todosLosHashesDeLosUsuariosDemoCorrespondenALaClaveDocumentada() throws IOException {
		String sql = new ClassPathResource("db/datos-prueba/R__datos_prueba.sql")
				.getContentAsString(StandardCharsets.UTF_8);

		var hashes = Pattern.compile("\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}").matcher(sql).results()
				.map(r -> r.group()).distinct().toList();

		assertThat(hashes).isNotEmpty();
		var codificador = new BCryptPasswordEncoder();
		assertThat(hashes).allMatch(hash -> codificador.matches(CLAVE_DEMO, hash));
	}

}

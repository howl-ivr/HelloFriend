package com.hellofriend.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import com.hellofriend.academico.aplicacion.CursoSolicitud;

/** Validaciones de los datos de un curso, sin Spring ni base de datos. */
class CursoSolicitudTests {

	private List<String> errores(String codigo, String nombre) {
		try (var fabrica = Validation.buildDefaultValidatorFactory()) {
			return fabrica.getValidator().validate(new CursoSolicitud(codigo, nombre)).stream()
					.map(ConstraintViolation::getMessage).sorted().toList();
		}
	}

	@Test
	void aceptaCodigoYNombreValidos() {
		assertThat(errores("1INF36", "Ética y RSU")).isEmpty();
	}

	@Test
	void codigoEsObligatorio() {
		assertThat(errores(" ", "Algoritmia")).contains("El código es obligatorio.");
		assertThat(errores(null, "Algoritmia")).contains("El código es obligatorio.");
	}

	@Test
	void codigoDebeTenerDe3A10LetrasONumeros() {
		String mensaje = "El código debe tener de 3 a 10 letras o números.";
		assertThat(errores("AB", "Algoritmia")).containsExactly(mensaje);
		assertThat(errores("ABCDEFGHIJK", "Algoritmia")).containsExactly(mensaje);
		assertThat(errores("INF-263", "Algoritmia")).containsExactly(mensaje);
		assertThat(errores("ABC", "Algoritmia")).isEmpty();
		assertThat(errores("ABCDEFGHIJ", "Algoritmia")).isEmpty();
	}

	@Test
	void nombreEsObligatorioYTieneMaximo() {
		assertThat(errores("INF263", "  ")).containsExactly("El nombre es obligatorio.");
		assertThat(errores("INF263", "x".repeat(151)))
				.containsExactly("El nombre no puede superar los 150 caracteres.");
		assertThat(errores("INF263", "x".repeat(150))).isEmpty();
	}

}

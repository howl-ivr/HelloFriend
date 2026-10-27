package com.hellofriend.notificaciones.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** El recorte de títulos y mensajes para que quepan en las columnas de la tabla. */
class EmisorNotificacionesTests {

	@Test
	void unTextoQueCabeSeDejaIgualSinEspaciosDeLosExtremos() {
		assertThat(EmisorNotificaciones.recortar("  Tu incidencia fue aprobada  ", 500))
				.isEqualTo("Tu incidencia fue aprobada");
	}

	@Test
	void unTextoDeExactamenteElMaximoNoSeToca() {
		String texto = "a".repeat(150);

		assertThat(EmisorNotificaciones.recortar(texto, 150)).isEqualTo(texto);
	}

	@Test
	void unTextoMasLargoSeCortaYTerminaEnPuntosSuspensivosSinPasarElMaximo() {
		String recortado = EmisorNotificaciones.recortar("a".repeat(600), 500);

		assertThat(recortado).hasSize(500).endsWith("…");
	}

	@Test
	void alCortarNoQuedaUnEspacioAntesDelPuntoSuspensivo() {
		String texto = "a".repeat(8) + " " + "b".repeat(20);

		assertThat(EmisorNotificaciones.recortar(texto, 10)).isEqualTo("aaaaaaaa…");
	}

	@Test
	void unTextoNuloSeRechaza() {
		assertThatThrownBy(() -> EmisorNotificaciones.recortar(null, 10)).isInstanceOf(NullPointerException.class);
	}

}

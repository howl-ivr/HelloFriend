package com.hellofriend.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RelojConfigTests {

	@Test
	void usaLaZonaConfigurada() {
		Clock reloj = new RelojConfig().reloj("America/Lima");

		assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Lima"));
	}

	@Test
	void limaVaCincoHorasAtrasDeUtc() {
		Clock reloj = new RelojConfig().reloj("America/Lima");

		var offset = reloj.getZone().getRules().getOffset(reloj.instant());
		assertThat(offset.getTotalSeconds()).isEqualTo(-5 * 3600);
	}

}

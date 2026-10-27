package com.hellofriend.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación con la zona del negocio (America/Lima). La fecha y hora actuales se obtienen siempre de este
 * bean, nunca con {@code LocalDate.now()} suelto. En la base de datos las fechas se guardan en UTC.
 */
@Configuration
public class RelojConfig {

	@Bean
	public Clock reloj(@Value("${hellofriend.zona-horaria:America/Lima}") String zona) {
		return Clock.system(ZoneId.of(zona));
	}

}

package com.hellofriend.incidencias.dominio;

import java.time.LocalDateTime;

/** Una línea del historial de estados, con quien hizo el cambio. {@code tipoEvento} puede ser null (filas de V2). */
public record EventoHistorial(String tipoEvento, String estadoNuevo, LocalDateTime fecha, String detalle,
		String usuario) {
}

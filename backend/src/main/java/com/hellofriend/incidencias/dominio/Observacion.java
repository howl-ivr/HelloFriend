package com.hellofriend.incidencias.dominio;

import java.time.LocalDateTime;

/** Observación del director sobre la versión indicada de una incidencia. */
public record Observacion(String texto, LocalDateTime fecha, int version) {
}

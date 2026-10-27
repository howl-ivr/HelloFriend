package com.hellofriend.incidencias.dominio;

import java.time.LocalDateTime;

/** Una versión del contenido de la incidencia; cada edición crea la siguiente (solo de adición). */
public record VersionIncidencia(int n, String titulo, String descripcion, String categoria, LocalDateTime fecha) {
}

package com.hellofriend.incidencias.dominio;

import java.time.LocalDateTime;

/** Comentario elegido de la lista predefinida; no se escribe texto libre. */
public record ComentarioIncidencia(String id, String texto, LocalDateTime fecha) {
}

package com.hellofriend.incidencias.aplicacion;

/** Motivo de rechazo que el director puede elegir (docs/API.md > GET /motivos-rechazo). */
public record MotivoRechazoDto(String id, String descripcion) {
}

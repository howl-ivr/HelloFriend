package com.hellofriend.incidencias.aplicacion;

import java.time.LocalDateTime;

import com.hellofriend.incidencias.aplicacion.IncidenciaDto.CanalResumen;

/** Fila de la lista de reportes del administrador (docs/API.md > GET /admin/reportes). Incluye el autor. */
public record ReporteFilaDto(String id, String titulo, String categoria, String estado, LocalDateTime fechaCreacion,
		CanalResumen canal, int confirmaciones, String prioridad, String autor, int versiones) {
}

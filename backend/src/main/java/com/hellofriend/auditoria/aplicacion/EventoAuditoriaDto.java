package com.hellofriend.auditoria.aplicacion;

import java.time.LocalDateTime;

/** Fila de la auditoría general (docs/API.md > GET /admin/auditoria). Sin actor, la acción es del "Sistema". */
public record EventoAuditoriaDto(String id, String accion, String actor, String incidenciaId, String detalle,
		LocalDateTime fecha) {
}

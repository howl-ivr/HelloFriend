package com.hellofriend.incidencias.aplicacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hellofriend.incidencias.dominio.ComentarioIncidencia;
import com.hellofriend.incidencias.dominio.Observacion;
import com.hellofriend.incidencias.dominio.VersionIncidencia;

/**
 * Incidencia tal como la ve quien la pide (docs/API.md > Incidencia). Los campos que dependen del rol van en null
 * cuando no corresponde (por ejemplo {@code autor}, que solo reciben el director y el administrador). Los cinco
 * últimos solo salen en el detalle, y solo si la persona puede verlos.
 */
public record IncidenciaDto(String id, String titulo, String descripcion, String categoria, String estado,
		LocalDateTime fechaCreacion, CanalResumen canal, Autor autor, int confirmaciones, String prioridad,
		boolean confirmadaPorMi, Map<String, Integer> reacciones, String miReaccion, long totalComentarios,
		boolean esMia, boolean puedeConfirmar, boolean puedeReaccionar, boolean puedeComentar, boolean puedeEditar,
		boolean puedeReenviar, Revision revision, String observacion, String solucion, LocalDateTime fechaResolucion,
		@JsonInclude(Include.NON_NULL) List<Observacion> observaciones,
		@JsonInclude(Include.NON_NULL) List<VersionIncidencia> versiones,
		@JsonInclude(Include.NON_NULL) List<Evento> historial,
		@JsonInclude(Include.NON_NULL) List<ComentarioIncidencia> comentarios,
		@JsonInclude(Include.NON_NULL) List<Evento> eventos) {

	public record CanalResumen(String id, String nombre, String periodo) {
	}

	/** El correo solo viaja en el reporte del administrador. */
	public record Autor(String usuario, @JsonInclude(Include.NON_NULL) String correo) {
	}

	public record Revision(String decision, String motivo, String comentario, LocalDateTime fecha) {
	}

	/** Línea del historial o de la auditoría de una incidencia. */
	public record Evento(String tipo, String estado, LocalDateTime fecha, String detalle, String responsable) {
	}

	/** El mismo objeto, con el autor completo y los eventos de auditoría: es el reporte del administrador. */
	public IncidenciaDto comoReporte(Autor autorCompleto, List<Evento> eventosDeAuditoria) {
		return new IncidenciaDto(id, titulo, descripcion, categoria, estado, fechaCreacion, canal, autorCompleto,
				confirmaciones, prioridad, confirmadaPorMi, reacciones, miReaccion, totalComentarios, esMia,
				puedeConfirmar, puedeReaccionar, puedeComentar, puedeEditar, puedeReenviar, revision, observacion,
				solucion, fechaResolucion, observaciones, versiones, historial, comentarios, eventosDeAuditoria);
	}

}

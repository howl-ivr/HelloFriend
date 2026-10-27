package com.hellofriend.canales.aplicacion;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Canal tal como lo espera el frontend (docs/API.md). {@code totalPendientes} y {@code fechaCreacion} solo los recibe
 * el administrador: para los demás roles no salen en el JSON.
 */
public record CanalDto(String id, String nombre, String descripcion, String periodo, boolean activo,
		CursoResumenDto curso, boolean suscrito, long totalSuscritos, long totalPublicadas,
		@JsonInclude(Include.NON_NULL) Long totalPendientes,
		@JsonInclude(Include.NON_NULL) LocalDateTime fechaCreacion) {

	/** Curso al que pertenece el canal. */
	public record CursoResumenDto(String id, String codigo, String nombre) {
	}

}

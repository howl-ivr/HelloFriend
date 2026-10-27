package com.hellofriend.incidencias.aplicacion;

import java.util.List;

/** Panel del director (docs/API.md > GET /director/estadisticas). */
public record EstadisticasDto(int total, int resueltas, Double tiempoPromedioHoras, List<ConteoPorNombre> porCurso,
		List<ConteoPorNombre> porCategoria, List<ConteoPorEstado> porEstado, List<Destacada> masConfirmadas) {

	public record ConteoPorNombre(String nombre, long total) {
	}

	public record ConteoPorEstado(String estado, long total) {
	}

	/** Incidencia con confirmaciones, para el panel "lo más confirmado". */
	public record Destacada(String id, String titulo, String estado, String canal, int confirmaciones,
			String prioridad) {
	}

}

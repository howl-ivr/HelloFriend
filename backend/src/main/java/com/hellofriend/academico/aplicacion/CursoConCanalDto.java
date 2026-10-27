package com.hellofriend.academico.aplicacion;

import java.time.LocalDateTime;

/**
 * Curso del listado de administración (docs/API.md > GET /admin/cursos). {@code tieneCanal} indica si ya tiene canal
 * en el periodo consultado; sin periodo es siempre falso.
 */
public record CursoConCanalDto(String id, String codigo, String nombre, LocalDateTime creadoEn, boolean tieneCanal) {

	public static CursoConCanalDto de(CursoDto curso, boolean tieneCanal) {
		return new CursoConCanalDto(curso.id(), curso.codigo(), curso.nombre(), curso.creadoEn(), tieneCanal);
	}

}

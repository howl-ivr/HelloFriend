package com.hellofriend.academico.aplicacion;

import java.time.LocalDateTime;

import com.hellofriend.academico.dominio.Curso;

/** Curso tal como lo ve el cliente. {@code creadoEn} esta en UTC. */
public record CursoDto(String id, String codigo, String nombre, LocalDateTime creadoEn) {

	static CursoDto de(Curso curso) {
		return new CursoDto(curso.getId(), curso.getCodigo(), curso.getNombre(), curso.getCreadoEn());
	}

}

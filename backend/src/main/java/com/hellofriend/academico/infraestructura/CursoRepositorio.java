package com.hellofriend.academico.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hellofriend.academico.dominio.Curso;

public interface CursoRepositorio extends JpaRepository<Curso, String> {

	boolean existsByCodigo(String codigo);

	boolean existsByCodigoAndIdNot(String codigo, String id);

	List<Curso> findAllByOrderByCodigoAsc();

	/** Busca por parte del nombre. La colacion de la BD ignora mayusculas y tildes ("etica" encuentra "Ética"). */
	List<Curso> findByNombreContainingOrderByCodigoAsc(String texto);

}

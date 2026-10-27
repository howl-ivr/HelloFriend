package com.hellofriend.academico.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.academico.dominio.Curso;
import com.hellofriend.academico.infraestructura.CursoRepositorio;
import com.hellofriend.compartido.web.ExcepcionNegocio;

/** Casos de uso del catalogo de cursos: crear, modificar, eliminar y consultar. */
@Service
@Transactional
public class CursoServicio {

	private final CursoRepositorio repositorio;
	private final Clock reloj;

	public CursoServicio(CursoRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	public CursoDto crear(CursoSolicitud solicitud) {
		String codigo = codigoNormalizado(solicitud);
		if (repositorio.existsByCodigo(codigo)) {
			throw ExcepcionNegocio.conflicto("Ya existe un curso con el código " + codigo + ".");
		}
		LocalDateTime creadoEn = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
		return CursoDto.de(repositorio.saveAndFlush(new Curso(codigo, solicitud.nombre().trim(), creadoEn)));
	}

	public CursoDto modificar(String id, CursoSolicitud solicitud) {
		Curso curso = buscar(id);
		String codigo = codigoNormalizado(solicitud);
		if (repositorio.existsByCodigoAndIdNot(codigo, id)) {
			throw ExcepcionNegocio.conflicto("Ya existe un curso con el código " + codigo + ".");
		}
		curso.editar(codigo, solicitud.nombre().trim());
		return CursoDto.de(repositorio.saveAndFlush(curso));
	}

	public void eliminar(String id) {
		Curso curso = buscar(id);
		try {
			repositorio.delete(curso);
			repositorio.flush();
		} catch (DataIntegrityViolationException e) {
			throw ExcepcionNegocio.conflicto("No se puede eliminar el curso porque tiene canales asociados.");
		}
	}

	@Transactional(readOnly = true)
	public List<CursoDto> listar() {
		return repositorio.findAllByOrderByCodigoAsc().stream().map(CursoDto::de).toList();
	}

	@Transactional(readOnly = true)
	public CursoDto porId(String id) {
		return CursoDto.de(buscar(id));
	}

	/** Cursos cuyo nombre contiene el texto. Si el texto esta vacio, devuelve todos. */
	@Transactional(readOnly = true)
	public List<CursoDto> porNombre(String texto) {
		if (texto == null || texto.isBlank()) {
			return listar();
		}
		return repositorio.findByNombreContainingOrderByCodigoAsc(texto.trim()).stream().map(CursoDto::de).toList();
	}

	private Curso buscar(String id) {
		return repositorio.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El curso no existe."));
	}

	private String codigoNormalizado(CursoSolicitud solicitud) {
		return solicitud.codigo().trim().toUpperCase();
	}

}

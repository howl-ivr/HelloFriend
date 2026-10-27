package com.hellofriend.academico.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

/**
 * Curso del catalogo permanente (tabla {@code academico_curso}). El identificador se genera solo y no se puede
 * cambiar; el codigo, el nombre y el ciclo si se pueden editar.
 */
@Entity
@Table(name = "academico_curso")
public class Curso {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, length = 36)
	private String id;

	@Column(name = "codigo", nullable = false, length = 20)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 150)
	private String nombre;

	@Column(name = "ciclo")
	private Integer ciclo;

	/** Fecha de creacion en UTC. */
	@Column(name = "creado_en", nullable = false, updatable = false)
	private LocalDateTime creadoEn;

	protected Curso() {
	}

	public Curso(String codigo, String nombre, Integer ciclo, LocalDateTime creadoEn) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.ciclo = ciclo;
		this.creadoEn = creadoEn;
	}

	public void editar(String codigo, String nombre, Integer ciclo) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.ciclo = ciclo;
	}

	public String getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public Integer getCiclo() {
		return ciclo;
	}

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

}

package com.hellofriend.academico.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

/**
 * Curso del catalogo permanente (tabla {@code academico_curso}). El identificador se genera solo y no se puede
 * cambiar; el codigo y el nombre si se pueden editar.
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

	/** Fecha de creacion en UTC. */
	@Column(name = "creado_en", nullable = false, updatable = false)
	private LocalDateTime creadoEn;

	protected Curso() {
	}

	public Curso(String codigo, String nombre, LocalDateTime creadoEn) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.creadoEn = creadoEn;
	}

	public void editar(String codigo, String nombre) {
		this.codigo = codigo;
		this.nombre = nombre;
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

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

}

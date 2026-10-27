package com.hellofriend.notificaciones.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

/**
 * Aviso en la bandeja de una persona (tabla {@code notificaciones_notificacion}, V2): una fila por destinatario. Los
 * ids de usuario, incidencia y canal son texto, no entidades: este módulo no usa las de otros. El contenido nunca debe
 * incluir al autor de la incidencia ni al profesor (RF-17.3); quien escribe el mensaje es responsable de cumplirlo.
 */
@Entity
@Table(name = "notificaciones_notificacion")
public class Notificacion {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, length = 36)
	private String id;

	@Column(name = "destinatario_id", nullable = false, updatable = false, length = 36)
	private String destinatarioId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, updatable = false, length = 20)
	private TipoNotificacion tipo;

	@Column(name = "incidencia_id", updatable = false, length = 36)
	private String incidenciaId;

	@Column(name = "canal_id", updatable = false, length = 36)
	private String canalId;

	@Column(name = "titulo", nullable = false, updatable = false, length = 150)
	private String titulo;

	@Column(name = "mensaje", nullable = false, updatable = false, length = 500)
	private String mensaje;

	/** Cuándo se leyó, en UTC; null mientras no se lea. */
	@Column(name = "leida_en")
	private LocalDateTime leidaEn;

	/** Fecha de creación en UTC. */
	@Column(name = "creada_en", nullable = false, updatable = false)
	private LocalDateTime creadaEn;

	protected Notificacion() {
	}

	public Notificacion(String destinatarioId, TipoNotificacion tipo, String incidenciaId, String canalId,
			String titulo, String mensaje, LocalDateTime creadaEn) {
		this.destinatarioId = destinatarioId;
		this.tipo = tipo;
		this.incidenciaId = incidenciaId;
		this.canalId = canalId;
		this.titulo = titulo;
		this.mensaje = mensaje;
		this.creadaEn = creadaEn;
	}

	/** Marca como leída. Si ya lo estaba no cambia la fecha: marcar dos veces es inofensivo. */
	public void marcarLeida(LocalDateTime ahora) {
		if (leidaEn == null) {
			leidaEn = ahora;
		}
	}

	public boolean isLeida() {
		return leidaEn != null;
	}

	public String getId() {
		return id;
	}

	public String getDestinatarioId() {
		return destinatarioId;
	}

	public TipoNotificacion getTipo() {
		return tipo;
	}

	public String getIncidenciaId() {
		return incidenciaId;
	}

	public String getCanalId() {
		return canalId;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getMensaje() {
		return mensaje;
	}

	public LocalDateTime getLeidaEn() {
		return leidaEn;
	}

	public LocalDateTime getCreadaEn() {
		return creadaEn;
	}

}

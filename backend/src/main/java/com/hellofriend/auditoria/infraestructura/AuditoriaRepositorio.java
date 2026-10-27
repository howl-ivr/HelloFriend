package com.hellofriend.auditoria.infraestructura;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acceso a la tabla {@code auditoria_evento} (V4): una fila por acción, solo de adición. */
@Repository
public class AuditoriaRepositorio {

	/** Evento tal como está guardado; el actor ya viene como nombre de usuario (null si fue el sistema). */
	public record FilaEvento(String id, String accion, String actor, String incidenciaId, String detalle,
			LocalDateTime fecha) {
	}

	/** Confirmación (o su retiro) de una incidencia, con la identidad de quien la hizo. */
	public record FilaConfirmacion(String accion, LocalDateTime fecha, String usuario) {
	}

	private final JdbcTemplate jdbc;

	public AuditoriaRepositorio(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Si el actor no existe como usuario se guarda sin actor (acción del sistema) en vez de fallar por la FK. */
	public void insertar(String id, String accion, String actorId, String incidenciaId, String detalle,
			LocalDateTime fecha) {
		jdbc.update("""
				INSERT INTO auditoria_evento (id, accion, actor_id, incidencia_id, detalle, fecha)
				VALUES (?, ?, (SELECT id FROM usuarios_usuario WHERE id = ?), ?, ?, ?)
				""", id, accion, actorId, incidenciaId, detalle, fecha);
	}

	/** Todos los eventos, del más nuevo al más viejo. */
	public List<FilaEvento> todos() {
		return jdbc.query("""
				SELECT e.id, e.accion, u.usuario AS actor, e.incidencia_id, e.detalle, e.fecha
				FROM auditoria_evento e LEFT JOIN usuarios_usuario u ON u.id = e.actor_id ORDER BY e.fecha DESC
				""", (rs, n) -> new FilaEvento(rs.getString("id"), rs.getString("accion"), rs.getString("actor"),
				rs.getString("incidencia_id"), rs.getString("detalle"), rs.getObject("fecha", LocalDateTime.class)));
	}

	public List<FilaConfirmacion> confirmacionesDe(String incidenciaId) {
		return jdbc.query("""
				SELECT e.fecha, e.accion, u.usuario FROM auditoria_evento e
				JOIN usuarios_usuario u ON u.id = e.actor_id WHERE e.incidencia_id = ?
				AND e.accion IN ('INCIDENCIA_CONFIRMADA', 'INCIDENCIA_CONFIRMACION_RETIRADA')
				""", (rs, n) -> new FilaConfirmacion(rs.getString("accion"), rs.getObject("fecha", LocalDateTime.class),
				rs.getString("usuario")), incidenciaId);
	}

}

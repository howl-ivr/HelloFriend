package com.hellofriend.notificaciones.aplicacion;

import java.time.LocalDateTime;

import com.hellofriend.notificaciones.dominio.Notificacion;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;

/**
 * Aviso de la bandeja (docs/API.md: id, incidenciaId, mensaje, leida, fecha). {@code tipo} y {@code titulo} son campos
 * extra, compatibles con el contrato. Nunca incluye al destinatario ni a quien originó el aviso.
 */
public record NotificacionDto(String id, String incidenciaId, TipoNotificacion tipo, String titulo, String mensaje,
		boolean leida, LocalDateTime fecha) {

	public static NotificacionDto de(Notificacion n) {
		return new NotificacionDto(n.getId(), n.getIncidenciaId(), n.getTipo(), n.getTitulo(), n.getMensaje(),
				n.isLeida(), n.getCreadaEn());
	}

}

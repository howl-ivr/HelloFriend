package com.hellofriend.notificaciones.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.notificaciones.infraestructura.NotificacionRepositorio;

/** La bandeja de la persona autenticada: ver sus avisos y marcarlos como leídos. Nadie accede a los de otra persona. */
@Service
@Transactional
public class ServicioNotificaciones {

	private final NotificacionRepositorio repositorio;
	private final Clock reloj;

	public ServicioNotificaciones(NotificacionRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	/** Todos los avisos de la persona, los más nuevos primero. */
	@Transactional(readOnly = true)
	public List<NotificacionDto> listar(String usuarioId) {
		return repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(usuarioId).stream().map(NotificacionDto::de)
				.toList();
	}

	/** Cuántos avisos sin leer tiene la persona. */
	@Transactional(readOnly = true)
	public NoLeidasDto contarNoLeidas(String usuarioId) {
		return new NoLeidasDto(repositorio.countByDestinatarioIdAndLeidaEnIsNull(usuarioId));
	}

	/** 404 si no existe o es de otra persona (no se revela cuál). Si ya estaba leída, no cambia nada. */
	public void marcarLeida(String usuarioId, String notificacionId) {
		repositorio.findByIdAndDestinatarioId(notificacionId, usuarioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Notificación no encontrada."))
				.marcarLeida(ahora());
	}

	/**
	 * Marca como leídas las no leídas de la persona. Con {@code hasta} (la {@code fecha} del aviso más nuevo que vio en
	 * el panel) solo marca las creadas hasta ese momento: lo que llegó mientras miraba sigue sin leer. Sin
	 * {@code hasta}
	 * marca todas.
	 */
	public void marcarTodasLeidas(String usuarioId, LocalDateTime hasta) {
		if (hasta == null) {
			repositorio.marcarTodasLeidas(usuarioId, ahora());
		} else {
			repositorio.marcarLeidasHasta(usuarioId, ahora(), hasta);
		}
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

}

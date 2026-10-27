package com.hellofriend.notificaciones.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.notificaciones.dominio.Notificacion;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;
import com.hellofriend.notificaciones.infraestructura.NotificacionRepositorio;

/**
 * Crea avisos en las bandejas. Es lo que usan los demás módulos (por ejemplo incidencias al aprobar o resolver): se
 * llama dentro de su misma transacción, así el aviso y el cambio que lo origina se guardan juntos o ninguno.
 *
 * <p>
 * Regla (RF-17.3): el {@code titulo} y el {@code mensaje} nunca incluyen el nombre del profesor ni la identidad del
 * autor de la incidencia. El emisor no puede comprobarlo; lo debe cumplir quien lo llama.
 */
@Service
@Transactional
public class EmisorNotificaciones {

	static final int MAX_TITULO = 150;
	static final int MAX_MENSAJE = 500;

	private final NotificacionRepositorio repositorio;
	private final Clock reloj;

	public EmisorNotificaciones(NotificacionRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	/**
	 * Un aviso por cada destinatario. Los repetidos y los vacíos se ignoran, y un texto más largo que el de la tabla se
	 * recorta con "…". Devuelve cuántos avisos creó.
	 *
	 * @param incidenciaId
	 *            incidencia a la que se refiere (puede ser null)
	 * @param canalId
	 *            canal de esa incidencia (puede ser null)
	 */
	public int notificar(Collection<String> destinatarioIds, TipoNotificacion tipo, String incidenciaId, String canalId,
			String titulo, String mensaje) {
		Objects.requireNonNull(tipo, "El tipo de aviso es obligatorio");
		Set<String> destinatarios = new LinkedHashSet<>();
		for (String id : destinatarioIds) {
			if (id != null && !id.isBlank()) {
				destinatarios.add(id);
			}
		}
		if (destinatarios.isEmpty()) {
			return 0;
		}
		LocalDateTime creadaEn = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
		String tituloFinal = recortar(titulo, MAX_TITULO);
		String mensajeFinal = recortar(mensaje, MAX_MENSAJE);
		repositorio.saveAllAndFlush(destinatarios.stream().map(
				id -> new Notificacion(id, tipo, incidenciaId, canalId, tituloFinal, mensajeFinal, creadaEn)).toList());
		return destinatarios.size();
	}

	/** Si el texto no cabe, lo corta y termina en "…" para que el total no pase de {@code max}. */
	static String recortar(String texto, int max) {
		Objects.requireNonNull(texto, "El texto del aviso es obligatorio");
		String limpio = texto.strip();
		return limpio.length() <= max ? limpio : limpio.substring(0, max - 1).stripTrailing() + "…";
	}

}

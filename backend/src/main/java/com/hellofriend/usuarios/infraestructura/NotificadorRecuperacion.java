package com.hellofriend.usuarios.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envía el correo con el enlace de recuperación (en local llega a Mailpit). Se ejecuta en segundo plano para que
 * pedir el enlace tarde lo mismo exista o no el correo, y nunca escribe el token en el log.
 */
@Component
public class NotificadorRecuperacion {

	private static final Logger log = LoggerFactory.getLogger(NotificadorRecuperacion.class);

	private final JavaMailSender correo;
	private final String remitente;
	private final String urlFrontend;
	private final long minutos;

	public NotificadorRecuperacion(JavaMailSender correo, @Value("${hellofriend.correo.remitente}") String remitente,
			@Value("${hellofriend.frontend.url}") String urlFrontend,
			@Value("${hellofriend.recuperacion.minutos}") long minutos) {
		this.correo = correo;
		this.remitente = remitente;
		this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
		this.minutos = minutos;
	}

	@Async
	public void enviar(String destinatario, String token) {
		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(remitente);
		mensaje.setTo(destinatario);
		mensaje.setSubject("Restablece tu contraseña de HelloFriend");
		mensaje.setText("""
				Recibimos una solicitud para restablecer tu contraseña de HelloFriend.

				Abre este enlace en los próximos %d minutos:
				%s/restablecer?token=%s

				Si no fuiste tú, ignora este mensaje: tu contraseña no cambia.
				""".formatted(minutos, urlFrontend, token));
		try {
			correo.send(mensaje);
		} catch (RuntimeException e) {
			// Sin destinatario ni token en el log: son datos personales y un secreto.
			log.warn("No se pudo enviar un correo de recuperación: {}", e.getClass().getSimpleName());
		}
	}

}

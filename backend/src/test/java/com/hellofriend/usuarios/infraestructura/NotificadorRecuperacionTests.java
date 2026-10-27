package com.hellofriend.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class NotificadorRecuperacionTests {

	private final JavaMailSender correo = mock(JavaMailSender.class);

	@Test
	void enviaElEnlaceConElTokenAlDestinatario() {
		var notificador = new NotificadorRecuperacion(correo, "no-responder@hellofriend.local",
				"http://localhost:5173", 30);

		notificador.enviar("ana@pucp.edu.pe", "tok-123");

		var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(correo).send(captor.capture());
		SimpleMailMessage mensaje = captor.getValue();
		assertThat(mensaje.getTo()).containsExactly("ana@pucp.edu.pe");
		assertThat(mensaje.getFrom()).isEqualTo("no-responder@hellofriend.local");
		assertThat(mensaje.getText()).contains("http://localhost:5173/restablecer?token=tok-123")
				.contains("30 minutos");
	}

	@Test
	void ignoraLaBarraFinalDeLaUrlDelFrontend() {
		var notificador = new NotificadorRecuperacion(correo, "no-responder@hellofriend.local",
				"https://app.example.com/", 30);

		notificador.enviar("ana@pucp.edu.pe", "tok");

		var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(correo).send(captor.capture());
		assertThat(captor.getValue().getText()).contains("https://app.example.com/restablecer?token=tok");
	}

	@Test
	void siElCorreoFallaNoLanzaNadaNiFiltraElToken() {
		doThrow(new MailSendException("SMTP caido")).when(correo).send(any(SimpleMailMessage.class));
		var notificador = new NotificadorRecuperacion(correo, "no-responder@hellofriend.local",
				"http://localhost:5173", 30);

		assertThatCode(() -> notificador.enviar("ana@pucp.edu.pe", "tok-secreto")).doesNotThrowAnyException();
	}

}

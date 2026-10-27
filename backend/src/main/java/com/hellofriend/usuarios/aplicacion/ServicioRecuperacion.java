package com.hellofriend.usuarios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.TokenRecuperacion;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.NotificadorRecuperacion;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** Recuperación de contraseña por correo: enlace de un solo uso que vence y cierra las sesiones al usarse. */
@Service
@Transactional
public class ServicioRecuperacion {

	static final String ENLACE_INVALIDO = "El enlace de recuperación no es válido o ya venció.";

	private final UsuarioRepositorio repositorio;
	private final PasswordEncoder contrasenas;
	private final NotificadorRecuperacion notificador;
	private final Clock reloj;
	private final Duration vigencia;

	public ServicioRecuperacion(UsuarioRepositorio repositorio, PasswordEncoder contrasenas,
			NotificadorRecuperacion notificador, Clock reloj,
			@Value("${hellofriend.recuperacion.minutos}") long minutos) {
		this.repositorio = repositorio;
		this.contrasenas = contrasenas;
		this.notificador = notificador;
		this.reloj = reloj;
		this.vigencia = Duration.ofMinutes(minutos);
	}

	/**
	 * Si el correo es de una cuenta que puede iniciar sesión, genera un enlace nuevo (el anterior deja de valer) y lo
	 * envía. En cualquier otro caso no hace nada: quien llama no puede saber si el correo existe.
	 */
	public void solicitar(String correo) {
		repositorio.findByCorreo(correo.strip().toLowerCase(Locale.ROOT)).filter(Usuario::puedeIniciarSesion)
				.ifPresent(usuario -> {
					String token = TokenRecuperacion.generar();
					usuario.iniciarRecuperacion(TokenRecuperacion.hash(token), ahora().plus(vigencia));
					notificador.enviar(usuario.getCorreo(), token);
				});
	}

	/** Cambia la contraseña con el token del correo. Un token desconocido, vencido o ya usado da el mismo 400. */
	public void restablecer(String token, String contrasenaNueva) {
		ReglasContrasena.exigirLargoSoportado(contrasenaNueva);
		Usuario usuario = repositorio.findByTokenRecuperacionHash(TokenRecuperacion.hash(token.strip()))
				.filter(Usuario::puedeIniciarSesion).filter(u -> u.recuperacionVigenteEn(ahora()))
				.orElseThrow(() -> ExcepcionNegocio.invalido(ENLACE_INVALIDO));
		usuario.restablecerContrasena(contrasenas.encode(contrasenaNueva));
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

}

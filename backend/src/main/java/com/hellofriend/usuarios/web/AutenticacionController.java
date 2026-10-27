package com.hellofriend.usuarios.web;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.aplicacion.PerfilDto;
import com.hellofriend.usuarios.aplicacion.RespuestaLogin;
import com.hellofriend.usuarios.aplicacion.ServicioAutenticacion;
import com.hellofriend.usuarios.aplicacion.ServicioRecuperacion;
import com.hellofriend.usuarios.aplicacion.ServicioRegistro;
import com.hellofriend.usuarios.aplicacion.SolicitudCambioContrasena;
import com.hellofriend.usuarios.aplicacion.SolicitudLogin;
import com.hellofriend.usuarios.aplicacion.SolicitudRecuperacion;
import com.hellofriend.usuarios.aplicacion.SolicitudRegistro;
import com.hellofriend.usuarios.aplicacion.SolicitudRestablecer;

/**
 * Registro, login y recuperación de contraseña (públicos), quién soy y cerrar sesión (requieren token). Registro y
 * login siguen docs/API.md; la recuperación no está en ese contrato todavía.
 */
@RestController
@RequestMapping("/auth")
public class AutenticacionController {

	private final ServicioAutenticacion servicio;
	private final ServicioRegistro registro;
	private final ServicioRecuperacion recuperacion;

	public AutenticacionController(ServicioAutenticacion servicio, ServicioRegistro registro,
			ServicioRecuperacion recuperacion) {
		this.servicio = servicio;
		this.registro = registro;
		this.recuperacion = recuperacion;
	}

	/** POST /auth/registro {correo, usuario, password} → 201 {mensaje}. La cuenta queda activa como ESTUDIANTE. */
	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, String> registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
		registro.registrar(solicitud);
		return Map.of("mensaje", "Tu cuenta fue creada. Ya puedes iniciar sesión.");
	}

	/**
	 * POST /auth/login {usuario, password} → {token, usuario, rol}. 401 si falla; 403 si la cuenta está desactivada.
	 */
	@PostMapping("/login")
	public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
		return servicio.iniciarSesion(solicitud);
	}

	/** POST /auth/recuperar {correo} → 200 {mensaje}, exista o no el correo. Si existe, envía el enlace. */
	@PostMapping("/recuperar")
	public Map<String, String> recuperar(@Valid @RequestBody SolicitudRecuperacion solicitud) {
		recuperacion.solicitar(solicitud.correo());
		return Map.of("mensaje", "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.");
	}

	/**
	 * POST /auth/restablecer {token, password} → 200 {mensaje}. 400 si el enlace no sirve o la contraseña no cumple.
	 */
	@PostMapping("/restablecer")
	public Map<String, String> restablecer(@Valid @RequestBody SolicitudRestablecer solicitud) {
		recuperacion.restablecer(solicitud.token(), solicitud.password());
		return Map.of("mensaje", "Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
	}

	@GetMapping("/me")
	public PerfilDto yo(Authentication autenticacion) {
		return servicio.perfil(ServicioJwt.usuarioActual(autenticacion));
	}

	/**
	 * PUT /auth/me/contrasena {contrasenaActual, contrasenaNueva} → {token, usuario, rol}. Cierra las demás sesiones y
	 * devuelve un token nuevo; 400 si la actual es incorrecta o la nueva no cumple.
	 */
	@PutMapping("/me/contrasena")
	public RespuestaLogin cambiarContrasena(Authentication autenticacion,
			@Valid @RequestBody SolicitudCambioContrasena solicitud) {
		return servicio.cambiarContrasena(ServicioJwt.usuarioActual(autenticacion), solicitud);
	}

	/** Invalida todos los tokens del usuario (suma 1 a version_sesion). */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(Authentication autenticacion) {
		servicio.cerrarSesion(ServicioJwt.usuarioActual(autenticacion));
	}

}

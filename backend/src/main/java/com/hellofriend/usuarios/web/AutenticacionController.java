package com.hellofriend.usuarios.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.aplicacion.PerfilDto;
import com.hellofriend.usuarios.aplicacion.RespuestaLogin;
import com.hellofriend.usuarios.aplicacion.ServicioAutenticacion;
import com.hellofriend.usuarios.aplicacion.SolicitudLogin;

/** Sesión: login (público, contrato de docs/API.md), quién soy y cerrar sesión (requieren token). */
@RestController
@RequestMapping("/auth")
public class AutenticacionController {

	private final ServicioAutenticacion servicio;

	public AutenticacionController(ServicioAutenticacion servicio) {
		this.servicio = servicio;
	}

	/** POST /auth/login {usuario, password} → {token, usuario, rol}. 401 si falla; 403 si la cuenta no está activa. */
	@PostMapping("/login")
	public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
		return servicio.iniciarSesion(solicitud);
	}

	@GetMapping("/me")
	public PerfilDto yo(Authentication autenticacion) {
		return servicio.perfil(ServicioJwt.usuarioActual(autenticacion));
	}

	/** Invalida todos los tokens del usuario (suma 1 a version_sesion). */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(Authentication autenticacion) {
		servicio.cerrarSesion(ServicioJwt.usuarioActual(autenticacion));
	}

}

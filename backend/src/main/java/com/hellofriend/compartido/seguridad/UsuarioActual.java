package com.hellofriend.compartido.seguridad;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Quién hace la petición: su id y sus roles, sacados del token. Los servicios lo reciben en vez de depender de Spring
 * Security, y así las reglas por rol se pueden probar sin una petición HTTP.
 */
public record UsuarioActual(String id, Set<String> roles) {

	public static UsuarioActual de(Authentication autenticacion) {
		Set<String> roles = autenticacion.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.filter(a -> a.startsWith("ROLE_")).map(a -> a.substring("ROLE_".length()))
				.collect(Collectors.toUnmodifiableSet());
		return new UsuarioActual(ServicioJwt.usuarioActual(autenticacion), roles);
	}

	public boolean esEstudiante() {
		return roles.contains("ESTUDIANTE");
	}

	public boolean esDirector() {
		return roles.contains("DIRECTOR");
	}

	public boolean esAdministrador() {
		return roles.contains("ADMINISTRADOR");
	}

}

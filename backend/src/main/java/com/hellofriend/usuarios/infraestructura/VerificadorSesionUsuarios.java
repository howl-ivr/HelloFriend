package com.hellofriend.usuarios.infraestructura;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.seguridad.VerificadorSesion;
import com.hellofriend.usuarios.dominio.Usuario;

/** La sesión sigue vigente solo si la cuenta existe, puede iniciar sesión y su versión de sesión es la del token. */
@Component
public class VerificadorSesionUsuarios implements VerificadorSesion {

	private final UsuarioRepositorio repositorio;

	public VerificadorSesionUsuarios(UsuarioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean sesionVigente(String usuarioId, int version) {
		return repositorio.findById(usuarioId).filter(Usuario::puedeIniciarSesion)
				.filter(usuario -> usuario.getVersionSesion() == version).isPresent();
	}

}

package com.hellofriend.usuarios.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** Casos de uso de sesión: iniciar sesión, ver quién soy y cerrar sesión. */
@Service
@Transactional
public class ServicioAutenticacion {

	static final String CREDENCIALES_INVALIDAS = "El usuario o la contraseña son incorrectos.";

	private final UsuarioRepositorio repositorio;
	private final PasswordEncoder contrasenas;
	private final ServicioJwt jwt;
	private final String hashFicticio;

	public ServicioAutenticacion(UsuarioRepositorio repositorio, PasswordEncoder contrasenas, ServicioJwt jwt) {
		this.repositorio = repositorio;
		this.contrasenas = contrasenas;
		this.jwt = jwt;
		// Si el usuario no existe se compara contra este hash: tarda lo mismo y no revela qué cuentas existen.
		this.hashFicticio = contrasenas.encode("cuenta-inexistente");
	}

	@Transactional(readOnly = true)
	public RespuestaLogin iniciarSesion(SolicitudLogin solicitud) {
		Usuario usuario = repositorio.findByUsuario(solicitud.usuario().strip()).orElse(null);
		if (usuario == null) {
			contrasenas.matches(solicitud.password(), hashFicticio);
			throw ExcepcionNegocio.noAutorizado(CREDENCIALES_INVALIDAS);
		}
		if (!contrasenas.matches(solicitud.password(), usuario.getContrasenaHash())) {
			throw ExcepcionNegocio.noAutorizado(CREDENCIALES_INVALIDAS);
		}
		exigirQuePuedaEntrar(usuario);
		String token = jwt.emitir(usuario.getId(), usuario.getUsuario(), usuario.getRol().name(),
				usuario.getVersionSesion());
		return new RespuestaLogin(token, usuario.getUsuario(), usuario.getRol());
	}

	@Transactional(readOnly = true)
	public PerfilDto perfil(String usuarioId) {
		return PerfilDto.de(buscar(usuarioId));
	}

	/** Suma 1 a la versión de sesión: todos los tokens emitidos hasta ahora dejan de valer. */
	public void cerrarSesion(String usuarioId) {
		buscar(usuarioId).cerrarSesion();
	}

	/** Solo quien acierta la contraseña se entera del estado de la cuenta. */
	private static void exigirQuePuedaEntrar(Usuario usuario) {
		switch (usuario.getEstado()) {
			case PENDIENTE -> throw ExcepcionNegocio.prohibido("Tu cuenta está pendiente de aprobación.");
			case RECHAZADA -> throw ExcepcionNegocio.prohibido("Tu solicitud de registro fue rechazada.");
			case ACTIVA -> {
				if (!usuario.isActivo()) {
					throw ExcepcionNegocio.prohibido("Tu cuenta no está activa.");
				}
			}
		}
	}

	private Usuario buscar(String usuarioId) {
		return repositorio.findById(usuarioId)
				.orElseThrow(() -> ExcepcionNegocio.noAutorizado("La sesión ya no es válida."));
	}

}

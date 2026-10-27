package com.hellofriend.usuarios.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** Registro público de estudiantes: la cuenta queda PENDIENTE hasta que el administrador la apruebe. */
@Service
@Transactional
public class ServicioRegistro {

	private final UsuarioRepositorio repositorio;
	private final PasswordEncoder contrasenas;
	private final Clock reloj;

	public ServicioRegistro(UsuarioRepositorio repositorio, PasswordEncoder contrasenas, Clock reloj) {
		this.repositorio = repositorio;
		this.contrasenas = contrasenas;
		this.reloj = reloj;
	}

	public void registrar(SolicitudRegistro solicitud) {
		String correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);
		String usuario = solicitud.usuario().strip();
		ReglasContrasena.exigirLargoSoportado(solicitud.password());
		if (repositorio.existsByUsuario(usuario)) {
			throw ExcepcionNegocio.conflicto("Ese usuario ya está en uso.");
		}
		if (repositorio.existsByCorreo(correo)) {
			throw ExcepcionNegocio.conflicto("Ese correo ya está registrado.");
		}
		LocalDateTime creadoEn = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
		try {
			repositorio.saveAndFlush(
					Usuario.solicitud(correo, usuario, contrasenas.encode(solicitud.password()), creadoEn));
		} catch (DataIntegrityViolationException e) {
			// Dos registros simultáneos con el mismo dato: lo frena la restricción UNIQUE de la base.
			throw ExcepcionNegocio.conflicto("El usuario o el correo ya están registrados.");
		}
	}

}

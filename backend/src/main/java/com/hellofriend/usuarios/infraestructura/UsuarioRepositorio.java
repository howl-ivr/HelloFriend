package com.hellofriend.usuarios.infraestructura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hellofriend.usuarios.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {

	/** La colación de la BD ignora mayúsculas: "ANA@pucp.edu.pe" encuentra "ana@pucp.edu.pe". */
	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByUsuario(String usuario);

	/** Para el login: se acepta el correo o el nombre de usuario. */
	Optional<Usuario> findByCorreoOrUsuario(String correo, String usuario);

	boolean existsByCorreo(String correo);

	boolean existsByUsuario(String usuario);

}

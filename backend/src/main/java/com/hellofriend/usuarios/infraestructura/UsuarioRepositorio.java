package com.hellofriend.usuarios.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {

	/** La colación de la BD ignora mayúsculas: "ANA@pucp.edu.pe" encuentra "ana@pucp.edu.pe". */
	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByUsuario(String usuario);

	List<Usuario> findByRolOrderByCreadoEnAsc(Rol rol);

	List<Usuario> findByRolAndEstadoOrderByCreadoEnAsc(Rol rol, EstadoCuenta estado);

	boolean existsByCorreo(String correo);

	boolean existsByUsuario(String usuario);

}

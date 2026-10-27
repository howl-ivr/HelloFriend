package com.hellofriend.usuarios.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {

	/** La colación de la BD ignora mayúsculas: "ANA@pucp.edu.pe" encuentra "ana@pucp.edu.pe". */
	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByUsuario(String usuario);

	Optional<Usuario> findByTokenRecuperacionHash(String tokenRecuperacionHash);

	/**
	 * Lista para el administrador, los más antiguos primero. {@code rol} y {@code buscar} son opcionales (null = sin
	 * filtro); {@code buscar} ya viene escapado con {@code !} ({@link #escaparBusqueda}) y se compara contra el usuario
	 * y el correo, sin importar mayúsculas ni tildes.
	 */
	@Query("""
			select u from Usuario u
			where (:rol is null or u.rol = :rol)
			  and (:buscar is null
			       or u.usuario like concat('%', :buscar, '%') escape '!'
			       or u.correo like concat('%', :buscar, '%') escape '!')
			order by u.creadoEn asc, u.usuario asc
			""")
	List<Usuario> buscar(@Param("rol") Rol rol, @Param("buscar") String buscar);

	boolean existsByCorreo(String correo);

	boolean existsByUsuario(String usuario);

	/** Escapa los comodines de LIKE para que "50%" o "a_b" se busquen como texto literal. */
	static String escaparBusqueda(String texto) {
		return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

}

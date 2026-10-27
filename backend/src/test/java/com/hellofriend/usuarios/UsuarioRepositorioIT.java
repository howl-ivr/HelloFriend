package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** La entidad Usuario coincide con la tabla usuarios_usuario y el repositorio busca como se espera. */
class UsuarioRepositorioIT extends PruebaIntegracion {

	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 12, 0);

	@Autowired
	private UsuarioRepositorio repositorio;

	private Usuario guardarAna() {
		return repositorio.saveAndFlush(new Usuario("ana@pucp.edu.pe", "ana", "hash", Rol.ESTUDIANTE, AHORA));
	}

	@Test
	void guardaConIdGeneradoYValoresPorDefecto() {
		Usuario ana = guardarAna();

		assertThat(ana.getId()).hasSize(36);
		assertThat(ana.isActivo()).isTrue();
		assertThat(ana.getVersionSesion()).isZero();
		assertThat(ana.getCreadoEn()).isEqualTo(AHORA);
	}

	@Test
	void buscaPorCorreoSinImportarMayusculas() {
		guardarAna();

		assertThat(repositorio.findByCorreo("ANA@pucp.edu.pe")).isPresent();
		assertThat(repositorio.existsByCorreo("otra@pucp.edu.pe")).isFalse();
	}

	@Test
	void buscaPorUsuarioOPorCorreoParaElLogin() {
		guardarAna();

		assertThat(repositorio.findByCorreoOrUsuario("ana", "ana")).isPresent();
		assertThat(repositorio.findByCorreoOrUsuario("ana@pucp.edu.pe", "ana@pucp.edu.pe")).isPresent();
		assertThat(repositorio.findByCorreoOrUsuario("nadie", "nadie")).isEmpty();
		assertThat(repositorio.existsByUsuario("ana")).isTrue();
	}

	@Test
	void cerrarSesionSumaUnoALaVersionEnLaBase() {
		Usuario ana = guardarAna();

		ana.cerrarSesion();
		repositorio.saveAndFlush(ana);

		var version = jdbc.sql("SELECT version_sesion FROM usuarios_usuario WHERE usuario = 'ana'")
				.query(Integer.class).single();
		assertThat(version).isEqualTo(1);
	}

	@Test
	void desactivarApagaLaCuentaYCierraLaSesion() {
		Usuario ana = guardarAna();

		ana.desactivar();
		repositorio.saveAndFlush(ana);

		var fila = jdbc.sql("SELECT activo, version_sesion FROM usuarios_usuario WHERE usuario = 'ana'")
				.query((rs, n) -> new Object[]{rs.getBoolean("activo"), rs.getInt("version_sesion")}).single();
		assertThat(fila[0]).isEqualTo(false);
		assertThat(fila[1]).isEqualTo(1);
	}

}

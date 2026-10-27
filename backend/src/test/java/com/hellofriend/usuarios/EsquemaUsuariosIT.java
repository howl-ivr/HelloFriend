package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.hellofriend.PruebaIntegracion;

/** V1__usuarios_cuentas: la tabla existe y la base rechaza los datos que rompen cada regla. */
class EsquemaUsuariosIT extends PruebaIntegracion {

	private void insertar(String correo, String usuario, String rol) {
		jdbc.sql("INSERT INTO usuarios_usuario (correo, usuario, contrasena_hash, rol) VALUES (?, ?, 'hash', ?)")
				.params(correo, usuario, rol).update();
	}

	@Test
	void guardaUsuarioConValoresPorDefecto() {
		insertar("ana@pucp.edu.pe", "ana", "ESTUDIANTE");

		var fila = jdbc.sql("SELECT id, activo, creado_en FROM usuarios_usuario WHERE usuario = 'ana'")
				.query((rs,
						n) -> new Object[]{rs.getString("id"), rs.getBoolean("activo"), rs.getTimestamp("creado_en")})
				.single();
		assertThat((String) fila[0]).hasSize(36);
		assertThat(fila[1]).isEqualTo(true);
		assertThat(fila[2]).isNotNull();
	}

	@Test
	void correoNoSePuedeRepetir() {
		insertar("ana@pucp.edu.pe", "ana", "ESTUDIANTE");
		rechazaPor("uq_usuarios_usuario_correo", () -> insertar("ANA@pucp.edu.pe", "ana2", "ESTUDIANTE"));
	}

	@Test
	void nombreDeUsuarioNoSePuedeRepetir() {
		insertar("ana@pucp.edu.pe", "ana", "ESTUDIANTE");
		rechazaPor("uq_usuarios_usuario_usuario", () -> insertar("otra@pucp.edu.pe", "ana", "ESTUDIANTE"));
	}

	@Test
	void usuarioDeMasDe15CaracteresSeRechaza() {
		rechazaPor("usuario", () -> insertar("ana@pucp.edu.pe", "abcdefghijklmnop", "ESTUDIANTE"));
	}

	@Test
	void rolDebeSerUnoDeLosPermitidos() {
		rechazaPor("ck_usuarios_usuario_rol", () -> insertar("ana@pucp.edu.pe", "ana", "ADMIN"));
	}

	@Test
	void aceptaLosCuatroRoles() {
		insertar("a@pucp.edu.pe", "a", "ESTUDIANTE");
		insertar("b@pucp.edu.pe", "b", "PROFESOR");
		insertar("c@pucp.edu.pe", "c", "DIRECTOR");
		insertar("d@pucp.edu.pe", "d", "ADMINISTRADOR");
		assertThat(jdbc.sql("SELECT COUNT(*) FROM usuarios_usuario").query(Integer.class).single()).isEqualTo(4);
	}

}

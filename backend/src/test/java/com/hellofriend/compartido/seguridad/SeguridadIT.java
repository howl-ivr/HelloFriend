package com.hellofriend.compartido.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** La cadena de seguridad: sin token no se entra, y el token solo sirve mientras la sesión siga vigente. */
class SeguridadIT extends PruebaIntegracion {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private ServicioJwt jwt;

	private Usuario guardarAna() {
		return repositorio
				.saveAndFlush(new Usuario("ana@pucp.edu.pe", "ana", "hash", Rol.ESTUDIANTE, LocalDateTime.now()));
	}

	private ResultActions pedirCursosCon(String token) throws Exception {
		return mvc.perform(get("/admin/cursos").header("Authorization", "Bearer " + token));
	}

	@Test
	void sinTokenResponde401ConMensaje() throws Exception {
		mvc.perform(get("/admin/cursos")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión."));
	}

	@Test
	void conTokenBasuraResponde401() throws Exception {
		pedirCursosCon("esto-no-es-un-jwt").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión."));
	}

	@Test
	void elEndpointDeSaludEsPublico() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void conTokenVigenteEntra() throws Exception {
		Usuario ana = guardarAna();

		pedirCursosCon(jwt.emitir(ana.getId(), "ESTUDIANTE", ana.getVersionSesion())).andExpect(status().isOk());
	}

	@Test
	void trasCerrarSesionElTokenViejoDejaDeValer() throws Exception {
		Usuario ana = guardarAna();
		String token = jwt.emitir(ana.getId(), "ESTUDIANTE", ana.getVersionSesion());

		ana.cerrarSesion();
		repositorio.saveAndFlush(ana);

		pedirCursosCon(token).andExpect(status().isUnauthorized());
	}

	@Test
	void unaCuentaDesactivadaPierdeElAccesoAlInstante() throws Exception {
		Usuario ana = guardarAna();
		String token = jwt.emitir(ana.getId(), "ESTUDIANTE", ana.getVersionSesion());
		ana.desactivar();
		repositorio.saveAndFlush(ana);

		pedirCursosCon(token).andExpect(status().isUnauthorized());
	}

	@Test
	void unTokenDeUnUsuarioInexistenteSeRechaza() throws Exception {
		pedirCursosCon(jwt.emitir("00000000-0000-0000-0000-000000000000", "ADMINISTRADOR", 0))
				.andExpect(status().isUnauthorized());
	}

}

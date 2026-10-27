package com.hellofriend.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** /admin/cursos con tokens reales: solo el ADMINISTRADOR entra; sin token 401 y con otro rol 403. */
class CursoAccesoIT extends PruebaIntegracion {

	private static final String CURSO = "{\"codigo\":\"INF263\",\"nombre\":\"Algoritmia\"}";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private ServicioJwt jwt;

	/** Los cinco endpoints del controlador, con un id cualquiera donde hace falta. */
	private List<MockHttpServletRequestBuilder> endpoints() {
		return List.of(post("/admin/cursos").contentType(MediaType.APPLICATION_JSON).content(CURSO),
				get("/admin/cursos"),
				get("/admin/cursos/x"), put("/admin/cursos/x").contentType(MediaType.APPLICATION_JSON).content(CURSO),
				delete("/admin/cursos/x"));
	}

	private String tokenDe(Rol rol) {
		String nombre = rol.name().toLowerCase();
		Usuario usuario = repositorio.saveAndFlush(
				new Usuario(nombre + "@pucp.edu.pe", nombre, "hash", rol, LocalDateTime.now()));
		return jwt.emitir(usuario.getId(), usuario.getUsuario(), rol.name(), usuario.getVersionSesion());
	}

	private int cursos() {
		return jdbc.sql("SELECT COUNT(*) FROM academico_curso").query(Integer.class).single();
	}

	@Test
	void sinTokenCadaEndpointResponde401() throws Exception {
		for (var endpoint : endpoints()) {
			mvc.perform(endpoint).andExpect(status().isUnauthorized());
		}
		assertThat(cursos()).isZero();
	}

	@Test
	void conOtroRolCadaEndpointResponde403() throws Exception {
		for (Rol rol : List.of(Rol.ESTUDIANTE, Rol.PROFESOR, Rol.DIRECTOR)) {
			String token = tokenDe(rol);
			for (var endpoint : endpoints()) {
				mvc.perform(endpoint.header("Authorization", "Bearer " + token)).andExpect(status().isForbidden())
						.andExpect(jsonPath("$.mensaje").value("No tienes permiso para realizar esta acción."));
			}
		}
		assertThat(cursos()).isZero();
	}

	@Test
	void unAdministradorConTokenRealPuedeCrearYListar() throws Exception {
		String token = tokenDe(Rol.ADMINISTRADOR);

		mvc.perform(post("/admin/cursos").contentType(MediaType.APPLICATION_JSON).content(CURSO)
				.header("Authorization", "Bearer " + token)).andExpect(status().isCreated());
		mvc.perform(get("/admin/cursos").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].codigo").value("INF263"));
	}

}

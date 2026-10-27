package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;

/** Login, /auth/me y logout de punta a punta, con el contrato de docs/API.md. */
class AutenticacionIT extends PruebaIntegracion {

	private static final String CLAVE = "Clave#2026";
	private static final String CREDENCIALES_INVALIDAS = "El usuario o la contraseña son incorrectos.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private PasswordEncoder codificador;

	private Usuario ana;

	@BeforeEach
	void crearAna() {
		ana = repositorio.saveAndFlush(
				new Usuario("ana@pucp.edu.pe", "ana", codificador.encode(CLAVE), Rol.ESTUDIANTE, LocalDateTime.now()));
	}

	private ResultActions login(String usuario, String password) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"));
	}

	private String tokenDeAna() throws Exception {
		String cuerpo = login("ana", CLAVE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.token");
	}

	private ResultActions yo(String token) throws Exception {
		return mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
	}

	// --- login

	@Test
	void loginCorrectoDevuelveTokenUsuarioYRol() throws Exception {
		login("ana", CLAVE).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.usuario").value("ana")).andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
				.andExpect(jsonPath("$.correo").doesNotExist()).andExpect(jsonPath("$.contrasenaHash").doesNotExist());
	}

	@Test
	void elUsuarioSeBuscaSinImportarMayusculas() throws Exception {
		login("ANA", CLAVE).andExpect(status().isOk());
	}

	@Test
	void contrasenaIncorrectaResponde401() throws Exception {
		login("ana", "otra-clave").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value(CREDENCIALES_INVALIDAS));
	}

	@Test
	void usuarioInexistenteResponde401ConElMismoMensaje() throws Exception {
		login("nadie", CLAVE).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value(CREDENCIALES_INVALIDAS));
	}

	@Test
	void elCorreoNoSirveParaEntrar() throws Exception {
		login("ana@pucp.edu.pe", CLAVE).andExpect(status().isUnauthorized());
	}

	@Test
	void cuentaInactivaConClaveCorrectaResponde403() throws Exception {
		ana.desactivar();
		repositorio.saveAndFlush(ana);

		login("ana", CLAVE).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Tu cuenta no está activa."));
	}

	@Test
	void cuentaInactivaConClaveIncorrectaNoRevelaSuEstado() throws Exception {
		ana.desactivar();
		repositorio.saveAndFlush(ana);

		login("ana", "otra-clave").andExpect(status().isUnauthorized());
	}

	@Test
	void sinUsuarioOSinContrasenaResponde400() throws Exception {
		login("", CLAVE).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Ingresa tu usuario."));
		login("ana", "").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Ingresa tu contraseña."));
	}

	// --- /auth/me

	@Test
	void meDevuelveUsuarioYRolSinCorreo() throws Exception {
		yo(tokenDeAna()).andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value("ana"))
				.andExpect(jsonPath("$.rol").value("ESTUDIANTE")).andExpect(jsonPath("$.correo").doesNotExist());
	}

	@Test
	void meSinTokenResponde401() throws Exception {
		mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
	}

	// --- logout

	@Test
	void logoutInvalidaElTokenPeroPermiteEntrarDeNuevo() throws Exception {
		String token = tokenDeAna();

		mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		yo(token).andExpect(status().isUnauthorized());
		repositorio.flush();
		assertThat(jdbc.sql("SELECT version_sesion FROM usuarios_usuario WHERE usuario = 'ana'").query(Integer.class)
				.single()).isEqualTo(1);
		yo(tokenDeAna()).andExpect(status().isOk());
	}

	@Test
	void logoutSinTokenResponde401() throws Exception {
		mvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
	}

}

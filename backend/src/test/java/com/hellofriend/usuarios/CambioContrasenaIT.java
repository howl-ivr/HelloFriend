package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.TokenRecuperacion;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;

/** PUT /auth/me/contrasena: cambia la contraseña con la actual y deja el token nuevo como única sesión. */
class CambioContrasenaIT extends PruebaIntegracion {

	private static final String CLAVE = "Clave#2026";
	private static final String NUEVA = "Nueva#2027";

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

	private ResultActions login(String password) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"ana\",\"password\":\"" + password + "\"}"));
	}

	private String tokenDeAna() throws Exception {
		String cuerpo = login(CLAVE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.token");
	}

	private ResultActions cambiar(String token, String actual, String nueva) throws Exception {
		return mvc.perform(put("/auth/me/contrasena").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}"));
	}

	private ResultActions yo(String token) throws Exception {
		return mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
	}

	@Test
	void cambiaLaContrasenaYDevuelveUnTokenNuevoQueSigueValiendo() throws Exception {
		String viejo = tokenDeAna();

		String cuerpo = cambiar(viejo, CLAVE, NUEVA).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario").value("ana"))
				.andExpect(jsonPath("$.rol").value("ESTUDIANTE")).andReturn().getResponse().getContentAsString();

		String nuevo = JsonPath.read(cuerpo, "$.token");
		assertThat(nuevo).isNotEqualTo(viejo);
		yo(nuevo).andExpect(status().isOk());
		login(NUEVA).andExpect(status().isOk());
		login(CLAVE).andExpect(status().isUnauthorized());
	}

	@Test
	void elTokenAnteriorYLasOtrasSesionesDejanDeValer() throws Exception {
		String deOtroDispositivo = tokenDeAna();
		String actual = tokenDeAna();

		cambiar(actual, CLAVE, NUEVA).andExpect(status().isOk());

		yo(actual).andExpect(status().isUnauthorized());
		yo(deOtroDispositivo).andExpect(status().isUnauthorized());
	}

	@Test
	void unaContrasenaActualIncorrectaResponde400YNoCambiaNada() throws Exception {
		String token = tokenDeAna();

		cambiar(token, "otra-clave", NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña actual es incorrecta."));

		yo(token).andExpect(status().isOk());
		login(CLAVE).andExpect(status().isOk());
	}

	@Test
	void laNuevaDebeSerDistintaDeLaActual() throws Exception {
		cambiar(tokenDeAna(), CLAVE, CLAVE).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La nueva contraseña debe ser distinta de la actual."));
	}

	@Test
	void rechazaNuevasContrasenasCortasOMuyLargas() throws Exception {
		String token = tokenDeAna();

		cambiar(token, CLAVE, "123").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña debe tener de 6 a 64 caracteres."));
		cambiar(token, CLAVE, "ñ".repeat(40)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña es demasiado larga."));
		login(CLAVE).andExpect(status().isOk());
	}

	@Test
	void camposVaciosResponden400() throws Exception {
		String token = tokenDeAna();

		cambiar(token, "", NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Ingresa tu contraseña actual."));
		cambiar(token, CLAVE, "").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Ingresa la nueva contraseña."));
	}

	@Test
	void sinTokenResponde401() throws Exception {
		mvc.perform(put("/auth/me/contrasena").contentType(MediaType.APPLICATION_JSON)
				.content("{\"contrasenaActual\":\"" + CLAVE + "\",\"contrasenaNueva\":\"" + NUEVA + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tambienAnulaUnEnlaceDeRecuperacionPendiente() throws Exception {
		String token = tokenDeAna();
		String enlace = TokenRecuperacion.generar();
		ana.iniciarRecuperacion(TokenRecuperacion.hash(enlace), LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
		repositorio.saveAndFlush(ana);

		cambiar(token, CLAVE, NUEVA).andExpect(status().isOk());

		mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + enlace + "\",\"password\":\"Otra#2028\"}"))
				.andExpect(status().isBadRequest());
	}

}

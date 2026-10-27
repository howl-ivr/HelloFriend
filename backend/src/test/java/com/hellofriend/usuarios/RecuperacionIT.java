package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.TokenRecuperacion;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.NotificadorRecuperacion;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;

/** Recuperación de contraseña: enlace de un solo uso que vence, sin revelar qué correos existen. */
class RecuperacionIT extends PruebaIntegracion {

	private static final String CLAVE = "Clave#2026";
	private static final String NUEVA = "Nueva#2027";
	private static final String ENLACE_INVALIDO = "El enlace de recuperación no es válido o ya venció.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private PasswordEncoder codificador;

	@MockitoBean
	private NotificadorRecuperacion notificador;

	private Usuario ana;

	@BeforeEach
	void crearAna() {
		ana = repositorio.saveAndFlush(
				new Usuario("ana@pucp.edu.pe", "ana", codificador.encode(CLAVE), Rol.ESTUDIANTE, LocalDateTime.now()));
	}

	private ResultActions recuperar(String correo) throws Exception {
		return mvc.perform(post("/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\"}"));
	}

	private ResultActions restablecer(String token, String password) throws Exception {
		return mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
	}

	private ResultActions login(String usuario, String password) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"));
	}

	/** Pide el enlace y devuelve el token que se habría enviado por correo. */
	private String pedirEnlace(String correo) throws Exception {
		recuperar(correo).andExpect(status().isOk());
		var captor = ArgumentCaptor.forClass(String.class);
		verify(notificador, atLeastOnce()).enviar(eq("ana@pucp.edu.pe"), captor.capture());
		return captor.getValue();
	}

	private LocalDateTime ahoraUtc() {
		return LocalDateTime.now(ZoneOffset.UTC);
	}

	// --- pedir el enlace

	@Test
	void conUnCorreoRegistradoEnviaElTokenYGuardaSoloSuHash() throws Exception {
		String token = pedirEnlace("ana@pucp.edu.pe");

		repositorio.flush();
		Usuario guardada = repositorio.findByUsuario("ana").orElseThrow();
		assertThat(token).hasSize(43);
		assertThat(guardada.getTokenRecuperacionHash()).isEqualTo(TokenRecuperacion.hash(token)).isNotEqualTo(token);
		assertThat(guardada.getTokenRecuperacionExpiraEn()).isBetween(ahoraUtc().plusMinutes(29),
				ahoraUtc().plusMinutes(31));
	}

	@Test
	void conUnCorreoInexistenteResponde200IgualYNoEnviaNada() throws Exception {
		String existente = recuperar("ana@pucp.edu.pe").andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		String inexistente = recuperar("nadie@pucp.edu.pe").andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		assertThat(inexistente).isEqualTo(existente);
		verify(notificador, times(1)).enviar(anyString(), anyString());
	}

	@Test
	void ignoraMayusculasYEspaciosDelCorreo() throws Exception {
		recuperar("  ANA@PUCP.edu.pe ").andExpect(status().isOk());

		verify(notificador).enviar(eq("ana@pucp.edu.pe"), anyString());
	}

	@Test
	void unaCuentaPendienteNoRecibeEnlaceYLaRespuestaEsLaMisma() throws Exception {
		repositorio.saveAndFlush(
				Usuario.solicitud("pedro@pucp.edu.pe", "pedro", codificador.encode(CLAVE), LocalDateTime.now()));

		recuperar("pedro@pucp.edu.pe").andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").isNotEmpty());

		verify(notificador, never()).enviar(anyString(), anyString());
	}

	@Test
	void sinCorreoResponde400() throws Exception {
		recuperar("").andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Ingresa tu correo."));
	}

	@Test
	void pedirOtroEnlaceInvalidaElAnterior() throws Exception {
		String primero = pedirEnlace("ana@pucp.edu.pe");
		recuperar("ana@pucp.edu.pe").andExpect(status().isOk());
		var captor = ArgumentCaptor.forClass(String.class);
		verify(notificador, times(2)).enviar(eq("ana@pucp.edu.pe"), captor.capture());
		String segundo = captor.getAllValues().get(1);

		assertThat(segundo).isNotEqualTo(primero);
		restablecer(primero, NUEVA).andExpect(status().isBadRequest());
		restablecer(segundo, NUEVA).andExpect(status().isOk());
	}

	// --- usar el enlace

	@Test
	void restablecerCambiaLaContrasenaYConsumeElEnlace() throws Exception {
		String token = pedirEnlace("ana@pucp.edu.pe");

		restablecer(token, NUEVA).andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").isNotEmpty());

		login("ana", NUEVA).andExpect(status().isOk());
		login("ana", CLAVE).andExpect(status().isUnauthorized());
		repositorio.flush();
		Usuario guardada = repositorio.findByUsuario("ana").orElseThrow();
		assertThat(guardada.getTokenRecuperacionHash()).isNull();
		assertThat(guardada.getTokenRecuperacionExpiraEn()).isNull();
	}

	@Test
	void elEnlaceSoloSirveUnaVez() throws Exception {
		String token = pedirEnlace("ana@pucp.edu.pe");
		restablecer(token, NUEVA).andExpect(status().isOk());

		restablecer(token, "Otra#2028").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(ENLACE_INVALIDO));
		login("ana", NUEVA).andExpect(status().isOk());
	}

	@Test
	void restablecerCierraLasSesionesAbiertas() throws Exception {
		String cuerpo = login("ana", CLAVE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String jwt = JsonPath.read(cuerpo, "$.token");
		mvc.perform(get("/auth/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isOk());

		restablecer(pedirEnlace("ana@pucp.edu.pe"), NUEVA).andExpect(status().isOk());

		mvc.perform(get("/auth/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
	}

	@Test
	void unEnlaceVencidoSeRechaza() throws Exception {
		String token = TokenRecuperacion.generar();
		ana.iniciarRecuperacion(TokenRecuperacion.hash(token), ahoraUtc().minusMinutes(1));
		repositorio.saveAndFlush(ana);

		restablecer(token, NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(ENLACE_INVALIDO));
		login("ana", CLAVE).andExpect(status().isOk());
	}

	@Test
	void unTokenDesconocidoSeRechazaConElMismoMensaje() throws Exception {
		restablecer(TokenRecuperacion.generar(), NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(ENLACE_INVALIDO));
	}

	@Test
	void unaContrasenaInvalidaNoConsumeElEnlace() throws Exception {
		String token = pedirEnlace("ana@pucp.edu.pe");

		restablecer(token, "123").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña debe tener de 6 a 64 caracteres."));
		restablecer(token, "ñ".repeat(40)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña es demasiado larga."));

		restablecer(token, NUEVA).andExpect(status().isOk());
	}

	@Test
	void sinTokenResponde400() throws Exception {
		restablecer("", NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El enlace de recuperación no es válido."));
	}

	@Test
	void unaCuentaDesactivadaDespuesDePedirElEnlaceYaNoPuedeUsarlo() throws Exception {
		String token = pedirEnlace("ana@pucp.edu.pe");
		ana.desactivar();
		repositorio.saveAndFlush(ana);

		restablecer(token, NUEVA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(ENLACE_INVALIDO));
	}

}

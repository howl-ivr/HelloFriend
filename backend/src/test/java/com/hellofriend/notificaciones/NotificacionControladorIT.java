package com.hellofriend.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.notificaciones.dominio.Notificacion;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;
import com.hellofriend.notificaciones.infraestructura.NotificacionRepositorio;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** La bandeja: cada persona ve y marca solo sus avisos, con cualquier rol y con token. */
class NotificacionControladorIT extends PruebaIntegracion {

	private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 2, 10, 0);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private NotificacionRepositorio repositorio;

	@Autowired
	private UsuarioRepositorio usuarios;

	@Autowired
	private ServicioJwt jwt;

	private Usuario ana;
	private Usuario luis;
	private Notificacion antigua;
	private Notificacion reciente;
	private Notificacion deLuis;

	@BeforeEach
	void crearDatos() {
		ana = usuarios.save(new Usuario("ana@pucp.edu.pe", "ana", "hash", Rol.ESTUDIANTE, INICIO));
		luis = usuarios.saveAndFlush(new Usuario("luis@pucp.edu.pe", "luis", "hash", Rol.ESTUDIANTE, INICIO));
		antigua = aviso(ana, TipoNotificacion.APROBADA, "Aprobada", "Tu incidencia fue aprobada.", INICIO);
		reciente = aviso(ana, TipoNotificacion.RESUELTA, "Resuelta", "Tu incidencia fue resuelta.",
				INICIO.plusHours(1));
		deLuis = aviso(luis, TipoNotificacion.OBSERVADA, "Observada", "Tu incidencia fue observada.",
				INICIO.plusHours(2));
		repositorio.flush();
	}

	private Notificacion aviso(Usuario destinatario, TipoNotificacion tipo, String titulo, String mensaje,
			LocalDateTime creadaEn) {
		return repositorio.save(new Notificacion(destinatario.getId(), tipo, null, null, titulo, mensaje, creadaEn));
	}

	private ResultActions como(Usuario u, MockHttpServletRequestBuilder peticion) throws Exception {
		String token = jwt.emitir(u.getId(), u.getUsuario(), u.getRol().name(), u.getVersionSesion());
		return mvc.perform(peticion.header("Authorization", "Bearer " + token));
	}

	// --- sin token

	@Test
	void sinTokenCadaEndpointResponde401() throws Exception {
		mvc.perform(get("/notificaciones")).andExpect(status().isUnauthorized());
		mvc.perform(patch("/notificaciones/" + antigua.getId() + "/leida")).andExpect(status().isUnauthorized());
		mvc.perform(patch("/notificaciones/leidas")).andExpect(status().isUnauthorized());
	}

	// --- listar

	@Test
	void listaSoloMisAvisosLosMasNuevosPrimeroConSusCampos() throws Exception {
		como(ana, get("/notificaciones")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(reciente.getId()))
				.andExpect(jsonPath("$[0].tipo").value("RESUELTA")).andExpect(jsonPath("$[0].titulo").value("Resuelta"))
				.andExpect(jsonPath("$[0].mensaje").value("Tu incidencia fue resuelta."))
				.andExpect(jsonPath("$[0].leida").value(false))
				.andExpect(jsonPath("$[0].fecha").value("2026-10-02T11:00:00"))
				.andExpect(jsonPath("$[0].incidenciaId").value((Object) null))
				.andExpect(jsonPath("$[1].id").value(antigua.getId()))
				.andExpect(jsonPath("$[0].destinatarioId").doesNotExist());
	}

	@Test
	void cualquierRolPuedeVerSuBandejaAunqueEsteVacia() throws Exception {
		for (Rol rol : Rol.values()) {
			String nombre = "u_" + rol.name().toLowerCase();
			Usuario u = usuarios.saveAndFlush(
					new Usuario(nombre + "@pucp.edu.pe", nombre.substring(0, 8), "hash", rol, INICIO));

			como(u, get("/notificaciones")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		}
	}

	// --- marcar una

	@Test
	void marcarUnaLeidaResponde204YSoloCambiaEsa() throws Exception {
		como(ana, patch("/notificaciones/" + antigua.getId() + "/leida")).andExpect(status().isNoContent());

		como(ana, get("/notificaciones")).andExpect(jsonPath("$[1].id").value(antigua.getId()))
				.andExpect(jsonPath("$[1].leida").value(true)).andExpect(jsonPath("$[0].leida").value(false));
	}

	@Test
	void marcarDosVecesEsInofensivoYConservaLaPrimeraFecha() throws Exception {
		como(ana, patch("/notificaciones/" + antigua.getId() + "/leida")).andExpect(status().isNoContent());
		LocalDateTime primera = repositorio.findById(antigua.getId()).orElseThrow().getLeidaEn();

		como(ana, patch("/notificaciones/" + antigua.getId() + "/leida")).andExpect(status().isNoContent());

		assertThat(repositorio.findById(antigua.getId()).orElseThrow().getLeidaEn()).isEqualTo(primera);
	}

	@Test
	void unaNotificacionAjenaResponde404YNoCambia() throws Exception {
		como(ana, patch("/notificaciones/" + deLuis.getId() + "/leida")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Notificación no encontrada."));

		assertThat(repositorio.findById(deLuis.getId()).orElseThrow().isLeida()).isFalse();
	}

	@Test
	void unaNotificacionInexistenteResponde404() throws Exception {
		como(ana, patch("/notificaciones/no-existe/leida")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Notificación no encontrada."));
	}

	// --- marcar todas

	@Test
	void marcarTodasLeidasSoloAfectaMisAvisos() throws Exception {
		como(ana, patch("/notificaciones/leidas")).andExpect(status().isNoContent());

		como(ana, get("/notificaciones")).andExpect(jsonPath("$[0].leida").value(true))
				.andExpect(jsonPath("$[1].leida").value(true));
		como(luis, get("/notificaciones")).andExpect(jsonPath("$[0].leida").value(false));
		assertThat(repositorio.findById(deLuis.getId()).orElseThrow().isLeida()).isFalse();
	}

	@Test
	void marcarTodasSinAvisosResponde204() throws Exception {
		Usuario marta = usuarios.saveAndFlush(
				new Usuario("marta@pucp.edu.pe", "marta", "hash", Rol.PROFESOR, INICIO));

		como(marta, patch("/notificaciones/leidas")).andExpect(status().isNoContent());
	}

	@Test
	void marcarTodasNoCambiaLaFechaDeLasQueYaEstabanLeidas() throws Exception {
		como(ana, patch("/notificaciones/" + antigua.getId() + "/leida")).andExpect(status().isNoContent());
		LocalDateTime primera = repositorio.findById(antigua.getId()).orElseThrow().getLeidaEn();

		como(ana, patch("/notificaciones/leidas")).andExpect(status().isNoContent());

		// La base guarda microsegundos; la copia en memoria de la primera lectura puede tener más decimales.
		assertThat(repositorio.findById(antigua.getId()).orElseThrow().getLeidaEn())
				.isEqualTo(primera.truncatedTo(ChronoUnit.MICROS));
	}

}

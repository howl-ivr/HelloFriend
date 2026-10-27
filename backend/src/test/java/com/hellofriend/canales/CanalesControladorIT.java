package com.hellofriend.canales;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hellofriend.PruebaApi;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/** Canales: suscripción de estudiantes y profesores, y alta y baja por el administrador. */
class CanalesControladorIT extends PruebaApi {

	private Usuario admin;
	private Usuario director;
	private Usuario profe;
	private Usuario ana;
	private Usuario luis;
	private String curso1;
	private String curso2;

	@BeforeEach
	void mundo() throws Exception {
		admin = usuario("admin", Rol.ADMINISTRADOR);
		director = usuario("direct", Rol.DIRECTOR);
		profe = usuario("profe", Rol.PROFESOR);
		ana = usuario("ana", Rol.ESTUDIANTE);
		luis = usuario("luis", Rol.ESTUDIANTE);
		curso1 = crearCurso("INF263", "Algoritmia");
		curso2 = crearCurso("INF245", "Redes");
	}

	private String crearCurso(String codigo, String nombre) throws Exception {
		return leer(
				post(admin, "/admin/cursos", json("codigo", codigo, "nombre", nombre)).andExpect(status().isCreated()),
				"$.id");
	}

	private String crearCanal(String periodo, String cursoId) throws Exception {
		return leer(
				post(admin, "/admin/canales", "{\"periodo\":\"" + periodo + "\",\"cursoIds\":[\"" + cursoId + "\"]}")
						.andExpect(status().isCreated()),
				"$[0].id");
	}

	// --- quién puede entrar

	@Test
	void sinTokenCadaEndpointResponde401() throws Exception {
		get(null, "/canales").andExpect(status().isUnauthorized());
		get(null, "/canales/x").andExpect(status().isUnauthorized());
		post(null, "/canales/x/suscripcion", null).andExpect(status().isUnauthorized());
		delete(null, "/canales/x/suscripcion").andExpect(status().isUnauthorized());
		get(null, "/periodos").andExpect(status().isUnauthorized());
		get(null, "/admin/canales").andExpect(status().isUnauthorized());
		post(null, "/admin/canales", "{}").andExpect(status().isUnauthorized());
		post(null, "/admin/canales/x/desactivar", null).andExpect(status().isUnauthorized());
	}

	@Test
	void cadaRolSoloEntraALoSuyo() throws Exception {
		get(ana, "/canales").andExpect(status().isOk());
		get(profe, "/canales").andExpect(status().isOk());
		get(director, "/canales").andExpect(status().isForbidden());
		get(admin, "/canales").andExpect(status().isForbidden());

		get(ana, "/periodos").andExpect(status().isForbidden());
		get(profe, "/periodos").andExpect(status().isForbidden());
		get(director, "/periodos").andExpect(status().isOk());
		get(admin, "/periodos").andExpect(status().isOk());

		get(ana, "/admin/canales").andExpect(status().isForbidden());
		get(director, "/admin/canales").andExpect(status().isForbidden());
		post(ana, "/admin/canales", "{}").andExpect(status().isForbidden());
		post(director, "/admin/canales/x/desactivar", null).andExpect(status().isForbidden());
	}

	// --- alta por el administrador

	@Test
	void creaUnCanalPorCursoConSuDetalle() throws Exception {
		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[\"" + curso1 + "\"]}")
				.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].nombre").value("INF263 · Algoritmia"))
				.andExpect(jsonPath("$[0].periodo").value("2026-2")).andExpect(jsonPath("$[0].activo").value(true))
				.andExpect(jsonPath("$[0].curso.codigo").value("INF263"))
				.andExpect(jsonPath("$[0].curso.id").value(curso1))
				.andExpect(jsonPath("$[0].suscrito").value(false)).andExpect(jsonPath("$[0].totalSuscritos").value(0))
				.andExpect(jsonPath("$[0].totalPublicadas").value(0))
				.andExpect(jsonPath("$[0].totalPendientes").value(0))
				.andExpect(jsonPath("$[0].fechaCreacion").isNotEmpty());
	}

	@Test
	void creaVariosCanalesAlMismoTiempoYIgnoraCursosRepetidos() throws Exception {
		post(admin, "/admin/canales",
				"{\"periodo\":\"2026-1\",\"cursoIds\":[\"" + curso1 + "\",\"" + curso2 + "\",\"" + curso1 + "\"]}")
				.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void rechazaPeriodosInvalidos() throws Exception {
		for (String periodo : List.of("2026-3", "26-1", "abcd", "2026-0")) {
			post(admin, "/admin/canales", "{\"periodo\":\"" + periodo + "\",\"cursoIds\":[\"" + curso1 + "\"]}")
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
							.value("El periodo debe tener el formato año-semestre, por ejemplo 2026-2."));
		}
		post(admin, "/admin/canales", "{\"cursoIds\":[\"" + curso1 + "\"]}").andExpect(status().isBadRequest());
		post(admin, "/admin/canales", "{\"periodo\":\"1999-1\",\"cursoIds\":[\"" + curso1 + "\"]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El año del periodo debe estar entre 2000 y 2100."));
	}

	@Test
	void exigeAlMenosUnCursoQueExista() throws Exception {
		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[]}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Elige al menos un curso."));
		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\"}").andExpect(status().isBadRequest());
		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[\"no-existe\"]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Uno de los cursos no existe."));
	}

	@Test
	void unCanalPorCursoYPeriodo() throws Exception {
		crearCanal("2026-2", curso1);

		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[\"" + curso1 + "\"]}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ya existe un canal para ese curso y periodo."));
		crearCanal("2026-1", curso1);
	}

	// --- lectura

	@Test
	void elAdministradorListaYFiltraPorPeriodo() throws Exception {
		crearCanal("2026-1", curso1);
		crearCanal("2026-2", curso1);
		crearCanal("2026-2", curso2);

		get(admin, "/admin/canales").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
		get(admin, "/admin/canales", "periodo", "2026-2").andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].periodo").value("2026-2"));
		get(admin, "/admin/canales", "periodo", "2025-1").andExpect(jsonPath("$", hasSize(0)));
		get(admin, "/admin/canales", "periodo", "mal").andExpect(status().isBadRequest());
	}

	@Test
	void losPeriodosSalenSinRepetirDelMasNuevoAlMasViejo() throws Exception {
		crearCanal("2026-1", curso1);
		crearCanal("2026-2", curso1);
		crearCanal("2026-2", curso2);

		get(director, "/periodos").andExpect(status().isOk()).andExpect(jsonPath("$", contains("2026-2", "2026-1")));
	}

	@Test
	void estudiantesYProfesoresVenTodosLosCanalesSinCamposDeAdministrador() throws Exception {
		crearCanal("2026-2", curso1);

		get(ana, "/canales").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].totalPendientes").doesNotExist())
				.andExpect(jsonPath("$[0].fechaCreacion").doesNotExist());
		get(profe, "/canales").andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void detalleDeUnCanalOInexistente() throws Exception {
		String canal = crearCanal("2026-2", curso1);

		get(ana, "/canales/" + canal).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(canal));
		get(ana, "/canales/no-existe").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("El canal no existe."));
	}

	// --- suscripción

	@Test
	void suscribirseYDesuscribirseActualizaLosContadores() throws Exception {
		String canal = crearCanal("2026-2", curso1);

		post(ana, "/canales/" + canal + "/suscripcion", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.suscrito").value(true)).andExpect(jsonPath("$.totalSuscritos").value(1));
		post(profe, "/canales/" + canal + "/suscripcion", null).andExpect(jsonPath("$.totalSuscritos").value(2));
		get(luis, "/canales/" + canal).andExpect(jsonPath("$.suscrito").value(false))
				.andExpect(jsonPath("$.totalSuscritos").value(2));

		delete(ana, "/canales/" + canal + "/suscripcion").andExpect(status().isOk())
				.andExpect(jsonPath("$.suscrito").value(false)).andExpect(jsonPath("$.totalSuscritos").value(1));
	}

	@Test
	void suscribirseDosVecesNoDuplica() throws Exception {
		String canal = crearCanal("2026-2", curso1);

		post(ana, "/canales/" + canal + "/suscripcion", null).andExpect(status().isOk());
		post(ana, "/canales/" + canal + "/suscripcion", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalSuscritos").value(1));
	}

	@Test
	void unCanalDesactivadoNoAdmiteSuscripcionesNuevasPeroSiBajas() throws Exception {
		String canal = crearCanal("2026-2", curso1);
		post(ana, "/canales/" + canal + "/suscripcion", null).andExpect(status().isOk());
		post(admin, "/admin/canales/" + canal + "/desactivar", null).andExpect(status().isOk());

		post(luis, "/canales/" + canal + "/suscripcion", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Este canal está desactivado."));
		delete(ana, "/canales/" + canal + "/suscripcion").andExpect(status().isOk())
				.andExpect(jsonPath("$.totalSuscritos").value(0));
	}

	@Test
	void suscribirseAUnCanalInexistenteResponde404() throws Exception {
		post(ana, "/canales/no-existe/suscripcion", null).andExpect(status().isNotFound());
		delete(ana, "/canales/no-existe/suscripcion").andExpect(status().isNotFound());
	}

	// --- desactivar

	@Test
	void desactivarUnCanalLoMarcaInactivoYNoSePuedeRepetir() throws Exception {
		String canal = crearCanal("2026-2", curso1);

		post(admin, "/admin/canales/" + canal + "/desactivar", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));
		post(admin, "/admin/canales/" + canal + "/desactivar", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El canal ya está desactivado."));
		post(admin, "/admin/canales/no-existe/desactivar", null).andExpect(status().isNotFound());
		get(ana, "/canales/" + canal).andExpect(jsonPath("$.activo").value(false));
	}

	// --- auditoría

	@Test
	void crearYDesactivarDejanSuHuellaEnLaAuditoria() throws Exception {
		String canal = crearCanal("2026-2", curso1);
		post(admin, "/admin/canales/" + canal + "/desactivar", null).andExpect(status().isOk());

		org.assertj.core.api.Assertions
				.assertThat(contar(
						"SELECT COUNT(*) FROM auditoria_evento WHERE accion='CANAL_CREADO' AND actor_id=? AND detalle=?",
						admin.getId(), canal))
				.isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(contar(
				"SELECT COUNT(*) FROM auditoria_evento WHERE accion='CANAL_DESACTIVADO' AND actor_id=? AND detalle=?",
				admin.getId(), canal)).isEqualTo(1);
	}

}

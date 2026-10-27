package com.hellofriend.incidencias;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Estadísticas del director y reportes de auditoría del administrador. */
class ReportesIncidenciasIT extends PruebaIncidencias {

	/** Una pendiente de ana, una publicada de luis y una resuelta de marta; marta confirma la de luis. */
	private String[] tresIncidencias() throws Exception {
		String pendiente = registrar(ana, "Incidencia pendiente de ana");
		String publicada = publicada(luis, "Incidencia publicada de luis");
		String resuelta = publicada(marta, "Incidencia resuelta de marta");
		decidir(resuelta, "resolver", json("solucion", "Se cambió el proyector del aula."));
		post(marta, "/incidencias/" + publicada + "/confirmacion", null).andExpect(status().isOk());
		return new String[]{pendiente, publicada, resuelta};
	}

	// --- estadísticas del director

	@Test
	void lasEstadisticasContabilizanPorEstadoCursoYCategoria() throws Exception {
		String[] ids = tresIncidencias();

		get(director, "/director/estadisticas").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.resueltas").value(1)).andExpect(jsonPath("$.tiempoPromedioHoras").isNotEmpty())
				.andExpect(jsonPath("$.porEstado[?(@.estado == 'PENDIENTE')].total", contains(1)))
				.andExpect(jsonPath("$.porEstado[?(@.estado == 'PUBLICADA')].total", contains(1)))
				.andExpect(jsonPath("$.porEstado[?(@.estado == 'RESUELTA')].total", contains(1)))
				.andExpect(jsonPath("$.porEstado[?(@.estado == 'RECHAZADA')].total", contains(0)))
				.andExpect(jsonPath("$.porCurso[0].nombre").value("INF263 · Algoritmia"))
				.andExpect(jsonPath("$.porCurso[0].total").value(3))
				.andExpect(jsonPath("$.porCategoria[?(@.nombre == '" + categoria + "')].total", contains(3)))
				.andExpect(jsonPath("$.masConfirmadas", hasSize(1)))
				.andExpect(jsonPath("$.masConfirmadas[0].id").value(ids[1]))
				.andExpect(jsonPath("$.masConfirmadas[0].confirmaciones").value(1));
	}

	@Test
	void laCategoriaSinIncidenciasApareceEnCero() throws Exception {
		get(director, "/director/estadisticas").andExpect(jsonPath("$.total").value(0))
				.andExpect(jsonPath("$.resueltas").value(0))
				.andExpect(jsonPath("$.tiempoPromedioHoras").value((Object) null))
				.andExpect(jsonPath("$.porCategoria", hasSize(8))).andExpect(jsonPath("$.porEstado", hasSize(6)))
				.andExpect(jsonPath("$.masConfirmadas", hasSize(0)));
	}

	@Test
	void lasEstadisticasSeFiltranPorPeriodoYFechas() throws Exception {
		tresIncidencias();

		get(director, "/director/estadisticas", "periodo", "2026-2").andExpect(jsonPath("$.total").value(3));
		get(director, "/director/estadisticas", "periodo", "2025-1").andExpect(jsonPath("$.total").value(0));
		get(director, "/director/estadisticas", "desde", "2000-01-01", "hasta", "2099-12-31")
				.andExpect(jsonPath("$.total").value(3));
		get(director, "/director/estadisticas", "desde", "2099-01-01").andExpect(jsonPath("$.total").value(0));
		get(director, "/director/estadisticas", "hasta", "2000-01-01").andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void lasEstadisticasValidanLasFechas() throws Exception {
		get(director, "/director/estadisticas", "desde", "2026-12-01", "hasta", "2026-01-01")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La fecha inicial no puede ser posterior a la final."));
		get(director, "/director/estadisticas", "desde", "hoy").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La fecha debe tener el formato AAAA-MM-DD."));
	}

	@Test
	void soloElDirectorVeLasEstadisticas() throws Exception {
		get(null, "/director/estadisticas").andExpect(status().isUnauthorized());
		get(ana, "/director/estadisticas").andExpect(status().isForbidden());
		get(profe, "/director/estadisticas").andExpect(status().isForbidden());
		get(admin, "/director/estadisticas").andExpect(status().isForbidden());
	}

	// --- reportes del administrador

	@Test
	void elAdministradorListaLosReportesConAutorYVersiones() throws Exception {
		String[] ids = tresIncidencias();
		put(ana, "/incidencias/" + ids[0],
				json("categoria", categoria, "titulo", "Incidencia pendiente de ana (editada)",
						"descripcion", DESCRIPCION))
				.andExpect(status().isOk());

		get(admin, "/admin/reportes").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[?(@.id == '" + ids[0] + "')].autor", contains("ana")))
				.andExpect(jsonPath("$[?(@.id == '" + ids[0] + "')].versiones", contains(2)))
				.andExpect(jsonPath("$[?(@.id == '" + ids[1] + "')].confirmaciones", contains(1)))
				.andExpect(jsonPath("$[0].canal.nombre").value("INF263 · Algoritmia"))
				.andExpect(jsonPath("$[0].descripcion").doesNotExist());
	}

	@Test
	void losReportesSeFiltranPorEstadoCursoFechasYPersonaInvolucrada() throws Exception {
		String[] ids = tresIncidencias();
		String otroCurso = leer(post(admin, "/admin/cursos", json("codigo", "INF245", "nombre", "Redes"))
				.andExpect(status().isCreated()), "$.id");

		get(admin, "/admin/reportes", "estado", "PENDIENTE").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(ids[0]));
		get(admin, "/admin/reportes", "estado", "PUBLICADA").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(ids[1]));
		get(admin, "/admin/reportes", "cursoId", curso).andExpect(jsonPath("$", hasSize(3)));
		get(admin, "/admin/reportes", "cursoId", otroCurso).andExpect(jsonPath("$", hasSize(0)));
		get(admin, "/admin/reportes", "desde", "2099-01-01").andExpect(jsonPath("$", hasSize(0)));
		get(admin, "/admin/reportes", "usuario", "ana").andExpect(jsonPath("$", hasSize(1)));
		get(admin, "/admin/reportes", "usuario", "MARTA").andExpect(jsonPath("$", hasSize(2)));
		get(admin, "/admin/reportes", "usuario", "direct").andExpect(jsonPath("$", hasSize(2)));
		get(admin, "/admin/reportes", "usuario", "nadie").andExpect(jsonPath("$", hasSize(0)));
		get(admin, "/admin/reportes", "desde", "ayer").andExpect(status().isBadRequest());
	}

	@Test
	void soloElAdministradorVeLosReportes() throws Exception {
		String id = publicada(ana);

		get(null, "/admin/reportes").andExpect(status().isUnauthorized());
		get(director, "/admin/reportes").andExpect(status().isForbidden());
		get(ana, "/admin/reportes/" + id).andExpect(status().isForbidden());
		get(director, "/admin/reportes/" + id).andExpect(status().isForbidden());
		get(admin, "/admin/reportes/no-existe").andExpect(status().isNotFound());
	}

	@Test
	void elReporteDeUnaIncidenciaTrae_elAutorConCorreoYTodosLosEventos() throws Exception {
		String id = publicada(ana);
		suscribir(luis);
		post(luis, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isOk());
		post(luis, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isOk());

		get(admin, "/admin/reportes/" + id).andExpect(status().isOk())
				.andExpect(jsonPath("$.autor.usuario").value("ana"))
				.andExpect(jsonPath("$.autor.correo").value("ana@pucp.edu.pe"))
				.andExpect(jsonPath("$.versiones", hasSize(1)))
				.andExpect(jsonPath("$.eventos[?(@.tipo == 'CREADA')].responsable", contains("ana")))
				.andExpect(jsonPath("$.eventos[?(@.tipo == 'APROBADA')].responsable", contains("direct")))
				.andExpect(jsonPath("$.eventos[?(@.tipo == 'CONFIRMADA')].responsable", contains("luis")))
				.andExpect(jsonPath("$.eventos[?(@.tipo == 'CONFIRMACION_RETIRADA')].responsable", contains("luis")));
	}

	@Test
	void laConfirmacionConIdentidadSoloLaVeElAdministrador() throws Exception {
		String id = publicada(ana);
		suscribir(luis);
		post(luis, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isOk());

		String comoDirector = get(director, "/incidencias/" + id).andReturn().getResponse().getContentAsString();
		String comoEstudiante = get(ana, "/incidencias/" + id).andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(comoDirector).doesNotContain("luis");
		org.assertj.core.api.Assertions.assertThat(comoEstudiante).doesNotContain("luis");
	}

	// --- auditoría general

	@Test
	void laAuditoriaListaLasAccionesConSuActorDelMasNuevoAlMasViejo() throws Exception {
		String id = publicada(ana);
		post(director, "/director/incidencias/" + id + "/atender", null).andExpect(status().isOk());

		get(admin, "/admin/auditoria").andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.accion == 'CURSO_REGISTRADO')].actor", contains("admin")))
				.andExpect(jsonPath("$[?(@.accion == 'CANAL_CREADO')].actor", contains("admin")))
				.andExpect(jsonPath("$[?(@.accion == 'INCIDENCIA_REGISTRADA')].actor", contains("ana")))
				.andExpect(jsonPath("$[?(@.accion == 'INCIDENCIA_APROBADA')].actor", contains("direct")))
				.andExpect(jsonPath("$[0].accion").value("INCIDENCIA_EN_ATENCION"))
				.andExpect(jsonPath("$[?(@.accion == 'INCIDENCIA_REGISTRADA')].incidenciaId", contains(id)));
	}

	@Test
	void soloElAdministradorVeLaAuditoria() throws Exception {
		get(null, "/admin/auditoria").andExpect(status().isUnauthorized());
		get(director, "/admin/auditoria").andExpect(status().isForbidden());
		get(ana, "/admin/auditoria").andExpect(status().isForbidden());
	}

}

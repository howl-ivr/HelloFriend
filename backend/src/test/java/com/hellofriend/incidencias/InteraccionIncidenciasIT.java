package com.hellofriend.incidencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/** Confirmaciones, reacciones y comentarios, y las listas que ven estudiantes, profesores y el director. */
class InteraccionIncidenciasIT extends PruebaIncidencias {

	// --- confirmar

	@Test
	void confirmarAlternaYCuentaLasConfirmaciones() throws Exception {
		String id = publicada(ana);
		suscribir(luis);

		post(luis, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.confirmaciones").value(1)).andExpect(jsonPath("$.confirmadaPorMi").value(true));
		get(luis, "/incidencias/" + id).andExpect(jsonPath("$.confirmadaPorMi").value(true))
				.andExpect(jsonPath("$.confirmaciones").value(1));
		post(luis, "/incidencias/" + id + "/confirmacion", null).andExpect(jsonPath("$.confirmaciones").value(0))
				.andExpect(jsonPath("$.confirmadaPorMi").value(false));
	}

	@Test
	void laPrioridadSubeConLasConfirmaciones() throws Exception {
		String id = publicada(ana);
		List<Usuario> estudiantes = List.of(luis, marta, usuario("e3", Rol.ESTUDIANTE), usuario("e4", Rol.ESTUDIANTE),
				usuario("e5", Rol.ESTUDIANTE));
		for (Usuario e : estudiantes) {
			suscribir(e);
		}

		post(estudiantes.get(0), "/incidencias/" + id + "/confirmacion", null);
		get(luis, "/incidencias/" + id).andExpect(jsonPath("$.prioridad").value("BAJA"));
		post(estudiantes.get(1), "/incidencias/" + id + "/confirmacion", null);
		get(luis, "/incidencias/" + id).andExpect(jsonPath("$.prioridad").value("MEDIA"));
		for (Usuario e : estudiantes.subList(2, 5)) {
			post(e, "/incidencias/" + id + "/confirmacion", null);
		}
		get(luis, "/incidencias/" + id).andExpect(jsonPath("$.prioridad").value("ALTA"))
				.andExpect(jsonPath("$.confirmaciones").value(5));
	}

	@Test
	void noSePuedeConfirmarLaPropiaIncidenciaNiSinSuscripcionNiSiNoEstaPublicada() throws Exception {
		String id = publicada(ana);
		String pendiente = registrar(ana, "Otra incidencia pendiente");
		suscribir(luis);

		post(ana, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No puedes confirmar esta incidencia."));
		post(marta, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isForbidden());
		post(luis, "/incidencias/" + pendiente + "/confirmacion", null).andExpect(status().isForbidden());
		post(luis, "/incidencias/no-existe/confirmacion", null).andExpect(status().isNotFound());
	}

	@Test
	void soloLosEstudiantesConfirman() throws Exception {
		String id = publicada(ana);
		post(null, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isUnauthorized());
		for (Usuario otro : List.of(profe, director, admin)) {
			post(otro, "/incidencias/" + id + "/confirmacion", null).andExpect(status().isForbidden());
		}
	}

	// --- reaccionar

	@Test
	void unaReaccionPorPersonaSeReemplazaYSeRetira() throws Exception {
		String id = publicada(ana);
		suscribir(luis);
		suscribir(marta);

		put(luis, "/incidencias/" + id + "/reaccion", json("tipo", "APOYO")).andExpect(status().isOk())
				.andExpect(jsonPath("$.miReaccion").value("APOYO")).andExpect(jsonPath("$.reacciones.APOYO").value(1));
		put(marta, "/incidencias/" + id + "/reaccion", json("tipo", "APOYO"))
				.andExpect(jsonPath("$.reacciones.APOYO").value(2));
		put(luis, "/incidencias/" + id + "/reaccion", json("tipo", "URGENTE"))
				.andExpect(jsonPath("$.miReaccion").value("URGENTE"))
				.andExpect(jsonPath("$.reacciones.APOYO").value(1))
				.andExpect(jsonPath("$.reacciones.URGENTE").value(1));
		put(luis, "/incidencias/" + id + "/reaccion", json("tipo", null))
				.andExpect(jsonPath("$.miReaccion").value((Object) null))
				.andExpect(jsonPath("$.reacciones.URGENTE").value(0));
		get(marta, "/incidencias/" + id).andExpect(jsonPath("$.miReaccion").value("APOYO"));
	}

	@Test
	void reaccionarValidaElTipoYLaSuscripcion() throws Exception {
		String id = publicada(ana);
		suscribir(luis);

		put(luis, "/incidencias/" + id + "/reaccion", json("tipo", "ENOJO")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Tipo de reacción desconocido."));
		put(marta, "/incidencias/" + id + "/reaccion", json("tipo", "APOYO")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No puedes reaccionar a esta incidencia."));
		put(profe, "/incidencias/" + id + "/reaccion", json("tipo", "APOYO")).andExpect(status().isForbidden());
		put(luis, "/incidencias/no-existe/reaccion", json("tipo", "APOYO")).andExpect(status().isNotFound());
	}

	// --- comentar

	@Test
	void comentarConUnaOpcionPredefinidaLoAgregaAlDetalle() throws Exception {
		String id = publicada(ana);
		suscribir(luis);
		String opcion = leer(get(luis, "/comentarios-predefinidos"), "$[0].id");
		String texto = leer(get(luis, "/comentarios-predefinidos"), "$[0].texto");

		post(luis, "/incidencias/" + id + "/comentarios", json("comentarioPredefinidoId", opcion))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.texto").value(texto))
				.andExpect(jsonPath("$.id").isNotEmpty()).andExpect(jsonPath("$.fecha").isNotEmpty());

		get(luis, "/incidencias/" + id).andExpect(jsonPath("$.totalComentarios").value(1))
				.andExpect(jsonPath("$.comentarios", hasSize(1)))
				.andExpect(jsonPath("$.comentarios[0].texto").value(texto));
	}

	@Test
	void comentarValidaLaOpcionYLaSuscripcion() throws Exception {
		String id = publicada(ana);
		suscribir(luis);
		String opcion = leer(get(luis, "/comentarios-predefinidos"), "$[0].id");

		post(luis, "/incidencias/" + id + "/comentarios", json("comentarioPredefinidoId", "no-existe"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Elige un comentario de la lista."));
		post(luis, "/incidencias/" + id + "/comentarios", "{}").andExpect(status().isBadRequest());
		post(marta, "/incidencias/" + id + "/comentarios", json("comentarioPredefinidoId", opcion))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Suscríbete al canal para comentar."));
		post(director, "/incidencias/" + id + "/comentarios", json("comentarioPredefinidoId", opcion))
				.andExpect(status().isForbidden());
	}

	// --- feed y listas

	@Test
	void elFeedMuestraSoloLoPublicadoDeLosCanalesSuscritos() throws Exception {
		String publicada = publicada(ana, "Incidencia publicada del aula");
		registrar(ana, "Incidencia todavia pendiente");
		String rechazada = registrar(ana, "Incidencia que sera rechazada");
		post(director, "/director/incidencias/" + rechazada + "/rechazar", json("comentario", "No corresponde."));
		suscribir(profe);

		get(ana, "/feed").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(publicada)).andExpect(jsonPath("$[0].estado").value("PUBLICADA"));
		get(profe, "/feed").andExpect(jsonPath("$", hasSize(1)));
		get(luis, "/feed").andExpect(jsonPath("$", hasSize(0)));
		get(director, "/feed").andExpect(status().isForbidden());
		get(null, "/feed").andExpect(status().isUnauthorized());
	}

	@Test
	void elFeedPorCanalExigeSuscripcionYSeFiltraPorPrioridad() throws Exception {
		publicada(ana);

		get(luis, "/feed", "canalId", canal).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Suscríbete al canal para ver sus incidencias."));
		get(ana, "/feed", "canalId", canal).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
		get(ana, "/feed", "prioridad", "BAJA").andExpect(jsonPath("$", hasSize(1)));
		get(ana, "/feed", "prioridad", "ALTA").andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void elFeedSePuedeOrdenarPorPrioridad() throws Exception {
		String primera = publicada(ana, "Primera incidencia publicada");
		String segunda = publicada(ana, "Segunda incidencia publicada");
		suscribir(luis);
		post(luis, "/incidencias/" + primera + "/confirmacion", null);

		get(ana, "/feed", "orden", "prioridad").andExpect(jsonPath("$[0].id").value(primera))
				.andExpect(jsonPath("$[1].id").value(segunda));
		get(ana, "/feed", "orden", "recientes").andExpect(jsonPath("$[0].id").value(segunda));
	}

	@Test
	void lasDestacadasSonLasPublicadasConConfirmacionesDeLosCanalesSuscritos() throws Exception {
		String confirmada = publicada(ana, "Incidencia con confirmaciones");
		publicada(ana, "Incidencia sin confirmaciones");
		suscribir(luis);
		post(luis, "/incidencias/" + confirmada + "/confirmacion", null);

		get(ana, "/feed/destacadas").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(confirmada));
		get(marta, "/feed/destacadas").andExpect(jsonPath("$", hasSize(0)));
		get(director, "/feed/destacadas").andExpect(status().isForbidden());
	}

	@Test
	void lasDestacadasNoPasanDeCincoNiIncluyenLasResueltas() throws Exception {
		suscribir(luis);
		for (int n = 1; n <= 6; n++) {
			String id = publicada(ana, "Incidencia destacada número " + n);
			post(luis, "/incidencias/" + id + "/confirmacion", null);
		}
		String resuelta = publicada(ana, "Incidencia ya resuelta");
		post(luis, "/incidencias/" + resuelta + "/confirmacion", null);
		decidir(resuelta, "resolver", json("solucion", "Se cambió el proyector del aula."));

		get(luis, "/feed/destacadas").andExpect(jsonPath("$", hasSize(5)));
	}

	@Test
	void lasIncidenciasDeUnCanalExigenSuscripcion() throws Exception {
		String id = publicada(ana);
		registrar(ana, "Incidencia pendiente del canal");

		get(luis, "/canales/" + canal + "/incidencias").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Suscríbete al canal para ver sus incidencias."));
		get(ana, "/canales/" + canal + "/incidencias").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(id));
		get(director, "/canales/" + canal + "/incidencias").andExpect(status().isForbidden());
	}

	@Test
	void misIncidenciasMuestraTodoLoMioEnCualquierEstadoLasMasNuevasPrimero() throws Exception {
		String primera = registrar(ana, "Mi primera incidencia");
		String segunda = registrar(ana, "Mi segunda incidencia");
		registrar(luis, "Incidencia de otra persona");
		decidir(primera, "aprobar", null);

		get(ana, "/mis-incidencias").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(segunda)).andExpect(jsonPath("$[1].estado").value("PUBLICADA"))
				.andExpect(jsonPath("$[1].revision.decision").value("APROBADA"));
		get(profe, "/mis-incidencias").andExpect(status().isForbidden());
		get(director, "/mis-incidencias").andExpect(status().isForbidden());
	}

	@Test
	void elDirectorListaPorEstadoYPublicadaEsElNombreDeAprobada() throws Exception {
		String pendiente = registrar(ana, "Incidencia pendiente de revisión");
		String aprobada = publicada(luis, "Incidencia ya aprobada");

		get(director, "/director/incidencias").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(pendiente)).andExpect(jsonPath("$[0].autor.usuario").value("ana"));
		get(director, "/director/incidencias", "estado", "PUBLICADA").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(aprobada));
		get(director, "/director/incidencias", "estado", "RESUELTA").andExpect(jsonPath("$", hasSize(0)));
		get(director, "/director/incidencias", "estado", "INVENTADO").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Estado desconocido."));
		get(ana, "/director/incidencias").andExpect(status().isForbidden());
		get(admin, "/director/incidencias").andExpect(status().isForbidden());
	}

	@Test
	void lasPendientesSalenDeLasMasAntiguasALasMasNuevas() throws Exception {
		String primera = registrar(ana, "Primera pendiente");
		String segunda = registrar(luis, "Segunda pendiente");

		get(director, "/director/incidencias").andExpect(jsonPath("$[*].id", contains(primera, segunda)));
	}

	@Test
	void elEstadoDeUnaIncidenciaCambiaLosContadoresDelCanal() throws Exception {
		String id = registrar(ana);
		get(admin, "/admin/canales").andExpect(jsonPath("$[0].totalPendientes").value(1));

		decidir(id, "aprobar", null);
		decidir(id, "atender", null);

		get(admin, "/admin/canales").andExpect(jsonPath("$[0].totalPendientes").value(0))
				.andExpect(jsonPath("$[0].totalPublicadas").value(1));
		assertThat(contar("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id=?", canal)).isEqualTo(1);
	}

}

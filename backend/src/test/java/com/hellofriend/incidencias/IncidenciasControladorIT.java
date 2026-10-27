package com.hellofriend.incidencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.hellofriend.usuarios.dominio.Rol;

/** Registro, edición y revisión de incidencias: quién puede, en qué estado y qué avisos se generan. */
class IncidenciasControladorIT extends PruebaIncidencias {

	// --- catálogos

	@Test
	void losCatalogosSolicitanSesionYLosMotivosSoloLosVeElDirector() throws Exception {
		get(null, "/categorias").andExpect(status().isUnauthorized());
		get(ana, "/categorias").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
		get(ana, "/comentarios-predefinidos").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[0].id").isNotEmpty()).andExpect(jsonPath("$[0].texto").isNotEmpty());
		get(director, "/motivos-rechazo").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)));
		get(ana, "/motivos-rechazo").andExpect(status().isForbidden());
		get(admin, "/motivos-rechazo").andExpect(status().isForbidden());
	}

	// --- registrar

	@Test
	void unEstudianteRegistraUnaIncidenciaPendienteYQuedaSuscritoAlCanal() throws Exception {
		post(ana, "/incidencias", contenido(TITULO)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.titulo").value(TITULO)).andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.categoria").value(categoria)).andExpect(jsonPath("$.esMia").value(true))
				.andExpect(jsonPath("$.autor").value((Object) null)).andExpect(jsonPath("$.confirmaciones").value(0))
				.andExpect(jsonPath("$.prioridad").value("BAJA")).andExpect(jsonPath("$.puedeEditar").value(true))
				.andExpect(jsonPath("$.puedeReenviar").value(false))
				.andExpect(jsonPath("$.canal.nombre").value("INF263 · Algoritmia"))
				.andExpect(jsonPath("$.canal.periodo").value("2026-2"))
				.andExpect(jsonPath("$.revision").value((Object) null));

		get(ana, "/canales/" + canal).andExpect(jsonPath("$.suscrito").value(true));
		get(admin, "/admin/canales").andExpect(jsonPath("$[0].totalPendientes").value(1))
				.andExpect(jsonPath("$[0].totalPublicadas").value(0));
	}

	@Test
	void soloLosEstudiantesRegistranIncidencias() throws Exception {
		post(null, "/incidencias", contenido(TITULO)).andExpect(status().isUnauthorized());
		post(profe, "/incidencias", contenido(TITULO)).andExpect(status().isForbidden());
		post(director, "/incidencias", contenido(TITULO)).andExpect(status().isForbidden());
		post(admin, "/incidencias", contenido(TITULO)).andExpect(status().isForbidden());
	}

	@Test
	void validaElContenidoAlRegistrar() throws Exception {
		post(ana, "/incidencias",
				json("canalId", canal, "categoria", categoria, "titulo", "abcd", "descripcion", DESCRIPCION))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El título debe tener de 5 a 100 caracteres."));
		post(ana, "/incidencias",
				json("canalId", canal, "categoria", categoria, "titulo", TITULO, "descripcion", "muy corta"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La descripción debe tener de 20 a 1000 caracteres."));
		post(ana, "/incidencias",
				json("canalId", canal, "categoria", "No existe", "titulo", TITULO, "descripcion", DESCRIPCION))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Elige una categoría de la lista."));
		post(ana, "/incidencias", json("categoria", categoria, "titulo", TITULO, "descripcion", DESCRIPCION))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Elige un canal."));
		post(ana, "/incidencias",
				json("canalId", "no-existe", "categoria", categoria, "titulo", TITULO, "descripcion", DESCRIPCION))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").value("El canal no existe."));
		assertThat(contar("SELECT COUNT(*) FROM incidencias_incidencia")).isZero();
	}

	@Test
	void noSeRegistraEnUnCanalDesactivado() throws Exception {
		post(admin, "/admin/canales/" + canal + "/desactivar", null).andExpect(status().isOk());

		post(ana, "/incidencias", contenido(TITULO)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Este canal está desactivado."));
	}

	@Test
	void registrarGuardaLaVersionUnoElHistorialYLaAuditoria() throws Exception {
		String id = registrar(ana);

		assertThat(contar("SELECT COUNT(*) FROM incidencias_version WHERE incidencia_id=? AND numero=1", id))
				.isEqualTo(1);
		assertThat(contar(
				"SELECT COUNT(*) FROM incidencias_historial_estado WHERE incidencia_id=? AND tipo_evento='CREADA'",
				id)).isEqualTo(1);
		assertThat(
				contar("SELECT COUNT(*) FROM auditoria_evento WHERE accion='INCIDENCIA_REGISTRADA' AND incidencia_id=? "
						+ "AND actor_id=?", id, ana.getId()))
				.isEqualTo(1);
	}

	// --- ver el detalle

	@Test
	void unaIncidenciaPendienteSoloLaVenElAutorYElDirector() throws Exception {
		String id = registrar(ana);

		get(ana, "/incidencias/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.autor").value((Object) null));
		get(director, "/incidencias/" + id).andExpect(status().isOk())
				.andExpect(jsonPath("$.autor.usuario").value("ana")).andExpect(jsonPath("$.versiones", hasSize(1)));
		get(luis, "/incidencias/" + id).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("La incidencia no existe."));
		get(profe, "/incidencias/" + id).andExpect(status().isNotFound());
		get(null, "/incidencias/" + id).andExpect(status().isUnauthorized());
	}

	@Test
	void elAdministradorNoConsultaIncidenciasPorEsteEndpoint() throws Exception {
		String id = publicada(ana);

		get(admin, "/incidencias/" + id).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("El administrador consulta incidencias desde la auditoría."));
	}

	@Test
	void unaIncidenciaPublicadaExigeEstarSuscritoAlCanal() throws Exception {
		String id = publicada(ana);

		get(luis, "/incidencias/" + id).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Suscríbete al canal para ver esta incidencia."));
		suscribir(luis);
		get(luis, "/incidencias/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"))
				.andExpect(jsonPath("$.autor").value((Object) null)).andExpect(jsonPath("$.esMia").value(false))
				.andExpect(jsonPath("$.puedeConfirmar").value(true)).andExpect(jsonPath("$.puedeEditar").value(false));
		get(ana, "/incidencias/no-existe").andExpect(status().isNotFound());
	}

	// --- editar

	@Test
	void elAutorEditaYSeCreaUnaVersionNueva() throws Exception {
		String id = registrar(ana);

		put(ana, "/incidencias/" + id, json("categoria", categoria, "titulo", "Aula 301 sin proyector (actualizado)",
				"descripcion", DESCRIPCION)).andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Aula 301 sin proyector (actualizado)"));

		get(director, "/incidencias/" + id).andExpect(jsonPath("$.versiones", hasSize(2)))
				.andExpect(jsonPath("$.historial[?(@.tipo == 'EDITADA')].responsable").value(hasSize(1)));
		get(ana, "/incidencias/" + id).andExpect(jsonPath("$.versiones").doesNotExist())
				.andExpect(jsonPath("$.historial[?(@.tipo == 'EDITADA')]").value(hasSize(0)));
	}

	@Test
	void editarExigeSerElAutorYUnEstadoEditable() throws Exception {
		String id = registrar(ana);
		String cambio = json("categoria", categoria, "titulo", "Otro título distinto", "descripcion", DESCRIPCION);

		put(luis, "/incidencias/" + id, cambio).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Solo el autor puede editar su incidencia."));
		put(profe, "/incidencias/" + id, cambio).andExpect(status().isForbidden());
		put(ana, "/incidencias/" + id, contenido(TITULO)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("No hay cambios que guardar."));
		put(ana, "/incidencias/" + id, json("categoria", categoria, "titulo", "x", "descripcion", DESCRIPCION))
				.andExpect(status().isBadRequest());

		decidir(id, "aprobar", null);
		put(ana, "/incidencias/" + id, cambio).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Solo se puede editar una incidencia pendiente u observada."));
		put(ana, "/incidencias/no-existe", cambio).andExpect(status().isNotFound());
	}

	// --- decisiones del director

	@Test
	void aprobarPublicaYAvisaAlAutorYALosSuscritos() throws Exception {
		String id = registrar(ana);
		suscribir(luis);

		post(director, "/director/incidencias/" + id + "/aprobar", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PUBLICADA"))
				.andExpect(jsonPath("$.revision.decision").value("APROBADA"))
				.andExpect(jsonPath("$.autor.usuario").value("ana"));

		for (var destinatario : new com.hellofriend.usuarios.dominio.Usuario[]{ana, luis}) {
			get(destinatario, "/notificaciones").andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].tipo").value("APROBADA"))
					.andExpect(jsonPath("$[0].incidenciaId").value(id))
					.andExpect(jsonPath("$[0].mensaje").value(containsString("«" + TITULO + "»")))
					.andExpect(jsonPath("$[0].mensaje").value(containsString("PUBLICADA")));
		}
		get(marta, "/notificaciones").andExpect(jsonPath("$", hasSize(0)));
		get(admin, "/admin/canales").andExpect(jsonPath("$[0].totalPublicadas").value(1))
				.andExpect(jsonPath("$[0].totalPendientes").value(0));
	}

	@Test
	void elAvisoNuncaIncluyeAlAutorNiElTextoDeLaDescripcion() throws Exception {
		var autora = usuario("zulema", Rol.ESTUDIANTE);
		String id = registrar(autora);
		suscribir(luis);
		decidir(id, "aprobar", null);

		var aviso = get(luis, "/notificaciones");
		String titulo = leer(aviso, "$[0].titulo");
		String mensaje = leer(aviso, "$[0].mensaje");
		assertThat(titulo + " " + mensaje).doesNotContainIgnoringCase("zulema").doesNotContain(DESCRIPCION);
		assertThat(titulo).isEqualTo("Actualización de incidencia");
	}

	@Test
	void decidirDosVecesOEnElEstadoEquivocadoResponde409() throws Exception {
		String id = registrar(ana);
		post(director, "/director/incidencias/" + id + "/atender", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("La incidencia ya cambió de estado."));
		post(director, "/director/incidencias/" + id + "/resolver",
				json("solucion", "Se cambió el proyector del aula."))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Solo se pueden resolver incidencias aprobadas."));

		decidir(id, "aprobar", null);
		post(director, "/director/incidencias/" + id + "/aprobar", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("La incidencia ya cambió de estado."));
	}

	@Test
	void soloElDirectorDecide() throws Exception {
		String id = registrar(ana);

		for (var otro : new com.hellofriend.usuarios.dominio.Usuario[]{ana, profe, admin}) {
			post(otro, "/director/incidencias/" + id + "/aprobar", null).andExpect(status().isForbidden());
		}
		post(null, "/director/incidencias/" + id + "/aprobar", null).andExpect(status().isUnauthorized());
		post(director, "/director/incidencias/no-existe/aprobar", null).andExpect(status().isNotFound());
		post(director, "/director/incidencias/" + id + "/inventar", null).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Acción desconocida."));
	}

	@Test
	void observarExigeUnaObservacionDeDiezAQuinientosCaracteres() throws Exception {
		String id = registrar(ana);

		post(director, "/director/incidencias/" + id + "/observar", json("observacion", "corta"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La observación debe tener entre 10 y 500 caracteres."));
		post(director, "/director/incidencias/" + id + "/observar", json("observacion", "x".repeat(501)))
				.andExpect(status().isBadRequest());
		post(director, "/director/incidencias/" + id + "/observar", null).andExpect(status().isBadRequest());
	}

	@Test
	void observarAvisaSoloAlAutorYSeVeSuObservacion() throws Exception {
		String id = registrar(ana);
		suscribir(luis);

		post(director, "/director/incidencias/" + id + "/observar",
				json("observacion", "Indica en qué horario ocurre."))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("OBSERVADA"));

		get(ana, "/notificaciones").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].tipo").value("OBSERVADA"));
		get(luis, "/notificaciones").andExpect(jsonPath("$", hasSize(0)));
		get(ana, "/incidencias/" + id).andExpect(jsonPath("$.observacion").value("Indica en qué horario ocurre."))
				.andExpect(jsonPath("$.observaciones", hasSize(1))).andExpect(jsonPath("$.puedeEditar").value(true))
				.andExpect(jsonPath("$.puedeReenviar").value(false));
	}

	@Test
	void reenviarExigeEditarAntesYAvisaALosDirectores() throws Exception {
		String id = registrar(ana);
		decidir(id, "observar", json("observacion", "Indica en qué horario ocurre."));

		post(ana, "/incidencias/" + id + "/reenviar", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Edita la incidencia antes de reenviarla."));
		put(ana, "/incidencias/" + id, json("categoria", categoria, "titulo", TITULO, "descripcion",
				DESCRIPCION + " Ocurre por las tardes.")).andExpect(status().isOk())
				.andExpect(jsonPath("$.puedeReenviar").value(true));
		post(ana, "/incidencias/" + id + "/reenviar", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));

		get(director, "/notificaciones").andExpect(jsonPath("$[?(@.tipo == 'REENVIADA')]", hasSize(1)))
				.andExpect(
						jsonPath("$[?(@.tipo == 'REENVIADA')].incidenciaId").value(org.hamcrest.Matchers.contains(id)));
	}

	@Test
	void reenviarExigeSerElAutorYEstarObservada() throws Exception {
		String id = registrar(ana);

		post(luis, "/incidencias/" + id + "/reenviar", null).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Solo el autor puede reenviar su incidencia."));
		post(ana, "/incidencias/" + id + "/reenviar", null).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Solo se puede reenviar una incidencia observada."));
		post(profe, "/incidencias/" + id + "/reenviar", null).andExpect(status().isForbidden());
	}

	@Test
	void rechazarExigeUnMotivoOUnComentario() throws Exception {
		String id = registrar(ana);

		post(director, "/director/incidencias/" + id + "/rechazar", null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Elige un motivo o escribe un comentario."));
		post(director, "/director/incidencias/" + id + "/rechazar", json("motivoId", "no-existe", "comentario", "  "))
				.andExpect(status().isBadRequest());
		post(director, "/director/incidencias/" + id + "/rechazar", json("comentario", "x".repeat(501)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El comentario no puede superar los 500 caracteres."));
	}

	@Test
	void rechazarConMotivoYComentarioLoMuestraAlAutorYAvisaSoloAEl() throws Exception {
		String id = registrar(ana);
		suscribir(luis);
		String motivo = leer(get(director, "/motivos-rechazo"), "$[0].id");
		String descripcionMotivo = leer(get(director, "/motivos-rechazo"), "$[0].descripcion");

		post(director, "/director/incidencias/" + id + "/rechazar",
				json("motivoId", motivo, "comentario", "Revisa el curso."))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));

		get(ana, "/incidencias/" + id).andExpect(jsonPath("$.revision.decision").value("RECHAZADA"))
				.andExpect(jsonPath("$.revision.motivo").value(descripcionMotivo))
				.andExpect(jsonPath("$.revision.comentario").value("Revisa el curso."));
		get(ana, "/notificaciones").andExpect(jsonPath("$[0].tipo").value("RECHAZADA"));
		get(luis, "/notificaciones").andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void rechazarSoloConComentarioTambienSirve() throws Exception {
		String id = registrar(ana);

		post(director, "/director/incidencias/" + id + "/rechazar", json("comentario", "No corresponde al curso."))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
		get(ana, "/incidencias/" + id).andExpect(jsonPath("$.revision.comentario").value("No corresponde al curso."));
	}

	@Test
	void atenderYResolverRecorrenElFlujoYAvisanAlAutorYALosSuscritos() throws Exception {
		String id = publicada(ana);
		suscribir(luis);

		post(director, "/director/incidencias/" + id + "/atender", null).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"));
		post(director, "/director/incidencias/" + id + "/resolver", json("solucion", "corta"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La solución debe tener entre 10 y 1000 caracteres."));
		post(director, "/director/incidencias/" + id + "/resolver",
				json("solucion", "Se cambió el proyector del aula."))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RESUELTA"))
				.andExpect(jsonPath("$.solucion").value("Se cambió el proyector del aula."))
				.andExpect(jsonPath("$.fechaResolucion").isNotEmpty());

		get(luis, "/notificaciones").andExpect(jsonPath("$[?(@.tipo == 'EN_ATENCION')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.tipo == 'RESUELTA')]", hasSize(1)));
	}

	@Test
	void sePuedeResolverDirectamenteDesdePublicada() throws Exception {
		String id = publicada(ana);

		post(director, "/director/incidencias/" + id + "/resolver",
				json("solucion", "Se cambió el proyector del aula."))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RESUELTA"));
	}

	@Test
	void cadaDecisionDejaHistorialYAuditoria() throws Exception {
		String id = registrar(ana);
		decidir(id, "aprobar", null);
		decidir(id, "atender", null);

		assertThat(contar("SELECT COUNT(*) FROM incidencias_historial_estado WHERE incidencia_id=?", id)).isEqualTo(3);
		assertThat(contar("SELECT COUNT(*) FROM auditoria_evento WHERE incidencia_id=? AND actor_id=? "
				+ "AND accion IN ('INCIDENCIA_APROBADA','INCIDENCIA_EN_ATENCION')", id, director.getId())).isEqualTo(2);
	}

	// --- incidencia nueva: aviso a los directores

	@Test
	void registrarUnaIncidenciaAvisaALosDirectoresActivos() throws Exception {
		var otroDirector = usuario("direct2", Rol.DIRECTOR);
		var inactivo = usuario("direct3", Rol.DIRECTOR);
		inactivo.desactivar();
		usuarios.saveAndFlush(inactivo);

		var autora = usuario("zulema", Rol.ESTUDIANTE);
		String id = registrar(autora);

		for (var d : new com.hellofriend.usuarios.dominio.Usuario[]{director, otroDirector}) {
			get(d, "/notificaciones").andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].tipo").value("NUEVA"))
					.andExpect(jsonPath("$[0].incidenciaId").value(id))
					.andExpect(jsonPath("$[0].mensaje").value(containsString("INF263 · Algoritmia")));
		}
		assertThat(contar("SELECT COUNT(*) FROM notificaciones_notificacion WHERE destinatario_id=?", inactivo.getId()))
				.isZero();
		assertThat(contar("SELECT COUNT(*) FROM notificaciones_notificacion WHERE destinatario_id=?", autora.getId()))
				.isZero();
		String texto = leer(get(director, "/notificaciones"), "$[0].titulo") + " "
				+ leer(get(director, "/notificaciones"), "$[0].mensaje");
		assertThat(texto).doesNotContainIgnoringCase("zulema");
	}

}

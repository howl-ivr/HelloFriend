package com.hellofriend.incidencias;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hellofriend.PruebaIntegracion;

/**
 * V2__modelo_incidencias: las tablas existen y la base rechaza los datos que rompen las reglas de canales,
 * incidencias, versiones y confirmaciones.
 */
class EsquemaIncidenciasIT extends PruebaIntegracion {

	private static final String CATEGORIA_OTROS = "00000005-0000-0000-0000-000000000008";

	private String cursoId;
	private String canalId;
	private String estudianteId;

	@BeforeEach
	void datosBase() {
		estudianteId = usuario("ana");
		cursoId = "00000002-0000-0000-0000-0000000000a1";
		jdbc.sql("INSERT INTO academico_curso (id, codigo, nombre) VALUES (?, '1INF99', 'Curso de prueba')")
				.param(cursoId).update();
		canalId = canal(2026, 2);
	}

	private String usuario(String nombre) {
		jdbc.sql(
				"INSERT INTO usuarios_usuario (correo, usuario, contrasena_hash, rol) VALUES (?, ?, 'hash', 'ESTUDIANTE')")
				.params(nombre + "@pucp.edu.pe", nombre).update();
		return jdbc.sql("SELECT id FROM usuarios_usuario WHERE usuario = ?").param(nombre).query(String.class)
				.single();
	}

	private String canal(int anio, int semestre) {
		String id = java.util.UUID.randomUUID().toString();
		jdbc.sql("INSERT INTO canales_canal (id, curso_id, anio, semestre, creado_por) VALUES (?, ?, ?, ?, ?)")
				.params(id, cursoId, anio, semestre, estudianteId).update();
		return id;
	}

	private String incidencia(String codigo, String estado, String motivoRechazo, String solucion) {
		String id = java.util.UUID.randomUUID().toString();
		boolean aprobada = estado.equals("APROBADA") || estado.equals("EN_ATENCION") || estado.equals("RESUELTA");
		jdbc.sql("""
				INSERT INTO incidencias_incidencia (id, codigo_seguimiento, canal_id, autor_id, categoria_id, titulo,
				    descripcion, estado, motivo_rechazo, solucion, aprobada_en, resuelta_en)
				VALUES (?, ?, ?, ?, ?, 'Titulo', 'Descripcion', ?, ?, ?,
				    CASE WHEN ? THEN UTC_TIMESTAMP(6) END, CASE WHEN ? THEN UTC_TIMESTAMP(6) END)
				""")
				.params(id, codigo, canalId, estudianteId, CATEGORIA_OTROS, estado, motivoRechazo, solucion, aprobada,
						estado.equals("RESUELTA"))
				.update();
		return id;
	}

	@Test
	void categoriasDeReferenciaCargadas() {
		assertThat(jdbc.sql("SELECT COUNT(*) FROM incidencias_categoria").query(Integer.class).single())
				.isEqualTo(8);
	}

	@Test
	void incidenciaPendienteSeGuardaConValoresPorDefecto() {
		String id = incidencia("INC-00000001", "PENDIENTE", null, null);

		var totalConfirmaciones = jdbc.sql("SELECT total_confirmaciones FROM incidencias_incidencia WHERE id = ?")
				.param(id).query(Integer.class).single();
		assertThat(totalConfirmaciones).isZero();
	}

	@Test
	void incidenciaRechazadaExigeMotivo() {
		rechazaPor("ck_incidencias_incidencia_motivo_rechazo",
				() -> incidencia("INC-00000002", "RECHAZADA", null, null));
	}

	@Test
	void incidenciaResueltaExigeSolucion() {
		rechazaPor("ck_incidencias_incidencia_solucion", () -> incidencia("INC-00000003", "RESUELTA", null, null));
	}

	@Test
	void incidenciaRechazadaYResueltaConSusDatosSeAceptan() {
		incidencia("INC-00000004", "RECHAZADA", "Falta detalle", null);
		incidencia("INC-00000005", "RESUELTA", null, "Se corrigio");
		assertThat(jdbc.sql("SELECT COUNT(*) FROM incidencias_incidencia").query(Integer.class).single())
				.isEqualTo(2);
	}

	@Test
	void canalNoSePuedeRepetirEnElMismoCursoYPeriodo() {
		rechazaPor("uq_canales_canal_curso_anio_semestre", () -> canal(2026, 2));
	}

	@Test
	void mismoCursoEnOtroPeriodoSeAcepta() {
		canal(2026, 1);
		canal(2025, 2);
		assertThat(jdbc.sql("SELECT COUNT(*) FROM canales_canal WHERE curso_id = ?").param(cursoId)
				.query(Integer.class).single()).isEqualTo(3);
	}

	@Test
	void suscripcionNoSePuedeRepetir() {
		jdbc.sql("INSERT INTO canales_suscripcion (canal_id, usuario_id) VALUES (?, ?)").params(canalId, estudianteId)
				.update();
		rechazaPor("uq_canales_suscripcion_canal_usuario",
				() -> jdbc.sql("INSERT INTO canales_suscripcion (canal_id, usuario_id) VALUES (?, ?)")
						.params(canalId, estudianteId).update());
	}

	@Test
	void confirmacionNoSePuedeRepetir() {
		String incidenciaId = incidencia("INC-00000006", "APROBADA", null, null);
		String otro = usuario("beto");
		jdbc.sql("INSERT INTO incidencias_confirmacion (incidencia_id, estudiante_id) VALUES (?, ?)")
				.params(incidenciaId, otro).update();
		rechazaPor("uq_incidencias_confirmacion_incidencia_estudiante",
				() -> jdbc.sql("INSERT INTO incidencias_confirmacion (incidencia_id, estudiante_id) VALUES (?, ?)")
						.params(incidenciaId, otro).update());
	}

	@Test
	void numeroDeVersionDebeSerPositivo() {
		String incidenciaId = incidencia("INC-00000007", "PENDIENTE", null, null);
		rechazaPor("ck_incidencias_version_numero", () -> jdbc.sql("""
				INSERT INTO incidencias_version (incidencia_id, numero, titulo, descripcion, categoria_id)
				VALUES (?, 0, 'Titulo', 'Descripcion', ?)
				""").params(incidenciaId, CATEGORIA_OTROS).update());
	}

}

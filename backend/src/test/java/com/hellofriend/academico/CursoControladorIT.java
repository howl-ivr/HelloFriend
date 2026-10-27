package com.hellofriend.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** Endpoints de /admin/cursos: crear, listar, buscar, modificar y eliminar. */
class CursoControladorIT extends PruebaIntegracion {

	@Autowired
	private MockMvc mvc;

	private ResultActions crear(String json) throws Exception {
		return mvc.perform(post("/admin/cursos").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String crearCurso(String codigo, String nombre) throws Exception {
		String cuerpo = crear("{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre + "\"}")
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.id");
	}

	private ResultActions modificar(String id, String json) throws Exception {
		return mvc.perform(put("/admin/cursos/" + id).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	// --- crear

	@Test
	void creaConIdentificadorYFechaGenerados() throws Exception {
		String cuerpo = crear("{\"codigo\":\"inf263\",\"nombre\":\"  Algoritmia \"}").andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("INF263")).andExpect(jsonPath("$.nombre").value("Algoritmia"))
				.andExpect(jsonPath("$.creadoEn").isNotEmpty()).andReturn().getResponse().getContentAsString();

		String id = JsonPath.read(cuerpo, "$.id");
		assertThat(id).hasSize(36);
		assertThat(jdbc.sql("SELECT COUNT(*) FROM academico_curso WHERE id = ?").param(id).query(Integer.class)
				.single()).isEqualTo(1);
	}

	@Test
	void ignoraElIdentificadorQueEnvieElCliente() throws Exception {
		String cuerpo = crear("{\"id\":\"mio\",\"codigo\":\"INF263\",\"nombre\":\"Algoritmia\"}")
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		assertThat((String) JsonPath.read(cuerpo, "$.id")).isNotEqualTo("mio");
	}

	@Test
	void rechazaCodigoRepetidoSinImportarMayusculas() throws Exception {
		crearCurso("INF263", "Algoritmia");

		crear("{\"codigo\":\"inf263\",\"nombre\":\"Otro\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ya existe un curso con el código INF263."));
	}

	@Test
	void rechazaDatosInvalidosConSuMensaje() throws Exception {
		crear("{\"codigo\":\"IN\",\"nombre\":\"Algoritmia\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El código debe tener de 3 a 10 letras o números."));
		crear("{\"codigo\":\"INF263\",\"nombre\":\" \"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre es obligatorio."));
		crear("no es json").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La solicitud está mal formada."));
	}

	// --- listar y buscar

	@Test
	void listaTodosOrdenadosPorCodigo() throws Exception {
		crearCurso("INF263", "Algoritmia");
		crearCurso("1INF01", "Fundamentos");

		mvc.perform(get("/admin/cursos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].codigo").value("1INF01")).andExpect(jsonPath("$[1].codigo").value("INF263"));
	}

	@Test
	void buscaPorIdYDa404SiNoExiste() throws Exception {
		String id = crearCurso("INF263", "Algoritmia");

		mvc.perform(get("/admin/cursos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.codigo").value("INF263"));
		mvc.perform(get("/admin/cursos/no-existe")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("El curso no existe."));
	}

	@Test
	void buscaPorParteDelNombreSinImportarMayusculasNiTildes() throws Exception {
		crearCurso("1INF36", "Ética y RSU");
		crearCurso("INF263", "Algoritmia");

		mvc.perform(get("/admin/cursos").param("nombre", "etica")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].codigo").value("1INF36"));
		mvc.perform(get("/admin/cursos").param("nombre", "ALGO")).andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(get("/admin/cursos").param("nombre", "zzz")).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(get("/admin/cursos").param("nombre", " ")).andExpect(jsonPath("$", hasSize(2)));
	}

	// --- modificar

	@Test
	void modificaCodigoYNombrePeroNoElIdNiLaFechaDeCreacion() throws Exception {
		String cuerpo = crear("{\"codigo\":\"INF263\",\"nombre\":\"Algoritmia\"}").andReturn().getResponse()
				.getContentAsString();
		String id = JsonPath.read(cuerpo, "$.id");
		String creadoEn = JsonPath.read(cuerpo, "$.creadoEn");

		modificar(id, "{\"id\":\"otro\",\"codigo\":\"inf300\",\"nombre\":\"Algoritmia avanzada\"}")
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.codigo").value("INF300"))
				.andExpect(jsonPath("$.nombre").value("Algoritmia avanzada"))
				.andExpect(jsonPath("$.creadoEn").value(creadoEn));
		assertThat(jdbc.sql("SELECT codigo FROM academico_curso WHERE id = ?").param(id).query(String.class).single())
				.isEqualTo("INF300");
	}

	@Test
	void permiteConservarElMismoCodigoAlModificar() throws Exception {
		String id = crearCurso("INF263", "Algoritmia");

		modificar(id, "{\"codigo\":\"INF263\",\"nombre\":\"Nuevo nombre\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
	}

	@Test
	void noPermiteUsarElCodigoDeOtroCurso() throws Exception {
		crearCurso("INF263", "Algoritmia");
		String id = crearCurso("INF300", "Otro");

		modificar(id, "{\"codigo\":\"INF263\",\"nombre\":\"Otro\"}").andExpect(status().isConflict());
	}

	@Test
	void modificarUnCursoInexistenteDa404YDatosInvalidosDa400() throws Exception {
		modificar("no-existe", "{\"codigo\":\"INF263\",\"nombre\":\"Algoritmia\"}").andExpect(status().isNotFound());
		String id = crearCurso("INF263", "Algoritmia");
		modificar(id, "{\"codigo\":\"\",\"nombre\":\"Algoritmia\"}").andExpect(status().isBadRequest());
	}

	// --- eliminar

	@Test
	void eliminaUnCurso() throws Exception {
		String id = crearCurso("INF263", "Algoritmia");

		mvc.perform(delete("/admin/cursos/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/admin/cursos/" + id)).andExpect(status().isNotFound());
		mvc.perform(delete("/admin/cursos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void noEliminaUnCursoQueTieneCanales() throws Exception {
		String id = crearCurso("INF263", "Algoritmia");
		jdbc.sql(
				"INSERT INTO usuarios_usuario (correo, usuario, contrasena_hash, rol) VALUES (?, ?, 'hash', 'ADMINISTRADOR')")
				.params("admin@pucp.edu.pe", "admin").update();
		String adminId = jdbc.sql("SELECT id FROM usuarios_usuario WHERE usuario = 'admin'").query(String.class)
				.single();
		jdbc.sql("INSERT INTO canales_canal (curso_id, anio, semestre, creado_por) VALUES (?, 2026, 2, ?)")
				.params(id, adminId).update();

		mvc.perform(delete("/admin/cursos/" + id)).andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.mensaje").value("No se puede eliminar el curso porque tiene canales asociados."));
	}

}

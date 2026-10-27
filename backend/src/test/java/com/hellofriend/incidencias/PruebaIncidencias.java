package com.hellofriend.incidencias;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.hellofriend.PruebaApi;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/**
 * Escenario común de las pruebas de incidencias: un administrador, un director, un profesor, tres estudiantes, un
 * curso y un canal activo del periodo 2026-2.
 */
public abstract class PruebaIncidencias extends PruebaApi {

	protected static final String TITULO = "Aula 301 sin proyector";
	protected static final String DESCRIPCION = "El proyector del aula 301 no funciona desde hace dos semanas.";

	protected Usuario admin;
	protected Usuario director;
	protected Usuario profe;
	protected Usuario ana;
	protected Usuario luis;
	protected Usuario marta;
	protected String curso;
	protected String canal;
	protected String categoria;

	@BeforeEach
	void escenario() throws Exception {
		admin = usuario("admin", Rol.ADMINISTRADOR);
		director = usuario("direct", Rol.DIRECTOR);
		profe = usuario("profe", Rol.PROFESOR);
		ana = usuario("ana", Rol.ESTUDIANTE);
		luis = usuario("luis", Rol.ESTUDIANTE);
		marta = usuario("marta", Rol.ESTUDIANTE);
		curso = leer(post(admin, "/admin/cursos", json("codigo", "INF263", "nombre", "Algoritmia"))
				.andExpect(status().isCreated()), "$.id");
		canal = leer(post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[\"" + curso + "\"]}")
				.andExpect(status().isCreated()), "$[0].id");
		List<String> categorias = leer(get(ana, "/categorias").andExpect(status().isOk()), "$");
		categoria = categorias.getFirst();
	}

	/** Contenido válido de una incidencia con el título dado. */
	protected String contenido(String titulo) {
		return json("canalId", canal, "categoria", categoria, "titulo", titulo, "descripcion", DESCRIPCION);
	}

	/** El autor registra una incidencia; devuelve su id. Queda PENDIENTE y el autor se suscribe al canal. */
	protected String registrar(Usuario autor) throws Exception {
		return registrar(autor, TITULO);
	}

	protected String registrar(Usuario autor, String titulo) throws Exception {
		return leer(post(autor, "/incidencias", contenido(titulo)).andExpect(status().isCreated()), "$.id");
	}

	/** El director toma una decisión que debe resultar bien. */
	protected void decidir(String id, String accion, String cuerpo) throws Exception {
		post(director, "/director/incidencias/" + id + "/" + accion, cuerpo).andExpect(status().isOk());
	}

	/** Registrada y aprobada por el director: queda PUBLICADA. */
	protected String publicada(Usuario autor) throws Exception {
		return publicada(autor, TITULO);
	}

	protected String publicada(Usuario autor, String titulo) throws Exception {
		String id = registrar(autor, titulo);
		decidir(id, "aprobar", null);
		return id;
	}

	protected void suscribir(Usuario quien) throws Exception {
		post(quien, "/canales/" + canal + "/suscripcion", null).andExpect(status().isOk());
	}

}

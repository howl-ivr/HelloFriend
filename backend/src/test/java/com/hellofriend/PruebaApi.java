package com.hellofriend;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;

/**
 * Base de las pruebas de la API con tokens reales: crea usuarios con un rol y hace peticiones como ellos. Con
 * {@code null} como usuario la petición va sin token.
 */
public abstract class PruebaApi extends PruebaIntegracion {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected UsuarioRepositorio usuarios;

	@Autowired
	protected ServicioJwt jwt;

	/** Crea un usuario activo con ese rol. El nombre de usuario tiene como máximo 15 caracteres. */
	protected Usuario usuario(String nombre, Rol rol) {
		return usuarios.saveAndFlush(new Usuario(nombre + "@pucp.edu.pe", nombre, "hash", rol, LocalDateTime.now()));
	}

	/** GET con parámetros de consulta en pares clave, valor. */
	protected ResultActions get(Usuario quien, String url, String... parametros) throws Exception {
		MockHttpServletRequestBuilder peticion = MockMvcRequestBuilders.get(url);
		for (int i = 0; i < parametros.length; i += 2) {
			peticion.param(parametros[i], parametros[i + 1]);
		}
		return mvc.perform(conToken(quien, peticion));
	}

	protected ResultActions post(Usuario quien, String url, String json) throws Exception {
		return mvc.perform(conToken(quien, conCuerpo(MockMvcRequestBuilders.post(url), json)));
	}

	protected ResultActions put(Usuario quien, String url, String json) throws Exception {
		return mvc.perform(conToken(quien, conCuerpo(MockMvcRequestBuilders.put(url), json)));
	}

	protected ResultActions patch(Usuario quien, String url) throws Exception {
		return mvc.perform(conToken(quien, MockMvcRequestBuilders.patch(url)));
	}

	protected ResultActions delete(Usuario quien, String url) throws Exception {
		return mvc.perform(conToken(quien, MockMvcRequestBuilders.delete(url)));
	}

	/** JSON de un objeto a partir de pares clave, valor de texto. Un valor {@code null} sale como null. */
	protected static String json(String... pares) {
		StringBuilder salida = new StringBuilder("{");
		for (int i = 0; i < pares.length; i += 2) {
			if (i > 0) {
				salida.append(',');
			}
			salida.append('"').append(pares[i]).append("\":");
			salida.append(pares[i + 1] == null
					? "null"
					: "\"" + pares[i + 1].replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
		}
		return salida.append('}').toString();
	}

	/** Lee un valor de la respuesta con una ruta JSONPath. */
	protected static <T> T leer(ResultActions respuesta, String ruta) throws Exception {
		return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
	}

	protected long contar(String sql, Object... parametros) {
		return jdbc.sql(sql).params(parametros).query(Long.class).single();
	}

	private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, String json) {
		return json == null ? peticion : peticion.contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MockHttpServletRequestBuilder conToken(Usuario quien, MockHttpServletRequestBuilder peticion) {
		if (quien != null) {
			peticion.header("Authorization",
					"Bearer " + jwt.emitir(quien.getId(), quien.getUsuario(), quien.getRol().name(),
							quien.getVersionSesion()));
		}
		return peticion;
	}

}

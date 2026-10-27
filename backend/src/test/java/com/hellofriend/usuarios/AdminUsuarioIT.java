package com.hellofriend.usuarios;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** GET /admin/usuarios: solo el administrador, con correo, y con los filtros buscar y rol. */
class AdminUsuarioIT extends PruebaIntegracion {

	private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 1, 9, 0);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private ServicioJwt jwt;

	private Usuario admin;
	private Usuario ana;
	private Usuario marta;

	@BeforeEach
	void crearUsuarios() {
		String hash = codificador.encode("Clave#2026");
		admin = repositorio.save(new Usuario("admin@pucp.edu.pe", "admin", hash, Rol.ADMINISTRADOR, INICIO));
		ana = repositorio.save(new Usuario("ana@pucp.edu.pe", "ana", hash, Rol.ESTUDIANTE, INICIO.plusMinutes(1)));
		marta = repositorio.save(new Usuario("marta@pucp.edu.pe", "marta", hash, Rol.PROFESOR, INICIO.plusMinutes(2)));
		repositorio.saveAndFlush(new Usuario("luis@pucp.edu.pe", "luis", hash, Rol.ESTUDIANTE, INICIO.plusMinutes(3)));
	}

	private ResultActions como(Usuario u, MockHttpServletRequestBuilder peticion) throws Exception {
		String token = jwt.emitir(u.getId(), u.getUsuario(), u.getRol().name(), u.getVersionSesion());
		return mvc.perform(peticion.header("Authorization", "Bearer " + token));
	}

	private ResultActions listar(String buscar, String rol) throws Exception {
		MockHttpServletRequestBuilder peticion = get("/admin/usuarios");
		if (buscar != null) {
			peticion.param("buscar", buscar);
		}
		if (rol != null) {
			peticion.param("rol", rol);
		}
		return como(admin, peticion);
	}

	// --- quién puede entrar

	@Test
	void sinTokenResponde401() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void conOtroRolResponde403() throws Exception {
		como(ana, get("/admin/usuarios")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No tienes permiso para realizar esta acción."));
		como(marta, get("/admin/usuarios")).andExpect(status().isForbidden());
	}

	// --- contenido

	@Test
	void listaATodosLosUsuariosConCorreoLosMasAntiguosPrimero() throws Exception {
		listar(null, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].usuario").value("admin")).andExpect(jsonPath("$[1].usuario").value("ana"))
				.andExpect(jsonPath("$[3].usuario").value("luis")).andExpect(jsonPath("$[1].id").value(ana.getId()))
				.andExpect(jsonPath("$[1].correo").value("ana@pucp.edu.pe"))
				.andExpect(jsonPath("$[1].rol").value("ESTUDIANTE")).andExpect(jsonPath("$[1].estado").value("ACTIVA"))
				.andExpect(jsonPath("$[1].fechaRegistro").isNotEmpty())
				.andExpect(jsonPath("$[1].contrasenaHash").doesNotExist());
	}

	@Test
	void unaCuentaDesactivadaSeVeComoDesactivada() throws Exception {
		ana.desactivar();
		repositorio.saveAndFlush(ana);

		listar("ana", null).andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("DESACTIVADA"));
	}

	// --- filtros

	@Test
	void filtraPorRol() throws Exception {
		listar(null, "ESTUDIANTE").andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].usuario").value("ana"))
				.andExpect(jsonPath("$[1].usuario").value("luis"));
		listar(null, "PROFESOR").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].usuario").value("marta"));
	}

	@Test
	void buscaPorParteDelUsuarioSinImportarMayusculas() throws Exception {
		listar("MAR", null).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].usuario").value("marta"));
	}

	@Test
	void buscaPorParteDelCorreo() throws Exception {
		listar("luis@pucp", null).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].usuario").value("luis"));
	}

	@Test
	void buscarYRolSeCombinan() throws Exception {
		listar("a", null).andExpect(jsonPath("$", hasSize(3)));
		listar("a", "PROFESOR").andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].usuario").value("marta"));
	}

	@Test
	void sinCoincidenciasDevuelveLista() throws Exception {
		listar("nadie", null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unaBusquedaEnBlancoNoFiltra() throws Exception {
		listar("   ", null).andExpect(jsonPath("$", hasSize(4)));
	}

	@Test
	void losComodinesDeLikeSeBuscanComoTexto() throws Exception {
		listar("%", null).andExpect(jsonPath("$", hasSize(0)));
		listar("_", null).andExpect(jsonPath("$", hasSize(0)));
		listar("an!", null).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unRolDesconocidoResponde400() throws Exception {
		listar(null, "JEFE").andExpect(status().isBadRequest());
	}

}

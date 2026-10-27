package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.compartido.seguridad.ServicioJwt;
import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** /admin/usuarios: solo el administrador revisa, aprueba o rechaza solicitudes de registro. */
class AdminUsuarioIT extends PruebaIntegracion {

	private static final String CLAVE = "Clave#2026";
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
	private Usuario pedro;
	private Usuario luis;

	@BeforeEach
	void crearUsuarios() {
		String hash = codificador.encode(CLAVE);
		admin = repositorio.save(new Usuario("admin@pucp.edu.pe", "admin", hash, Rol.ADMINISTRADOR, INICIO));
		ana = repositorio.save(new Usuario("ana@pucp.edu.pe", "ana", hash, Rol.ESTUDIANTE, INICIO));
		luis = repositorio.save(Usuario.solicitud("luis@pucp.edu.pe", "luis", hash, INICIO.plusMinutes(5)));
		pedro = repositorio.saveAndFlush(Usuario.solicitud("pedro@pucp.edu.pe", "pedro", hash, INICIO.plusMinutes(1)));
	}

	private String tokenDe(Usuario u) {
		return jwt.emitir(u.getId(), u.getUsuario(), u.getRol().name(), u.getVersionSesion());
	}

	private ResultActions como(Usuario u, MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header("Authorization", "Bearer " + tokenDe(u)));
	}

	// --- quién puede entrar

	@Test
	void sinTokenResponde401() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void unEstudianteRecibe403EnTodosLosEndpoints() throws Exception {
		como(ana, get("/admin/usuarios")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No tienes permiso para realizar esta acción."));
		como(ana, post("/admin/usuarios/" + pedro.getId() + "/aprobar")).andExpect(status().isForbidden());
		como(ana, post("/admin/usuarios/" + pedro.getId() + "/rechazar")).andExpect(status().isForbidden());

		repositorio.flush();
		assertThat(repositorio.findById(pedro.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
	}

	// --- listar

	@Test
	void listaLasPendientesDeLasMasAntiguasALasMasNuevasSinCorreo() throws Exception {
		como(admin, get("/admin/usuarios").param("estado", "PENDIENTE")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].usuario").value("pedro"))
				.andExpect(jsonPath("$[1].usuario").value("luis")).andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
				.andExpect(jsonPath("$[0].rol").value("ESTUDIANTE")).andExpect(jsonPath("$[0].id").value(pedro.getId()))
				.andExpect(jsonPath("$[0].fechaRegistro").isNotEmpty())
				.andExpect(jsonPath("$[0].correo").doesNotExist())
				.andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());
	}

	@Test
	void sinFiltroListaTodosLosEstudiantesPeroNoALosOtrosRoles() throws Exception {
		como(admin, get("/admin/usuarios")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].usuario").value("ana"));
	}

	@Test
	void unEstadoDesconocidoResponde400() throws Exception {
		como(admin, get("/admin/usuarios").param("estado", "NOEXISTE")).andExpect(status().isBadRequest());
	}

	// --- aprobar y rechazar

	@Test
	void aprobarPasaLaCuentaAActivaYYaPuedeEntrar() throws Exception {
		como(admin, post("/admin/usuarios/" + pedro.getId() + "/aprobar")).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario").value("pedro")).andExpect(jsonPath("$.estado").value("ACTIVA"));

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"pedro\",\"password\":\"" + CLAVE + "\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void rechazarPasaLaCuentaARechazadaYNoPuedeEntrar() throws Exception {
		como(admin, post("/admin/usuarios/" + luis.getId() + "/rechazar")).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("RECHAZADA"));

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"luis\",\"password\":\"" + CLAVE + "\"}")).andExpect(status().isForbidden());
	}

	@Test
	void soloSePuedeResolverUnaCuentaPendiente() throws Exception {
		como(admin, post("/admin/usuarios/" + ana.getId() + "/aprobar")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Solo se puede aprobar una cuenta pendiente."));
		como(admin, post("/admin/usuarios/" + ana.getId() + "/rechazar")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Solo se puede rechazar una cuenta pendiente."));

		como(admin, post("/admin/usuarios/" + pedro.getId() + "/aprobar")).andExpect(status().isOk());
		como(admin, post("/admin/usuarios/" + pedro.getId() + "/rechazar")).andExpect(status().isConflict());
	}

	@Test
	void unUsuarioInexistenteResponde404() throws Exception {
		como(admin, post("/admin/usuarios/no-existe/aprobar")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
	}

}

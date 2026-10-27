package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/**
 * Cambiar el rol, desactivar y reactivar cuentas: solo el administrador, y cada cambio corta las sesiones de la
 * persona.
 */
class GestionUsuarioIT extends PruebaIntegracion {

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
	private Usuario marta;

	@BeforeEach
	void crearUsuarios() {
		String hash = codificador.encode(CLAVE);
		admin = repositorio.save(new Usuario("admin@pucp.edu.pe", "admin", hash, Rol.ADMINISTRADOR, INICIO));
		ana = repositorio.save(new Usuario("ana@pucp.edu.pe", "ana", hash, Rol.ESTUDIANTE, INICIO));
		marta = repositorio.saveAndFlush(new Usuario("marta@pucp.edu.pe", "marta", hash, Rol.PROFESOR, INICIO));
	}

	private String tokenDe(Usuario u) {
		return jwt.emitir(u.getId(), u.getUsuario(), u.getRol().name(), u.getVersionSesion());
	}

	private ResultActions como(Usuario u, MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header("Authorization", "Bearer " + tokenDe(u)));
	}

	private ResultActions cambiarRol(Usuario actor, Usuario objetivo, String rol) throws Exception {
		return como(actor, put("/admin/usuarios/" + objetivo.getId() + "/rol").contentType(MediaType.APPLICATION_JSON)
				.content("{\"rol\":" + (rol == null ? "null" : "\"" + rol + "\"") + "}"));
	}

	private ResultActions desactivar(Usuario actor, Usuario objetivo) throws Exception {
		return como(actor, post("/admin/usuarios/" + objetivo.getId() + "/desactivar"));
	}

	private ResultActions reactivar(Usuario actor, Usuario objetivo) throws Exception {
		return como(actor, post("/admin/usuarios/" + objetivo.getId() + "/reactivar"));
	}

	private ResultActions login(String usuario) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + CLAVE + "\"}"));
	}

	private ResultActions yo(String token) throws Exception {
		return mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token));
	}

	// --- quién puede entrar

	@Test
	void sinTokenResponde401() throws Exception {
		mvc.perform(put("/admin/usuarios/" + ana.getId() + "/rol").contentType(MediaType.APPLICATION_JSON)
				.content("{\"rol\":\"DIRECTOR\"}")).andExpect(status().isUnauthorized());
		mvc.perform(post("/admin/usuarios/" + ana.getId() + "/desactivar")).andExpect(status().isUnauthorized());
		mvc.perform(post("/admin/usuarios/" + ana.getId() + "/reactivar")).andExpect(status().isUnauthorized());
	}

	@Test
	void conOtroRolResponde403YNoCambiaNada() throws Exception {
		cambiarRol(marta, ana, "ADMINISTRADOR").andExpect(status().isForbidden());
		desactivar(marta, ana).andExpect(status().isForbidden());
		reactivar(marta, ana).andExpect(status().isForbidden());

		assertThat(ana.getRol()).isEqualTo(Rol.ESTUDIANTE);
		assertThat(ana.isActivo()).isTrue();
	}

	// --- cambiar rol

	@Test
	void cambiarElRolActualizaLaCuentaYCierraSusSesiones() throws Exception {
		String tokenViejo = tokenDe(ana);

		cambiarRol(admin, ana, "PROFESOR").andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value("ana"))
				.andExpect(jsonPath("$.rol").value("PROFESOR"))
				.andExpect(jsonPath("$.correo").value("ana@pucp.edu.pe"));

		yo(tokenViejo).andExpect(status().isUnauthorized());
		login("ana").andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("PROFESOR"));
	}

	@Test
	void unNuevoAdministradorYaPuedeAdministrar() throws Exception {
		cambiarRol(admin, ana, "ADMINISTRADOR").andExpect(status().isOk());

		como(ana, get("/admin/usuarios")).andExpect(status().isOk());
	}

	@Test
	void pasarAlMismoRolNoHaceNadaNiCierraSesiones() throws Exception {
		int version = ana.getVersionSesion();

		cambiarRol(admin, ana, "ESTUDIANTE").andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("ESTUDIANTE"));

		assertThat(repositorio.findById(ana.getId()).orElseThrow().getVersionSesion()).isEqualTo(version);
		yo(tokenDe(ana)).andExpect(status().isOk());
	}

	@Test
	void nadiePuedeCambiarSuPropioRolYResponde403() throws Exception {
		cambiarRol(admin, admin, "ESTUDIANTE").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No puedes cambiar tu propio rol."));

		assertThat(repositorio.findById(admin.getId()).orElseThrow().getRol()).isEqualTo(Rol.ADMINISTRADOR);
	}

	@Test
	void unRolInvalidoOAusenteResponde400() throws Exception {
		cambiarRol(admin, ana, "JEFE").andExpect(status().isBadRequest());
		cambiarRol(admin, ana, null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("Indica el rol."));
	}

	@Test
	void cambiarElRolDeUnUsuarioInexistenteResponde404() throws Exception {
		como(admin, put("/admin/usuarios/no-existe/rol").contentType(MediaType.APPLICATION_JSON)
				.content("{\"rol\":\"DIRECTOR\"}")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
	}

	// --- desactivar y reactivar

	@Test
	void desactivarCortaLaSesionBloqueaElLoginYSeVeEnLaLista() throws Exception {
		String tokenViejo = tokenDe(ana);

		desactivar(admin, ana).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DESACTIVADA"));

		yo(tokenViejo).andExpect(status().isUnauthorized());
		login("ana").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Tu cuenta está desactivada."));
		como(admin, get("/admin/usuarios").param("buscar", "ana")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].estado").value("DESACTIVADA"));
	}

	@Test
	void nadiePuedeDesactivarSuPropiaCuentaYResponde403() throws Exception {
		desactivar(admin, admin).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No puedes desactivar tu propia cuenta."));

		assertThat(repositorio.findById(admin.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void desactivarDosVecesResponde409() throws Exception {
		desactivar(admin, ana).andExpect(status().isOk());

		desactivar(admin, ana).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("La cuenta ya está desactivada."));
	}

	@Test
	void reactivarDevuelveElAccesoPeroNoRevivaLasSesionesAnteriores() throws Exception {
		String tokenViejo = tokenDe(ana);
		desactivar(admin, ana).andExpect(status().isOk());

		reactivar(admin, ana).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVA"));

		yo(tokenViejo).andExpect(status().isUnauthorized());
		login("ana").andExpect(status().isOk());
	}

	@Test
	void reactivarUnaCuentaActivaResponde409() throws Exception {
		reactivar(admin, ana).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("La cuenta ya está activa."));
	}

	@Test
	void unAdministradorPuedeDesactivarAOtro() throws Exception {
		Usuario otro = repositorio.saveAndFlush(
				new Usuario("otro@pucp.edu.pe", "otro", codificador.encode(CLAVE), Rol.ADMINISTRADOR, INICIO));

		desactivar(admin, otro).andExpect(status().isOk());

		como(otro, get("/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void desactivarOReactivarUnUsuarioInexistenteResponde404() throws Exception {
		como(admin, post("/admin/usuarios/no-existe/desactivar")).andExpect(status().isNotFound());
		como(admin, post("/admin/usuarios/no-existe/reactivar")).andExpect(status().isNotFound());
	}

	// --- ya no hay aprobación

	@Test
	void aprobarYRechazarYaNoExisten() throws Exception {
		como(admin, post("/admin/usuarios/" + ana.getId() + "/aprobar")).andExpect(status().isNotFound());
		como(admin, post("/admin/usuarios/" + ana.getId() + "/rechazar")).andExpect(status().isNotFound());
	}

}

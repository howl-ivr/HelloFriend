package com.hellofriend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.usuarios.dominio.EstadoCuenta;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/** POST /auth/registro: validaciones del contrato, cuenta PENDIENTE y bloqueo del login hasta la aprobación. */
class RegistroIT extends PruebaIntegracion {

	private static final String CLAVE = "Clave#2026";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepositorio repositorio;

	@Autowired
	private PasswordEncoder codificador;

	private ResultActions registrar(String correo, String usuario, String password) throws Exception {
		return mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(
				"{\"correo\":\"" + correo + "\",\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"));
	}

	private ResultActions login(String usuario, String password) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"));
	}

	@Test
	void registraUnaCuentaPendienteDeEstudiante() throws Exception {
		registrar("Ana@PUCP.edu.pe", "ana.p", CLAVE).andExpect(status().isCreated())
				.andExpect(jsonPath("$.mensaje").isNotEmpty());

		Usuario ana = repositorio.findByUsuario("ana.p").orElseThrow();
		assertThat(ana.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(ana.getRol()).isEqualTo(Rol.ESTUDIANTE);
		assertThat(ana.isActivo()).isTrue();
		assertThat(ana.getCorreo()).isEqualTo("ana@pucp.edu.pe");
		assertThat(ana.getContrasenaHash()).isNotEqualTo(CLAVE);
		assertThat(codificador.matches(CLAVE, ana.getContrasenaHash())).isTrue();
	}

	@Test
	void laRespuestaNoRevelaCorreoNiContrasena() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", CLAVE).andExpect(status().isCreated())
				.andExpect(jsonPath("$.correo").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void rechazaUnCorreoQueNoEsDeLaPucp() throws Exception {
		registrar("ana@gmail.com", "ana", CLAVE).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El correo debe terminar en @pucp.edu.pe."));
		registrar("@pucp.edu.pe", "ana", CLAVE).andExpect(status().isBadRequest());
		registrar("ana@pucp.edu.pe.com", "ana", CLAVE).andExpect(status().isBadRequest());
	}

	@Test
	void rechazaUsuariosConFormatoInvalido() throws Exception {
		String mensaje = "El usuario debe tener de 3 a 15 caracteres: letras, números, punto, guion o guion bajo.";
		registrar("ana@pucp.edu.pe", "ab", CLAVE).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(mensaje));
		registrar("ana@pucp.edu.pe", "abcdefghijklmnop", CLAVE).andExpect(status().isBadRequest());
		registrar("ana@pucp.edu.pe", "ana perez", CLAVE).andExpect(status().isBadRequest());
	}

	@Test
	void rechazaContrasenasCortas() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", "12345").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña debe tener de 6 a 64 caracteres."));
		registrar("ana@pucp.edu.pe", "ana", "123456").andExpect(status().isCreated());
	}

	@Test
	void rechazaContrasenasQueSuperanLosBytesDeBcrypt() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", "ñ".repeat(40)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contraseña es demasiado larga."));
	}

	@Test
	void usuarioRepetidoResponde409SinImportarMayusculas() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", CLAVE).andExpect(status().isCreated());

		registrar("otra@pucp.edu.pe", "ANA", CLAVE).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ese usuario ya está en uso."));
	}

	@Test
	void correoRepetidoResponde409() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", CLAVE).andExpect(status().isCreated());

		registrar("ANA@pucp.edu.pe", "otra", CLAVE).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ese correo ya está registrado."));
	}

	@Test
	void camposVaciosResponden400() throws Exception {
		registrar("", "ana", CLAVE).andExpect(status().isBadRequest());
		registrar("ana@pucp.edu.pe", "", CLAVE).andExpect(status().isBadRequest());
		registrar("ana@pucp.edu.pe", "ana", "").andExpect(status().isBadRequest());
	}

	@Test
	void unaCuentaPendienteNoPuedeEntrar() throws Exception {
		registrar("ana@pucp.edu.pe", "ana", CLAVE).andExpect(status().isCreated());

		login("ana", CLAVE).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Tu cuenta está pendiente de aprobación."));
		login("ana", "otra-clave").andExpect(status().isUnauthorized());
	}

	@Test
	void unaCuentaRechazadaNoPuedeEntrar() throws Exception {
		Usuario luis = Usuario.solicitud("luis@pucp.edu.pe", "luis", codificador.encode(CLAVE), LocalDateTime.now());
		luis.rechazar();
		repositorio.saveAndFlush(luis);

		login("luis", CLAVE).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Tu solicitud de registro fue rechazada."));
	}

}

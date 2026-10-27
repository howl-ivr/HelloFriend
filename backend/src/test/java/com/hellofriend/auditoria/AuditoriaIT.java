package com.hellofriend.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hellofriend.PruebaApi;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;

/** Las acciones de cuentas y cursos dejan su huella en la auditoría, con su actor. */
class AuditoriaIT extends PruebaApi {

	private Usuario admin;
	private Usuario ana;

	@BeforeEach
	void usuarios() {
		admin = usuario("admin", Rol.ADMINISTRADOR);
		ana = usuario("ana", Rol.ESTUDIANTE);
	}

	private long eventos(String accion, String detalle) {
		return contar("SELECT COUNT(*) FROM auditoria_evento WHERE accion=? AND detalle=?", accion, detalle);
	}

	@Test
	void registrarseDejaCuentaRegistradaConLaPersonaComoActor() throws Exception {
		post(null, "/auth/registro", json("correo", "nuevo@pucp.edu.pe", "usuario", "nuevo", "password", "Clave#2026"))
				.andExpect(status().isCreated());

		assertThat(contar("SELECT COUNT(*) FROM auditoria_evento e JOIN usuarios_usuario u ON u.id=e.actor_id "
				+ "WHERE e.accion='CUENTA_REGISTRADA' AND u.usuario='nuevo'")).isEqualTo(1);
		get(admin, "/admin/auditoria").andExpect(jsonPath("$[0].accion").value("CUENTA_REGISTRADA"))
				.andExpect(jsonPath("$[0].actor").value("nuevo"));
	}

	@Test
	void unRegistroRechazadoNoDejaHuella() throws Exception {
		post(null, "/auth/registro", json("correo", "malo@gmail.com", "usuario", "malo", "password", "Clave#2026"))
				.andExpect(status().isBadRequest());

		assertThat(contar("SELECT COUNT(*) FROM auditoria_evento WHERE accion='CUENTA_REGISTRADA'")).isZero();
	}

	@Test
	void cambiarRolDesactivarYReactivarDejanHuellaConElAdministradorComoActor() throws Exception {
		put(admin, "/admin/usuarios/" + ana.getId() + "/rol", json("rol", "PROFESOR")).andExpect(status().isOk());
		post(admin, "/admin/usuarios/" + ana.getId() + "/desactivar", null).andExpect(status().isOk());
		post(admin, "/admin/usuarios/" + ana.getId() + "/reactivar", null).andExpect(status().isOk());

		assertThat(eventos("ROL_ASIGNADO", ana.getId())).isEqualTo(1);
		assertThat(eventos("CUENTA_DESACTIVADA", ana.getId())).isEqualTo(1);
		assertThat(eventos("CUENTA_REACTIVADA", ana.getId())).isEqualTo(1);
		get(admin, "/admin/auditoria").andExpect(jsonPath("$[?(@.accion == 'ROL_ASIGNADO')].actor", contains("admin")));
	}

	@Test
	void unaAccionRechazadaNoDejaHuella() throws Exception {
		put(admin, "/admin/usuarios/" + admin.getId() + "/rol", json("rol", "PROFESOR"))
				.andExpect(status().isForbidden());
		post(admin, "/admin/usuarios/" + admin.getId() + "/desactivar", null).andExpect(status().isForbidden());
		post(admin, "/admin/usuarios/" + ana.getId() + "/reactivar", null).andExpect(status().isConflict());

		assertThat(contar("SELECT COUNT(*) FROM auditoria_evento WHERE accion IN "
				+ "('ROL_ASIGNADO','CUENTA_DESACTIVADA','CUENTA_REACTIVADA')")).isZero();
	}

	@Test
	void crearUnCursoDejaCursoRegistradoConSuCodigo() throws Exception {
		post(admin, "/admin/cursos", json("codigo", "INF263", "nombre", "Algoritmia")).andExpect(status().isCreated());
		post(admin, "/admin/cursos", json("codigo", "inf263", "nombre", "Repetido")).andExpect(status().isConflict());

		assertThat(eventos("CURSO_REGISTRADO", "INF263")).isEqualTo(1);
		assertThat(contar("SELECT COUNT(*) FROM auditoria_evento WHERE accion='CURSO_REGISTRADO'")).isEqualTo(1);
	}

	@Test
	void elListadoDeCursosIndicaSiTienenCanalEnElPeriodo() throws Exception {
		String curso = leer(post(admin, "/admin/cursos", json("codigo", "INF263", "nombre", "Algoritmia"))
				.andExpect(status().isCreated()), "$.id");
		post(admin, "/admin/cursos", json("codigo", "INF245", "nombre", "Redes")).andExpect(status().isCreated());
		post(admin, "/admin/canales", "{\"periodo\":\"2026-2\",\"cursoIds\":[\"" + curso + "\"]}")
				.andExpect(status().isCreated());

		get(admin, "/admin/cursos", "periodo", "2026-2").andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.codigo == 'INF263')].tieneCanal", contains(true)))
				.andExpect(jsonPath("$[?(@.codigo == 'INF245')].tieneCanal", contains(false)));
		get(admin, "/admin/cursos", "periodo", "2026-1")
				.andExpect(jsonPath("$[?(@.codigo == 'INF263')].tieneCanal", contains(false)));
		get(admin, "/admin/cursos").andExpect(jsonPath("$[0].tieneCanal").value(false));
	}

}

package com.hellofriend.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.hellofriend.PruebaIntegracion;
import com.hellofriend.notificaciones.aplicacion.EmisorNotificaciones;
import com.hellofriend.notificaciones.dominio.Notificacion;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;
import com.hellofriend.notificaciones.infraestructura.NotificacionRepositorio;
import com.hellofriend.usuarios.dominio.Rol;
import com.hellofriend.usuarios.dominio.Usuario;
import com.hellofriend.usuarios.infraestructura.UsuarioRepositorio;

/**
 * Lo que usará el módulo de incidencias para crear avisos: uno por destinatario, sin repetidos y con texto que cabe.
 */
class EmisorNotificacionesIT extends PruebaIntegracion {

	@Autowired
	private EmisorNotificaciones emisor;

	@Autowired
	private NotificacionRepositorio repositorio;

	@Autowired
	private UsuarioRepositorio usuarios;

	private Usuario ana;
	private Usuario luis;

	@BeforeEach
	void crearUsuarios() {
		ana = usuarios.save(new Usuario("ana@pucp.edu.pe", "ana", "hash", Rol.ESTUDIANTE, LocalDateTime.now()));
		luis = usuarios
				.saveAndFlush(new Usuario("luis@pucp.edu.pe", "luis", "hash", Rol.ESTUDIANTE, LocalDateTime.now()));
	}

	private int notificar(List<String> destinatarios, TipoNotificacion tipo, String titulo, String mensaje) {
		return emisor.notificar(destinatarios, tipo, null, null, titulo, mensaje);
	}

	@Test
	void creaUnAvisoSinLeerPorCadaDestinatario() {
		int creadas = notificar(List.of(ana.getId(), luis.getId()), TipoNotificacion.APROBADA, "Incidencia aprobada",
				"Tu incidencia fue aprobada y publicada.");

		assertThat(creadas).isEqualTo(2);
		List<Notificacion> deAna = repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(ana.getId());
		assertThat(deAna).hasSize(1);
		Notificacion n = deAna.get(0);
		assertThat(n.getTipo()).isEqualTo(TipoNotificacion.APROBADA);
		assertThat(n.getTitulo()).isEqualTo("Incidencia aprobada");
		assertThat(n.getMensaje()).isEqualTo("Tu incidencia fue aprobada y publicada.");
		assertThat(n.isLeida()).isFalse();
		assertThat(n.getId()).hasSize(36);
		assertThat(n.getCreadaEn()).isBetween(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),
				LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));
		assertThat(repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(luis.getId())).hasSize(1);
	}

	@Test
	void ignoraLosDestinatariosRepetidosNulosYVacios() {
		List<String> destinatarios = Arrays.asList(ana.getId(), ana.getId(), null, "", "   ", luis.getId());

		int creadas = notificar(destinatarios, TipoNotificacion.RESUELTA, "Resuelta", "Fue resuelta.");

		assertThat(creadas).isEqualTo(2);
		assertThat(repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(ana.getId())).hasSize(1);
	}

	@Test
	void sinDestinatariosNoCreaNada() {
		assertThat(notificar(List.of(), TipoNotificacion.RESUELTA, "Resuelta", "Fue resuelta.")).isZero();
		assertThat(repositorio.count()).isZero();
	}

	@Test
	void recortaElTituloYElMensajeSiNoCaben() {
		notificar(List.of(ana.getId()), TipoNotificacion.OBSERVADA, "t".repeat(400), "m".repeat(900));

		Notificacion n = repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(ana.getId()).get(0);
		assertThat(n.getTitulo()).hasSize(150).endsWith("…");
		assertThat(n.getMensaje()).hasSize(500).endsWith("…");
	}

	@Test
	void laBaseAceptaTodosLosTiposDelEnum() {
		for (TipoNotificacion tipo : TipoNotificacion.values()) {
			notificar(List.of(ana.getId()), tipo, tipo.name(), "Aviso de prueba.");
		}

		assertThat(repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(ana.getId()))
				.hasSize(TipoNotificacion.values().length);
	}

	@Test
	void incidenciaYCanalSonOpcionales() {
		// Sin incidencia ni canal: las FK exigen filas reales de esos módulos, que aún no existen.
		emisor.notificar(List.of(ana.getId()), TipoNotificacion.EN_ATENCION, null, null, "En atención", "En atención.");

		Notificacion n = repositorio.findByDestinatarioIdOrderByCreadaEnDescIdDesc(ana.getId()).get(0);
		assertThat(n.getIncidenciaId()).isNull();
		assertThat(n.getCanalId()).isNull();
	}

	@Test
	void unDestinatarioInexistenteLoRechazaLaBase() {
		assertThatThrownBy(() -> notificar(List.of("00000000-0000-0000-0000-000000000000"), TipoNotificacion.APROBADA,
				"Aprobada", "Aprobada.")).isInstanceOf(DataIntegrityViolationException.class);
	}

}

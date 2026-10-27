package com.hellofriend.auditoria.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.auditoria.infraestructura.AuditoriaRepositorio;

/**
 * Registro de acciones (quién hizo qué y cuándo). Es la única puerta para escribir en la auditoría: los demás módulos
 * llaman a {@code registrar} dentro de su propia transacción, así la acción y su huella se guardan juntas o ninguna.
 */
@Service
@Transactional
public class ServicioAuditoria {

	private static final String CONFIRMADA = "INCIDENCIA_CONFIRMADA";

	private final AuditoriaRepositorio repositorio;
	private final Clock reloj;

	public ServicioAuditoria(AuditoriaRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	/** Registra una acción. {@code actorId} e {@code incidenciaId} pueden ser null. */
	public void registrar(String accion, String actorId, String incidenciaId, String detalle) {
		repositorio.insertar(UUID.randomUUID().toString(), accion, actorId, incidenciaId, detalle,
				LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
	}

	/** Registra una acción que no es de una incidencia. */
	public void registrar(String accion, String actorId, String detalle) {
		registrar(accion, actorId, null, detalle);
	}

	/** Todos los eventos, del más nuevo al más viejo. */
	@Transactional(readOnly = true)
	public List<EventoAuditoriaDto> listar() {
		return repositorio.todos().stream().map(e -> new EventoAuditoriaDto(e.id(), e.accion(),
				e.actor() == null ? "Sistema" : e.actor(), e.incidenciaId(), e.detalle(), e.fecha())).toList();
	}

	/** Confirmaciones de una incidencia y sus retiros, con identidad. Es para el reporte del administrador. */
	@Transactional(readOnly = true)
	public List<ConfirmacionAuditada> confirmacionesDe(String incidenciaId) {
		return repositorio.confirmacionesDe(incidenciaId).stream()
				.map(c -> new ConfirmacionAuditada(!CONFIRMADA.equals(c.accion()), c.fecha(), c.usuario())).toList();
	}

}

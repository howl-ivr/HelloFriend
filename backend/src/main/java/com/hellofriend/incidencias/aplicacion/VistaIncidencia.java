package com.hellofriend.incidencias.aplicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hellofriend.auditoria.aplicacion.ServicioAuditoria;
import com.hellofriend.canales.aplicacion.ServicioCanales;
import com.hellofriend.compartido.seguridad.UsuarioActual;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto.Autor;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto.CanalResumen;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto.Evento;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto.Revision;
import com.hellofriend.incidencias.dominio.ComentarioIncidencia;
import com.hellofriend.incidencias.dominio.EstadoIncidencia;
import com.hellofriend.incidencias.dominio.Observacion;
import com.hellofriend.incidencias.dominio.Prioridad;
import com.hellofriend.incidencias.dominio.TipoReaccion;
import com.hellofriend.incidencias.dominio.VersionIncidencia;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio.FilaIncidencia;

/**
 * Arma lo que ve cada persona de una incidencia: qué campos recibe y qué acciones puede hacer según su rol, si es la
 * autora y si está suscrita al canal. Aquí está la regla de anonimato: el autor solo llega al director y al
 * administrador, y la identidad de quien confirmó solo la ve el administrador en su reporte.
 */
@Component
class VistaIncidencia {

	private final IncidenciaRepositorio repositorio;
	private final ServicioCanales canales;
	private final ServicioAuditoria auditoria;

	VistaIncidencia(IncidenciaRepositorio repositorio, ServicioCanales canales, ServicioAuditoria auditoria) {
		this.repositorio = repositorio;
		this.canales = canales;
		this.auditoria = auditoria;
	}

	/** Vista de una incidencia; con {@code detalle} agrega historial, comentarios y, si corresponde, versiones. */
	IncidenciaDto de(FilaIncidencia i, UsuarioActual actor, boolean detalle) {
		String id = i.id();
		EstadoIncidencia estado = i.estado();
		boolean mia = actor.id().equals(i.autorId());
		boolean director = actor.esDirector();
		boolean administrador = actor.esAdministrador();
		boolean interactuable = estado.interactuable() && actor.esEstudiante()
				&& canales.estaSuscrito(i.canalId(), actor.id());

		Map<String, Integer> reacciones = new LinkedHashMap<>();
		for (TipoReaccion tipo : TipoReaccion.values()) {
			reacciones.put(tipo.name(), 0);
		}
		reacciones.putAll(repositorio.reacciones(id));
		List<Observacion> observaciones = repositorio.observaciones(id);
		String observacion = (mia || director) && estado == EstadoIncidencia.OBSERVADA && !observaciones.isEmpty()
				? observaciones.getLast().texto()
				: null;

		List<Observacion> observacionesVisibles = null;
		List<VersionIncidencia> versiones = null;
		List<Evento> historial = null;
		List<ComentarioIncidencia> comentarios = null;
		if (detalle) {
			observacionesVisibles = mia || director ? observaciones : null;
			versiones = director || administrador ? repositorio.versiones(id) : null;
			historial = historialPara(id, mia, director || administrador);
			comentarios = repositorio.comentarios(id);
		}

		return new IncidenciaDto(id, i.titulo(), i.descripcion(), i.categoria(), estado.api(), i.creadoEn(),
				new CanalResumen(i.canalId(), i.cursoCodigo() + " · " + i.cursoNombre(), i.anio() + "-" + i.semestre()),
				director || administrador ? new Autor(i.autorUsuario(), null) : null, i.totalConfirmaciones(),
				Prioridad.de(i.totalConfirmaciones()).name(), repositorio.confirmadaPor(id, actor.id()), reacciones,
				repositorio.reaccionDe(id, actor.id()).orElse(null), repositorio.totalComentarios(id), mia,
				interactuable && !mia, interactuable, interactuable, mia && estado.editable(),
				mia && estado == EstadoIncidencia.OBSERVADA && repositorio.editadaDespuesDeLaUltimaObservacion(id),
				mia || director || administrador ? revision(i) : null, observacion,
				estado.visible() || mia || director || administrador ? i.solucion() : null, i.resueltaEn(),
				observacionesVisibles, versiones, historial, comentarios, null);
	}

	/** Reporte del administrador: el detalle completo, el autor con su correo y las confirmaciones con identidad. */
	IncidenciaDto deReporte(FilaIncidencia i, UsuarioActual administrador) {
		List<Evento> eventos = new ArrayList<>(historialPara(i.id(), true, true));
		for (var c : auditoria.confirmacionesDe(i.id())) {
			eventos.add(new Evento(c.retirada() ? "CONFIRMACION_RETIRADA" : "CONFIRMADA", null, c.fecha(), null,
					c.usuario()));
		}
		eventos.sort(Comparator.comparing(Evento::fecha));
		return de(i, administrador, true).comoReporte(new Autor(i.autorUsuario(), i.autorCorreo()), eventos);
	}

	private Revision revision(FilaIncidencia i) {
		if (i.estado() == EstadoIncidencia.RECHAZADA) {
			Optional<String> motivo = i.revisionMotivoId() == null
					? Optional.empty()
					: repositorio.descripcionMotivo(i.revisionMotivoId());
			return new Revision("RECHAZADA", motivo.orElse(i.motivoRechazo()), i.revisionComentario(),
					repositorio.ultimaFechaEstado(i.id(), EstadoIncidencia.RECHAZADA));
		}
		if (i.aprobadaEn() != null) {
			return new Revision("APROBADA", null, null, i.aprobadaEn());
		}
		return null;
	}

	/**
	 * Historial visible: la autora ve los cambios de estado sin detalle ni responsable; el director y el administrador
	 * ven además las ediciones, con responsable y detalle. Los demás no ven nada.
	 */
	private List<Evento> historialPara(String id, boolean autora, boolean director) {
		if (!autora && !director) {
			return List.of();
		}
		return repositorio.historial(id).stream().filter(h -> director || !"EDITADA".equals(h.tipoEvento()))
				.map(h -> new Evento(h.tipoEvento() == null ? tipoDeEvento(h.estadoNuevo()) : h.tipoEvento(),
						estadoApi(h.estadoNuevo()), h.fecha(), director ? h.detalle() : null,
						director ? h.usuario() : null))
				.toList();
	}

	private static String tipoDeEvento(String estado) {
		return "PENDIENTE".equals(estado) ? "CREADA" : estado;
	}

	private static String estadoApi(String estado) {
		return EstadoIncidencia.deApi(estado).map(EstadoIncidencia::api).orElse(estado);
	}

}

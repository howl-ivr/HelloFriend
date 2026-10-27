package com.hellofriend.incidencias.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.auditoria.aplicacion.ServicioAuditoria;
import com.hellofriend.canales.aplicacion.ServicioCanales;
import com.hellofriend.compartido.seguridad.UsuarioActual;
import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.incidencias.dominio.ComentarioIncidencia;
import com.hellofriend.incidencias.dominio.EstadoIncidencia;
import com.hellofriend.incidencias.dominio.TipoReaccion;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio.FilaIncidencia;
import com.hellofriend.notificaciones.aplicacion.EmisorNotificaciones;
import com.hellofriend.notificaciones.dominio.TipoNotificacion;
import com.hellofriend.usuarios.aplicacion.ServicioUsuarios;
import com.hellofriend.usuarios.dominio.Rol;

/**
 * Casos de uso que cambian incidencias: registrar, editar y reenviar (estudiante), confirmar, reaccionar y comentar,
 * y las decisiones del director. Cada cambio deja su historial y su auditoría, y avisa a quien corresponde, todo en
 * la misma transacción.
 */
@Service
@Transactional
public class ServicioIncidencias {

	private final IncidenciaRepositorio repositorio;
	private final VistaIncidencia vista;
	private final ServicioCanales canales;
	private final ServicioAuditoria auditoria;
	private final EmisorNotificaciones avisos;
	private final ServicioUsuarios usuarios;
	private final Clock reloj;

	public ServicioIncidencias(IncidenciaRepositorio repositorio, VistaIncidencia vista, ServicioCanales canales,
			ServicioAuditoria auditoria, EmisorNotificaciones avisos, ServicioUsuarios usuarios, Clock reloj) {
		this.repositorio = repositorio;
		this.vista = vista;
		this.canales = canales;
		this.auditoria = auditoria;
		this.avisos = avisos;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	// --- estudiante

	/** Registra una incidencia PENDIENTE, suscribe a su autora al canal y avisa a los directores. */
	public IncidenciaDto crear(UsuarioActual actor, ContenidoIncidencia contenido) {
		String categoriaId = validarContenido(contenido);
		if (contenido.canalId() == null) {
			throw ExcepcionNegocio.invalido("Elige un canal.");
		}
		canales.exigirActivo(contenido.canalId());
		String id = UUID.randomUUID().toString();
		LocalDateTime fecha = ahora();
		repositorio.insertar(id, id.replace("-", "").substring(0, 16), contenido.canalId(), actor.id(), categoriaId,
				contenido.titulo().strip(), contenido.descripcion().strip(), fecha);
		registrarVersion(id, 1, contenido, categoriaId, fecha);
		registrarHistorial(id, null, EstadoIncidencia.PENDIENTE, "CREADA", actor, fecha, null);
		canales.suscribirSiNoLo(contenido.canalId(), actor.id());
		auditoria.registrar("INCIDENCIA_REGISTRADA", actor.id(), id, null);
		FilaIncidencia registrada = buscar(id);
		avisos.notificar(directores(), TipoNotificacion.NUEVA, id, registrada.canalId(), "Nueva incidencia pendiente",
				"Hay una nueva incidencia pendiente de revisión en " + registrada.cursoCodigo() + " · "
						+ registrada.cursoNombre() + ".");
		return vista.de(registrada, actor, false);
	}

	/** Solo su autora y solo mientras esté pendiente u observada; crea una versión nueva. */
	public IncidenciaDto editar(UsuarioActual actor, String id, ContenidoIncidencia contenido) {
		FilaIncidencia i = buscar(id);
		exigirAutora(i, actor, "Solo el autor puede editar su incidencia.");
		if (!i.estado().editable()) {
			throw ExcepcionNegocio.conflicto("Solo se puede editar una incidencia pendiente u observada.");
		}
		String categoriaId = validarContenido(contenido);
		if (Objects.equals(contenido.titulo().strip(), i.titulo())
				&& Objects.equals(contenido.descripcion().strip(), i.descripcion())
				&& Objects.equals(categoriaId, i.categoriaId())) {
			throw ExcepcionNegocio.invalido("No hay cambios que guardar.");
		}
		LocalDateTime fecha = ahora();
		int numero = repositorio.siguienteNumeroDeVersion(id);
		repositorio.actualizarContenido(id, contenido.titulo().strip(), contenido.descripcion().strip(), categoriaId,
				fecha);
		registrarVersion(id, numero, contenido, categoriaId, fecha);
		registrarHistorial(id, i.estado(), i.estado(), "EDITADA", actor, fecha, "Versión " + numero);
		auditoria.registrar("INCIDENCIA_EDITADA", actor.id(), id, "Versión " + numero);
		return vista.de(buscar(id), actor, true);
	}

	/** Vuelve a revisión una incidencia observada, siempre que la autora la haya editado después de la observación. */
	public IncidenciaDto reenviar(UsuarioActual actor, String id) {
		FilaIncidencia i = buscar(id);
		exigirAutora(i, actor, "Solo el autor puede reenviar su incidencia.");
		if (i.estado() != EstadoIncidencia.OBSERVADA) {
			throw ExcepcionNegocio.conflicto("Solo se puede reenviar una incidencia observada.");
		}
		if (!repositorio.editadaDespuesDeLaUltimaObservacion(id)) {
			throw ExcepcionNegocio.conflicto("Edita la incidencia antes de reenviarla.");
		}
		LocalDateTime fecha = ahora();
		repositorio.cambiarEstado(id, EstadoIncidencia.PENDIENTE, fecha);
		registrarHistorial(id, EstadoIncidencia.OBSERVADA, EstadoIncidencia.PENDIENTE, "REENVIADA", actor, fecha, null);
		auditoria.registrar("INCIDENCIA_REENVIADA", actor.id(), id, null);
		avisos.notificar(directores(), TipoNotificacion.REENVIADA, id, i.canalId(), "Incidencia reenviada",
				"Una incidencia observada volvió a revisión.");
		return vista.de(buscar(id), actor, true);
	}

	// --- estudiantes suscritos

	/** Alterna la confirmación: la pone si no estaba y la quita si estaba. No vale para la propia incidencia. */
	public ConfirmacionDto confirmar(UsuarioActual actor, String id) {
		FilaIncidencia i = buscar(id);
		if (!i.estado().interactuable() || actor.id().equals(i.autorId())
				|| !canales.estaSuscrito(i.canalId(), actor.id())) {
			throw ExcepcionNegocio.prohibido("No puedes confirmar esta incidencia.");
		}
		boolean anterior = repositorio.confirmadaPor(id, actor.id());
		if (anterior) {
			repositorio.quitarConfirmacion(id, actor.id());
		} else {
			repositorio.insertarConfirmacion(UUID.randomUUID().toString(), id, actor.id(), ahora());
		}
		int total = repositorio.recalcularConfirmaciones(id);
		auditoria.registrar(anterior ? "INCIDENCIA_CONFIRMACION_RETIRADA" : "INCIDENCIA_CONFIRMADA", actor.id(), id,
				null);
		return new ConfirmacionDto(total, !anterior);
	}

	/** Una reacción por persona: reemplaza la anterior; con {@code tipo} null la retira. */
	public ReaccionDto reaccionar(UsuarioActual actor, String id, String tipo) {
		FilaIncidencia i = buscar(id);
		if (!i.estado().interactuable() || !canales.estaSuscrito(i.canalId(), actor.id())) {
			throw ExcepcionNegocio.prohibido("No puedes reaccionar a esta incidencia.");
		}
		if (tipo != null && !TipoReaccion.existe(tipo)) {
			throw ExcepcionNegocio.invalido("Tipo de reacción desconocido.");
		}
		repositorio.quitarReaccion(id, actor.id());
		if (tipo != null) {
			repositorio.insertarReaccion(UUID.randomUUID().toString(), id, actor.id(), tipo);
		}
		return new ReaccionDto(vista.de(buscar(id), actor, false).reacciones(), tipo);
	}

	/** Comenta eligiendo una opción de la lista predefinida. */
	public ComentarioIncidencia comentar(UsuarioActual actor, String id, String predefinidoId) {
		FilaIncidencia i = buscar(id);
		if (!i.estado().interactuable() || !canales.estaSuscrito(i.canalId(), actor.id())) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para comentar.");
		}
		String texto = (predefinidoId == null
				? Optional.<String>empty()
				: repositorio.textoComentarioPredefinido(predefinidoId))
				.orElseThrow(() -> ExcepcionNegocio.invalido("Elige un comentario de la lista."));
		String nuevo = UUID.randomUUID().toString();
		LocalDateTime fecha = ahora();
		repositorio.insertarComentario(nuevo, id, actor.id(), predefinidoId, fecha);
		return new ComentarioIncidencia(nuevo, texto, fecha);
	}

	// --- director

	/**
	 * Decisión del director: aprobar, observar, rechazar, atender o resolver. Cada una exige el estado de partida, deja
	 * historial y auditoría, y avisa: a la autora si es observar o rechazar; a la autora y los suscritos en el resto.
	 */
	public IncidenciaDto decidir(UsuarioActual actor, String id, String accion, Map<String, String> cuerpo) {
		FilaIncidencia i = buscar(id);
		EstadoIncidencia anterior = i.estado();
		Map<String, String> body = cuerpo == null ? Map.of() : cuerpo;
		LocalDateTime fecha = ahora();
		EstadoIncidencia nuevo;
		String detalle = null;
		switch (accion) {
			case "aprobar" -> {
				exigirEstado(anterior, EstadoIncidencia.PENDIENTE);
				nuevo = EstadoIncidencia.APROBADA;
				repositorio.aprobar(id, fecha);
			}
			case "observar" -> {
				exigirEstado(anterior, EstadoIncidencia.PENDIENTE);
				detalle = texto(body.get("observacion"), 10, 500, "La observación");
				nuevo = EstadoIncidencia.OBSERVADA;
				repositorio.insertarObservacion(UUID.randomUUID().toString(), id, repositorio.ultimaVersionId(id),
						detalle, actor.id(), fecha);
				repositorio.cambiarEstado(id, nuevo, fecha);
			}
			case "rechazar" -> {
				exigirEstado(anterior, EstadoIncidencia.PENDIENTE);
				detalle = rechazar(id, body, fecha);
				nuevo = EstadoIncidencia.RECHAZADA;
			}
			case "atender" -> {
				exigirEstado(anterior, EstadoIncidencia.APROBADA);
				nuevo = EstadoIncidencia.EN_ATENCION;
				repositorio.cambiarEstado(id, nuevo, fecha);
			}
			case "resolver" -> {
				if (anterior != EstadoIncidencia.APROBADA && anterior != EstadoIncidencia.EN_ATENCION) {
					throw ExcepcionNegocio.conflicto("Solo se pueden resolver incidencias aprobadas.");
				}
				detalle = texto(body.get("solucion"), 10, 1000, "La solución");
				nuevo = EstadoIncidencia.RESUELTA;
				repositorio.resolver(id, detalle, fecha);
			}
			default -> throw ExcepcionNegocio.noEncontrado("Acción desconocida.");
		}
		registrarHistorial(id, anterior, nuevo, nuevo.name(), actor, fecha, detalle);
		auditoria.registrar("INCIDENCIA_" + nuevo.name(), actor.id(), id, detalle);
		boolean soloAutora = nuevo == EstadoIncidencia.OBSERVADA || nuevo == EstadoIncidencia.RECHAZADA;
		avisos.notificar(soloAutora ? List.of(i.autorId()) : autoraYSuscritos(i),
				TipoNotificacion.valueOf(nuevo.name()), id, i.canalId(), "Actualización de incidencia",
				"La incidencia «" + i.titulo() + "» está " + nuevo.api() + ".");
		return vista.de(buscar(id), actor, false);
	}

	/** Rechaza con un motivo de la lista, un comentario, o ambos; devuelve el texto que queda como motivo. */
	private String rechazar(String id, Map<String, String> body, LocalDateTime fecha) {
		String motivoId = body.get("motivoId");
		var motivo = motivoId == null ? Optional.<String>empty() : repositorio.descripcionMotivo(motivoId);
		String comentario = body.get("comentario") == null ? "" : body.get("comentario").strip();
		if (motivo.isEmpty() && comentario.isEmpty()) {
			throw ExcepcionNegocio.invalido("Elige un motivo o escribe un comentario.");
		}
		if (comentario.length() > 500) {
			throw ExcepcionNegocio.invalido("El comentario no puede superar los 500 caracteres.");
		}
		String detalle = motivo.isEmpty()
				? comentario
				: motivo.get() + (comentario.isEmpty() ? "" : " — " + comentario);
		repositorio.rechazar(id, detalle, motivo.isEmpty() ? null : motivoId, comentario.isEmpty() ? null : comentario,
				fecha);
		return detalle;
	}

	// --- internos

	private FilaIncidencia buscar(String id) {
		return repositorio.porId(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La incidencia no existe."));
	}

	private static void exigirAutora(FilaIncidencia i, UsuarioActual actor, String mensaje) {
		if (!actor.id().equals(i.autorId())) {
			throw ExcepcionNegocio.prohibido(mensaje);
		}
	}

	private static void exigirEstado(EstadoIncidencia actual, EstadoIncidencia esperado) {
		if (actual != esperado) {
			throw ExcepcionNegocio.conflicto("La incidencia ya cambió de estado.");
		}
	}

	private static String texto(String valor, int minimo, int maximo, String campo) {
		String limpio = valor == null ? "" : valor.strip();
		if (limpio.length() < minimo || limpio.length() > maximo) {
			throw ExcepcionNegocio.invalido(campo + " debe tener entre " + minimo + " y " + maximo + " caracteres.");
		}
		return limpio;
	}

	/** Valida título (5 a 100), descripción (20 a 1000) y categoría; devuelve el id de la categoría. */
	private String validarContenido(ContenidoIncidencia contenido) {
		String titulo = contenido.titulo();
		if (titulo == null || titulo.strip().length() < 5 || titulo.strip().length() > 100) {
			throw ExcepcionNegocio.invalido("El título debe tener de 5 a 100 caracteres.");
		}
		String descripcion = contenido.descripcion();
		if (descripcion == null || descripcion.strip().length() < 20 || descripcion.strip().length() > 1000) {
			throw ExcepcionNegocio.invalido("La descripción debe tener de 20 a 1000 caracteres.");
		}
		return repositorio.categoriaId(contenido.categoria())
				.orElseThrow(() -> ExcepcionNegocio.invalido("Elige una categoría de la lista."));
	}

	private void registrarVersion(String id, int numero, ContenidoIncidencia contenido, String categoriaId,
			LocalDateTime fecha) {
		repositorio.insertarVersion(UUID.randomUUID().toString(), id, numero, contenido.titulo().strip(),
				contenido.descripcion().strip(), categoriaId, fecha);
	}

	private void registrarHistorial(String id, EstadoIncidencia anterior, EstadoIncidencia nuevo, String tipo,
			UsuarioActual actor, LocalDateTime fecha, String detalle) {
		repositorio.insertarHistorial(UUID.randomUUID().toString(), id, anterior == null ? null : anterior.name(),
				nuevo.name(), actor.id(), fecha, tipo, detalle);
	}

	private List<String> directores() {
		return usuarios.idsActivosConRol(Rol.DIRECTOR);
	}

	private List<String> autoraYSuscritos(FilaIncidencia i) {
		List<String> ids = new ArrayList<>(canales.idsSuscritos(i.canalId()));
		ids.add(i.autorId());
		return ids;
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

}

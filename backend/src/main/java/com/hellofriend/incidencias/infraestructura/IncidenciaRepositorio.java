package com.hellofriend.incidencias.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hellofriend.incidencias.dominio.ComentarioIncidencia;
import com.hellofriend.incidencias.dominio.EstadoIncidencia;
import com.hellofriend.incidencias.dominio.EventoHistorial;
import com.hellofriend.incidencias.dominio.Observacion;
import com.hellofriend.incidencias.dominio.VersionIncidencia;

/**
 * Todo el SQL de incidencias. Las consultas de lectura unen el canal, el curso y el autor para armar la vista de una
 * incidencia; las escrituras solo tocan tablas de este módulo (la suscripción, la auditoría y los avisos pasan por el
 * servicio de su módulo).
 */
@Repository
public class IncidenciaRepositorio {

	/** Una incidencia con los datos de su categoría, curso, periodo y autor. */
	public record FilaIncidencia(String id, String canalId, String autorId, String categoriaId, String titulo,
			String descripcion, EstadoIncidencia estado, String motivoRechazo, String solucion,
			int totalConfirmaciones, LocalDateTime aprobadaEn, LocalDateTime resueltaEn, LocalDateTime creadoEn,
			String revisionComentario, String revisionMotivoId, String categoria, String cursoId, int anio,
			int semestre, String cursoCodigo, String cursoNombre, String autorUsuario, String autorCorreo) {
	}

	public record ComentarioPredefinido(String id, String texto) {
	}

	public record MotivoRechazo(String id, String descripcion) {
	}

	private static final String BASE = """
			SELECT i.*, k.nombre AS categoria, c.curso_id, c.anio, c.semestre, a.codigo AS curso_codigo,
			       a.nombre AS curso_nombre, u.usuario AS autor_usuario, u.correo AS autor_correo
			FROM incidencias_incidencia i
			JOIN incidencias_categoria k ON k.id = i.categoria_id
			JOIN canales_canal c ON c.id = i.canal_id
			JOIN academico_curso a ON a.id = c.curso_id
			JOIN usuarios_usuario u ON u.id = i.autor_id
			""";

	private static final String SUSCRITO = " AND EXISTS (SELECT 1 FROM canales_suscripcion s WHERE s.canal_id = i.canal_id"
			+ " AND s.usuario_id = ?)";

	private final JdbcTemplate jdbc;

	public IncidenciaRepositorio(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	// --- incidencias

	public Optional<FilaIncidencia> porId(String id) {
		return jdbc.query(BASE + " WHERE i.id = ?", (rs, n) -> fila(rs), id).stream().findFirst();
	}

	public List<FilaIncidencia> todas() {
		return jdbc.query(BASE, (rs, n) -> fila(rs));
	}

	/** Publicadas, en atención o resueltas de los canales a los que está suscrita la persona (o solo de uno). */
	public List<FilaIncidencia> visiblesParaSuscrito(String usuarioId, String canalId) {
		String sql = BASE + " WHERE i.estado IN ('APROBADA','EN_ATENCION','RESUELTA')" + SUSCRITO;
		if (canalId == null) {
			return jdbc.query(sql, (rs, n) -> fila(rs), usuarioId);
		}
		return jdbc.query(sql + " AND i.canal_id = ?", (rs, n) -> fila(rs), usuarioId, canalId);
	}

	/** Publicadas o en atención, con al menos una confirmación, de los canales de la persona. */
	public List<FilaIncidencia> conConfirmacionesParaSuscrito(String usuarioId) {
		return jdbc.query(BASE + " WHERE i.estado IN ('APROBADA','EN_ATENCION') AND i.total_confirmaciones > 0"
				+ SUSCRITO, (rs, n) -> fila(rs), usuarioId);
	}

	public List<FilaIncidencia> visiblesDeCanal(String canalId) {
		return jdbc.query(BASE + " WHERE i.canal_id = ? AND i.estado IN ('APROBADA','EN_ATENCION','RESUELTA')",
				(rs, n) -> fila(rs), canalId);
	}

	/** Las del autor en cualquier estado, las más nuevas primero. */
	public List<FilaIncidencia> deAutor(String autorId) {
		return jdbc.query(BASE + " WHERE i.autor_id = ? ORDER BY i.creado_en DESC", (rs, n) -> fila(rs), autorId);
	}

	public List<FilaIncidencia> enEstado(EstadoIncidencia estado) {
		return jdbc.query(BASE + " WHERE i.estado = ?", (rs, n) -> fila(rs), estado.name());
	}

	// --- catálogos

	public List<String> categorias() {
		return jdbc.queryForList("SELECT nombre FROM incidencias_categoria ORDER BY nombre", String.class);
	}

	public Optional<String> categoriaId(String nombre) {
		return jdbc.queryForList("SELECT id FROM incidencias_categoria WHERE nombre = ?", String.class, nombre).stream()
				.findFirst();
	}

	public List<ComentarioPredefinido> comentariosPredefinidos() {
		return jdbc.query("SELECT id, texto FROM incidencias_comentario_predefinido ORDER BY texto",
				(rs, n) -> new ComentarioPredefinido(rs.getString("id"), rs.getString("texto")));
	}

	public Optional<String> textoComentarioPredefinido(String id) {
		return jdbc.queryForList("SELECT texto FROM incidencias_comentario_predefinido WHERE id = ?", String.class, id)
				.stream().findFirst();
	}

	public List<MotivoRechazo> motivosDeRechazo() {
		return jdbc.query("SELECT id, descripcion FROM incidencias_motivo_rechazo ORDER BY descripcion",
				(rs, n) -> new MotivoRechazo(rs.getString("id"), rs.getString("descripcion")));
	}

	public Optional<String> descripcionMotivo(String id) {
		return jdbc.queryForList("SELECT descripcion FROM incidencias_motivo_rechazo WHERE id = ?", String.class, id)
				.stream().findFirst();
	}

	// --- detalle de una incidencia

	/** Cuántas reacciones hay de cada tipo (solo los tipos que tienen alguna). */
	public Map<String, Integer> reacciones(String incidenciaId) {
		Map<String, Integer> salida = new LinkedHashMap<>();
		jdbc.query("SELECT tipo, COUNT(*) AS total FROM incidencias_reaccion WHERE incidencia_id = ? GROUP BY tipo",
				rs -> {
					salida.put(rs.getString("tipo"), rs.getInt("total"));
				}, incidenciaId);
		return salida;
	}

	public Optional<String> reaccionDe(String incidenciaId, String usuarioId) {
		return jdbc.queryForList("SELECT tipo FROM incidencias_reaccion WHERE incidencia_id = ? AND usuario_id = ?",
				String.class, incidenciaId, usuarioId).stream().findFirst();
	}

	public boolean confirmadaPor(String incidenciaId, String estudianteId) {
		return contar("SELECT COUNT(*) FROM incidencias_confirmacion WHERE incidencia_id = ? AND estudiante_id = ?",
				incidenciaId, estudianteId) > 0;
	}

	public long totalComentarios(String incidenciaId) {
		return contar("SELECT COUNT(*) FROM incidencias_comentario WHERE incidencia_id = ?", incidenciaId);
	}

	public List<ComentarioIncidencia> comentarios(String incidenciaId) {
		return jdbc.query("""
				SELECT c.id, p.texto, c.creada_en AS fecha FROM incidencias_comentario c
				JOIN incidencias_comentario_predefinido p ON p.id = c.predefinido_id
				WHERE c.incidencia_id = ? ORDER BY c.creada_en
				""", (rs, n) -> new ComentarioIncidencia(rs.getString("id"), rs.getString("texto"),
				rs.getObject("fecha", LocalDateTime.class)), incidenciaId);
	}

	public List<Observacion> observaciones(String incidenciaId) {
		return jdbc.query("""
				SELECT o.texto, o.creada_en AS fecha, v.numero AS version FROM incidencias_observacion o
				JOIN incidencias_version v ON v.id = o.version_id WHERE o.incidencia_id = ? ORDER BY o.creada_en
				""", (rs, n) -> new Observacion(rs.getString("texto"), rs.getObject("fecha", LocalDateTime.class),
				rs.getInt("version")), incidenciaId);
	}

	public List<VersionIncidencia> versiones(String incidenciaId) {
		return jdbc.query("""
				SELECT v.numero AS n, v.titulo, v.descripcion, k.nombre AS categoria, v.creada_en AS fecha
				FROM incidencias_version v JOIN incidencias_categoria k ON k.id = v.categoria_id
				WHERE v.incidencia_id = ? ORDER BY v.numero
				""", (rs, n) -> new VersionIncidencia(rs.getInt("n"), rs.getString("titulo"),
				rs.getString("descripcion"), rs.getString("categoria"), rs.getObject("fecha", LocalDateTime.class)),
				incidenciaId);
	}

	public List<EventoHistorial> historial(String incidenciaId) {
		return jdbc.query("""
				SELECT h.tipo_evento, h.estado_nuevo, h.cambiado_en, h.detalle, u.usuario
				FROM incidencias_historial_estado h JOIN usuarios_usuario u ON u.id = h.cambiado_por
				WHERE h.incidencia_id = ? ORDER BY h.cambiado_en
				""", (rs, n) -> new EventoHistorial(rs.getString("tipo_evento"), rs.getString("estado_nuevo"),
				rs.getObject("cambiado_en", LocalDateTime.class), rs.getString("detalle"), rs.getString("usuario")),
				incidenciaId);
	}

	public LocalDateTime ultimaFechaEstado(String incidenciaId, EstadoIncidencia estado) {
		return jdbc.queryForObject(
				"SELECT MAX(cambiado_en) FROM incidencias_historial_estado WHERE incidencia_id = ? AND estado_nuevo = ?",
				LocalDateTime.class, incidenciaId, estado.name());
	}

	/** Verdadero si hubo una edición después de la última observación: solo entonces se puede reenviar. */
	public boolean editadaDespuesDeLaUltimaObservacion(String incidenciaId) {
		Integer actual = jdbc.queryForObject(
				"SELECT COALESCE(MAX(numero), 0) FROM incidencias_version WHERE incidencia_id = ?", Integer.class,
				incidenciaId);
		Integer observada = jdbc.queryForObject("""
				SELECT COALESCE(MAX(v.numero), 0) FROM incidencias_observacion o
				JOIN incidencias_version v ON v.id = o.version_id WHERE o.incidencia_id = ?
				""", Integer.class, incidenciaId);
		return actual != null && observada != null && actual > observada;
	}

	/** Nombres de usuario de quienes confirmaron o cambiaron el estado de la incidencia. */
	public List<String> usuariosInvolucrados(String incidenciaId) {
		return jdbc.queryForList("""
				SELECT u.usuario FROM incidencias_confirmacion c JOIN usuarios_usuario u ON u.id = c.estudiante_id
				WHERE c.incidencia_id = ? UNION ALL
				SELECT u.usuario FROM incidencias_historial_estado h JOIN usuarios_usuario u ON u.id = h.cambiado_por
				WHERE h.incidencia_id = ?
				""", String.class, incidenciaId, incidenciaId);
	}

	// --- escritura

	public void insertar(String id, String codigoSeguimiento, String canalId, String autorId, String categoriaId,
			String titulo, String descripcion, LocalDateTime fecha) {
		jdbc.update(
				"""
						INSERT INTO incidencias_incidencia
						(id, codigo_seguimiento, canal_id, autor_id, categoria_id, titulo, descripcion, estado, creado_en, actualizado_en)
						VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?)
						""",
				id, codigoSeguimiento, canalId, autorId, categoriaId, titulo, descripcion, fecha, fecha);
	}

	public void actualizarContenido(String id, String titulo, String descripcion, String categoriaId,
			LocalDateTime fecha) {
		jdbc.update(
				"UPDATE incidencias_incidencia SET titulo = ?, descripcion = ?, categoria_id = ?, actualizado_en = ? WHERE id = ?",
				titulo, descripcion, categoriaId, fecha, id);
	}

	public int siguienteNumeroDeVersion(String incidenciaId) {
		Integer siguiente = jdbc.queryForObject(
				"SELECT COALESCE(MAX(numero), 0) + 1 FROM incidencias_version WHERE incidencia_id = ?", Integer.class,
				incidenciaId);
		return siguiente == null ? 1 : siguiente;
	}

	public void insertarVersion(String id, String incidenciaId, int numero, String titulo, String descripcion,
			String categoriaId, LocalDateTime fecha) {
		jdbc.update(
				"""
						INSERT INTO incidencias_version (id, incidencia_id, numero, titulo, descripcion, categoria_id, creada_en)
						VALUES (?, ?, ?, ?, ?, ?, ?)
						""",
				id, incidenciaId, numero, titulo, descripcion, categoriaId, fecha);
	}

	public String ultimaVersionId(String incidenciaId) {
		return jdbc.queryForObject(
				"SELECT id FROM incidencias_version WHERE incidencia_id = ? ORDER BY numero DESC LIMIT 1", String.class,
				incidenciaId);
	}

	public void insertarObservacion(String id, String incidenciaId, String versionId, String texto, String actorId,
			LocalDateTime fecha) {
		jdbc.update("""
				INSERT INTO incidencias_observacion (id, incidencia_id, version_id, texto, creada_por, creada_en)
				VALUES (?, ?, ?, ?, ?, ?)
				""", id, incidenciaId, versionId, texto, actorId, fecha);
	}

	public void insertarHistorial(String id, String incidenciaId, String estadoAnterior, String estadoNuevo,
			String actorId, LocalDateTime fecha, String tipoEvento, String detalle) {
		jdbc.update("""
				INSERT INTO incidencias_historial_estado
				(id, incidencia_id, estado_anterior, estado_nuevo, cambiado_por, cambiado_en, tipo_evento, detalle)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""", id, incidenciaId, estadoAnterior, estadoNuevo, actorId, fecha, tipoEvento, detalle);
	}

	public void cambiarEstado(String id, EstadoIncidencia nuevo, LocalDateTime fecha) {
		jdbc.update("UPDATE incidencias_incidencia SET estado = ?, actualizado_en = ? WHERE id = ?", nuevo.name(),
				fecha, id);
	}

	public void aprobar(String id, LocalDateTime fecha) {
		jdbc.update("UPDATE incidencias_incidencia SET estado = ?, aprobada_en = ?, actualizado_en = ? WHERE id = ?",
				EstadoIncidencia.APROBADA.name(), fecha, fecha, id);
	}

	public void rechazar(String id, String motivoRechazo, String motivoId, String comentario, LocalDateTime fecha) {
		jdbc.update("""
				UPDATE incidencias_incidencia SET estado = ?, motivo_rechazo = ?, revision_motivo_id = ?,
				       revision_comentario = ?, actualizado_en = ? WHERE id = ?
				""", EstadoIncidencia.RECHAZADA.name(), motivoRechazo, motivoId, comentario, fecha, id);
	}

	public void resolver(String id, String solucion, LocalDateTime fecha) {
		jdbc.update(
				"UPDATE incidencias_incidencia SET estado = ?, solucion = ?, resuelta_en = ?, actualizado_en = ? WHERE id = ?",
				EstadoIncidencia.RESUELTA.name(), solucion, fecha, fecha, id);
	}

	public void insertarConfirmacion(String id, String incidenciaId, String estudianteId, LocalDateTime fecha) {
		jdbc.update(
				"INSERT INTO incidencias_confirmacion (id, incidencia_id, estudiante_id, creada_en) VALUES (?, ?, ?, ?)",
				id, incidenciaId, estudianteId, fecha);
	}

	public void quitarConfirmacion(String incidenciaId, String estudianteId) {
		jdbc.update("DELETE FROM incidencias_confirmacion WHERE incidencia_id = ? AND estudiante_id = ?", incidenciaId,
				estudianteId);
	}

	/** Recalcula y guarda el total de confirmaciones; lo devuelve. */
	public int recalcularConfirmaciones(String incidenciaId) {
		Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM incidencias_confirmacion WHERE incidencia_id = ?",
				Integer.class, incidenciaId);
		int nuevo = total == null ? 0 : total;
		jdbc.update("UPDATE incidencias_incidencia SET total_confirmaciones = ? WHERE id = ?", nuevo, incidenciaId);
		return nuevo;
	}

	public void quitarReaccion(String incidenciaId, String usuarioId) {
		jdbc.update("DELETE FROM incidencias_reaccion WHERE incidencia_id = ? AND usuario_id = ?", incidenciaId,
				usuarioId);
	}

	public void insertarReaccion(String id, String incidenciaId, String usuarioId, String tipo) {
		jdbc.update("INSERT INTO incidencias_reaccion (id, incidencia_id, usuario_id, tipo) VALUES (?, ?, ?, ?)", id,
				incidenciaId, usuarioId, tipo);
	}

	public void insertarComentario(String id, String incidenciaId, String usuarioId, String predefinidoId,
			LocalDateTime fecha) {
		jdbc.update("""
				INSERT INTO incidencias_comentario (id, incidencia_id, usuario_id, predefinido_id, creada_en)
				VALUES (?, ?, ?, ?, ?)
				""", id, incidenciaId, usuarioId, predefinidoId, fecha);
	}

	// --- internos

	private long contar(String sql, Object... parametros) {
		Long total = jdbc.queryForObject(sql, Long.class, parametros);
		return total == null ? 0 : total;
	}

	private static FilaIncidencia fila(ResultSet rs) throws SQLException {
		return new FilaIncidencia(rs.getString("id"), rs.getString("canal_id"), rs.getString("autor_id"),
				rs.getString("categoria_id"), rs.getString("titulo"), rs.getString("descripcion"),
				EstadoIncidencia.valueOf(rs.getString("estado")), rs.getString("motivo_rechazo"),
				rs.getString("solucion"), rs.getInt("total_confirmaciones"),
				rs.getObject("aprobada_en", LocalDateTime.class), rs.getObject("resuelta_en", LocalDateTime.class),
				rs.getObject("creado_en", LocalDateTime.class), rs.getString("revision_comentario"),
				rs.getString("revision_motivo_id"), rs.getString("categoria"), rs.getString("curso_id"),
				rs.getInt("anio"), rs.getInt("semestre"), rs.getString("curso_codigo"), rs.getString("curso_nombre"),
				rs.getString("autor_usuario"), rs.getString("autor_correo"));
	}

}

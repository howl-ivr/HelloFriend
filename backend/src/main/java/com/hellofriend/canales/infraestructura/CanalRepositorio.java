package com.hellofriend.canales.infraestructura;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a {@code canales_canal} y {@code canales_suscripcion}. La consulta de lectura une el curso y cuenta
 * suscritos e incidencias para armar la vista del canal; las escrituras solo tocan tablas de este módulo.
 */
@Repository
public class CanalRepositorio {

	/** Un canal con sus contadores, tal como lo devuelve la consulta. */
	public record FilaCanal(String id, String cursoId, String codigo, String cursoNombre, int anio, int semestre,
			String estado, LocalDateTime creadoEn, long suscritos, long publicadas, long pendientes) {
	}

	private static final String CANALES = """
			SELECT c.id, c.curso_id, a.codigo, a.nombre AS curso_nombre, c.anio, c.semestre, c.estado, c.creado_en,
			       (SELECT COUNT(*) FROM canales_suscripcion s WHERE s.canal_id = c.id) AS suscritos,
			       (SELECT COUNT(*) FROM incidencias_incidencia i WHERE i.canal_id = c.id
			          AND i.estado IN ('APROBADA', 'EN_ATENCION', 'RESUELTA')) AS publicadas,
			       (SELECT COUNT(*) FROM incidencias_incidencia i WHERE i.canal_id = c.id
			          AND i.estado = 'PENDIENTE') AS pendientes
			FROM canales_canal c JOIN academico_curso a ON a.id = c.curso_id
			""";

	private final JdbcTemplate jdbc;

	public CanalRepositorio(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Todos los canales, del periodo más nuevo al más viejo. */
	public List<FilaCanal> todos() {
		return jdbc.query(CANALES + " ORDER BY c.anio DESC, c.semestre DESC, a.codigo", (rs, n) -> fila(rs));
	}

	/** Canales para el administrador, los más recientes primero; con año y semestre, solo de ese periodo. */
	public List<FilaCanal> deAdministracion(Integer anio, Integer semestre) {
		if (anio == null) {
			return jdbc.query(CANALES + " ORDER BY c.creado_en DESC", (rs, n) -> fila(rs));
		}
		return jdbc.query(CANALES + " WHERE c.anio = ? AND c.semestre = ? ORDER BY c.creado_en DESC",
				(rs, n) -> fila(rs), anio, semestre);
	}

	public Optional<FilaCanal> porId(String id) {
		return jdbc.query(CANALES + " WHERE c.id = ?", (rs, n) -> fila(rs), id).stream().findFirst();
	}

	/** Periodos con algún canal, sin repetir, del más nuevo al más viejo (por ejemplo "2026-2"). */
	public List<String> periodos() {
		return jdbc.queryForList("SELECT DISTINCT CONCAT(anio, '-', semestre) FROM canales_canal ORDER BY 1 DESC",
				String.class);
	}

	public boolean existeCurso(String cursoId) {
		return contar("SELECT COUNT(*) FROM academico_curso WHERE id = ?", cursoId) > 0;
	}

	/** Falla con {@code DataIntegrityViolationException} si ya hay un canal de ese curso y periodo. */
	public void insertar(String id, String cursoId, int anio, int semestre, String creadoPor, LocalDateTime fecha) {
		jdbc.update("""
				INSERT INTO canales_canal (id, curso_id, anio, semestre, estado, creado_por, creado_en)
				VALUES (?, ?, ?, ?, 'ACTIVO', ?, ?)
				""", id, cursoId, anio, semestre, creadoPor, fecha);
	}

	public void desactivar(String id, LocalDateTime fecha) {
		jdbc.update("UPDATE canales_canal SET estado = 'DESACTIVADO', desactivado_en = ? WHERE id = ?", fecha, id);
	}

	/** Ids de los cursos que ya tienen canal en ese periodo. */
	public List<String> cursosConCanal(int anio, int semestre) {
		return jdbc.queryForList("SELECT curso_id FROM canales_canal WHERE anio = ? AND semestre = ?", String.class,
				anio, semestre);
	}

	// --- suscripciones

	public boolean estaSuscrito(String canalId, String usuarioId) {
		return contar("SELECT COUNT(*) FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?", canalId,
				usuarioId) > 0;
	}

	/** Ids de los canales a los que está suscrita la persona. */
	public Set<String> canalesSuscritos(String usuarioId) {
		return new HashSet<>(
				jdbc.queryForList("SELECT canal_id FROM canales_suscripcion WHERE usuario_id = ?", String.class,
						usuarioId));
	}

	public List<String> idsSuscritos(String canalId) {
		return jdbc.queryForList("SELECT usuario_id FROM canales_suscripcion WHERE canal_id = ?", String.class,
				canalId);
	}

	/** Suscribe si no lo estaba; suscribirse dos veces no duplica. */
	public void suscribir(String id, String canalId, String usuarioId, LocalDateTime fecha) {
		jdbc.update("INSERT IGNORE INTO canales_suscripcion (id, canal_id, usuario_id, creada_en) VALUES (?, ?, ?, ?)",
				id, canalId, usuarioId, fecha);
	}

	public void desuscribir(String canalId, String usuarioId) {
		jdbc.update("DELETE FROM canales_suscripcion WHERE canal_id = ? AND usuario_id = ?", canalId, usuarioId);
	}

	private long contar(String sql, Object... parametros) {
		Long total = jdbc.queryForObject(sql, Long.class, parametros);
		return total == null ? 0 : total;
	}

	private static FilaCanal fila(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new FilaCanal(rs.getString("id"), rs.getString("curso_id"), rs.getString("codigo"),
				rs.getString("curso_nombre"), rs.getInt("anio"), rs.getInt("semestre"), rs.getString("estado"),
				rs.getObject("creado_en", LocalDateTime.class), rs.getLong("suscritos"), rs.getLong("publicadas"),
				rs.getLong("pendientes"));
	}

}

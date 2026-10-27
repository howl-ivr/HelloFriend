package com.hellofriend.canales.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.auditoria.aplicacion.ServicioAuditoria;
import com.hellofriend.canales.aplicacion.CanalDto.CursoResumenDto;
import com.hellofriend.canales.infraestructura.CanalRepositorio;
import com.hellofriend.canales.infraestructura.CanalRepositorio.FilaCanal;
import com.hellofriend.compartido.web.ExcepcionNegocio;

/**
 * Canales (un curso en un periodo): consulta, suscripción de estudiantes y profesores, y alta y baja por el
 * administrador. Los métodos de la segunda mitad son la API que usan otros módulos (por ejemplo incidencias).
 */
@Service
@Transactional
public class ServicioCanales {

	private static final String ACTIVO = "ACTIVO";

	private final CanalRepositorio repositorio;
	private final ServicioAuditoria auditoria;
	private final Clock reloj;

	public ServicioCanales(CanalRepositorio repositorio, ServicioAuditoria auditoria, Clock reloj) {
		this.repositorio = repositorio;
		this.auditoria = auditoria;
		this.reloj = reloj;
	}

	// --- estudiantes y profesores

	@Transactional(readOnly = true)
	public List<CanalDto> listar(String usuarioId) {
		Set<String> mios = repositorio.canalesSuscritos(usuarioId);
		return repositorio.todos().stream().map(f -> dto(f, mios.contains(f.id()), false)).toList();
	}

	@Transactional(readOnly = true)
	public CanalDto detalle(String id, String usuarioId) {
		return dto(buscar(id), repositorio.estaSuscrito(id, usuarioId), false);
	}

	/** Suscribirse dos veces no duplica. Un canal desactivado no admite suscripciones nuevas. */
	public CanalDto suscribir(String id, String usuarioId) {
		if (!ACTIVO.equals(buscar(id).estado())) {
			throw ExcepcionNegocio.conflicto("Este canal está desactivado.");
		}
		repositorio.suscribir(UUID.randomUUID().toString(), id, usuarioId, ahora());
		return dto(buscar(id), true, false);
	}

	/** Darse de baja siempre se puede, también de un canal desactivado. */
	public CanalDto desuscribir(String id, String usuarioId) {
		buscar(id);
		repositorio.desuscribir(id, usuarioId);
		return dto(buscar(id), false, false);
	}

	// --- administración

	@Transactional(readOnly = true)
	public List<String> periodos() {
		return repositorio.periodos();
	}

	@Transactional(readOnly = true)
	public List<CanalDto> listarAdmin(String periodo, String usuarioId) {
		Set<String> mios = repositorio.canalesSuscritos(usuarioId);
		List<FilaCanal> filas;
		if (periodo == null || periodo.isBlank()) {
			filas = repositorio.deAdministracion(null, null);
		} else {
			int[] partes = periodo(periodo);
			filas = repositorio.deAdministracion(partes[0], partes[1]);
		}
		return filas.stream().map(f -> dto(f, mios.contains(f.id()), true)).toList();
	}

	/** Crea un canal activo por cada curso (sin repetir) para el periodo. Un curso ya con canal da 409. */
	public List<CanalDto> crear(SolicitudCanales solicitud, String actorId) {
		int[] partes = periodo(solicitud.periodo());
		if (solicitud.cursoIds() == null || solicitud.cursoIds().isEmpty()) {
			throw ExcepcionNegocio.invalido("Elige al menos un curso.");
		}
		List<CanalDto> creados = new ArrayList<>();
		for (String cursoId : solicitud.cursoIds().stream().distinct().toList()) {
			if (cursoId == null || !repositorio.existeCurso(cursoId)) {
				throw ExcepcionNegocio.invalido("Uno de los cursos no existe.");
			}
			String id = UUID.randomUUID().toString();
			try {
				repositorio.insertar(id, cursoId, partes[0], partes[1], actorId, ahora());
			} catch (DataIntegrityViolationException e) {
				throw ExcepcionNegocio.conflicto("Ya existe un canal para ese curso y periodo.");
			}
			auditoria.registrar("CANAL_CREADO", actorId, id);
			creados.add(dto(buscar(id), repositorio.estaSuscrito(id, actorId), true));
		}
		return creados;
	}

	public CanalDto desactivar(String id, String actorId) {
		if (!ACTIVO.equals(buscar(id).estado())) {
			throw ExcepcionNegocio.conflicto("El canal ya está desactivado.");
		}
		repositorio.desactivar(id, ahora());
		auditoria.registrar("CANAL_DESACTIVADO", actorId, id);
		return dto(buscar(id), repositorio.estaSuscrito(id, actorId), true);
	}

	// --- API para otros módulos

	/** Falla con 404 si el canal no existe y con 409 si está desactivado. */
	@Transactional(readOnly = true)
	public void exigirActivo(String canalId) {
		FilaCanal canal = repositorio.porId(canalId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El canal no existe."));
		if (!ACTIVO.equals(canal.estado())) {
			throw ExcepcionNegocio.conflicto("Este canal está desactivado.");
		}
	}

	@Transactional(readOnly = true)
	public boolean estaSuscrito(String canalId, String usuarioId) {
		return repositorio.estaSuscrito(canalId, usuarioId);
	}

	/** Suscribe a la persona si todavía no lo estaba (por ejemplo, al registrar una incidencia). */
	public void suscribirSiNoLo(String canalId, String usuarioId) {
		repositorio.suscribir(UUID.randomUUID().toString(), canalId, usuarioId, ahora());
	}

	@Transactional(readOnly = true)
	public List<String> idsSuscritos(String canalId) {
		return repositorio.idsSuscritos(canalId);
	}

	/** Ids de los cursos con canal en ese periodo. Un periodo ausente o mal escrito se ignora (lista vacía). */
	@Transactional(readOnly = true)
	public Set<String> idsDeCursosConCanal(String periodo) {
		if (periodo == null || !periodo.matches("\\d{4}-[12]")) {
			return Set.of();
		}
		return new HashSet<>(repositorio.cursosConCanal(Integer.parseInt(periodo.substring(0, 4)),
				Integer.parseInt(periodo.substring(5))));
	}

	// --- internos

	private FilaCanal buscar(String id) {
		return repositorio.porId(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El canal no existe."));
	}

	private CanalDto dto(FilaCanal c, boolean suscrito, boolean admin) {
		String periodo = c.anio() + "-" + c.semestre();
		return new CanalDto(c.id(), c.codigo() + " · " + c.cursoNombre(),
				"Incidencias del curso de " + c.cursoNombre() + " (" + periodo + ")", periodo,
				ACTIVO.equals(c.estado()), new CursoResumenDto(c.cursoId(), c.codigo(), c.cursoNombre()), suscrito,
				c.suscritos(), c.publicadas(), admin ? c.pendientes() : null, admin ? c.creadoEn() : null);
	}

	private static int[] periodo(String valor) {
		if (valor == null || !valor.matches("\\d{4}-[12]")) {
			throw ExcepcionNegocio.invalido("El periodo debe tener el formato año-semestre, por ejemplo 2026-2.");
		}
		int anio = Integer.parseInt(valor.substring(0, 4));
		if (anio < 2000 || anio > 2100) {
			throw ExcepcionNegocio.invalido("El año del periodo debe estar entre 2000 y 2100.");
		}
		return new int[]{anio, Integer.parseInt(valor.substring(5))};
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
	}

}

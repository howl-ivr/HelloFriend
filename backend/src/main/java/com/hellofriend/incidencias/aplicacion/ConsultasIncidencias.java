package com.hellofriend.incidencias.aplicacion;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hellofriend.canales.aplicacion.ServicioCanales;
import com.hellofriend.compartido.seguridad.UsuarioActual;
import com.hellofriend.compartido.web.ExcepcionNegocio;
import com.hellofriend.incidencias.aplicacion.EstadisticasDto.ConteoPorEstado;
import com.hellofriend.incidencias.aplicacion.EstadisticasDto.ConteoPorNombre;
import com.hellofriend.incidencias.aplicacion.EstadisticasDto.Destacada;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto.CanalResumen;
import com.hellofriend.incidencias.dominio.EstadoIncidencia;
import com.hellofriend.incidencias.dominio.Prioridad;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio;
import com.hellofriend.incidencias.infraestructura.IncidenciaRepositorio.FilaIncidencia;

/**
 * Lo que se lee de incidencias: el feed y las listas de estudiantes y profesores, la bandeja del director, sus
 * estadísticas y los reportes del administrador. No cambia nada.
 */
@Service
@Transactional(readOnly = true)
public class ConsultasIncidencias {

	private final IncidenciaRepositorio repositorio;
	private final VistaIncidencia vista;
	private final ServicioCanales canales;

	public ConsultasIncidencias(IncidenciaRepositorio repositorio, VistaIncidencia vista, ServicioCanales canales) {
		this.repositorio = repositorio;
		this.vista = vista;
		this.canales = canales;
	}

	// --- catálogos

	public List<String> categorias() {
		return repositorio.categorias();
	}

	public List<ComentarioPredefinidoDto> comentariosPredefinidos() {
		return repositorio.comentariosPredefinidos().stream()
				.map(c -> new ComentarioPredefinidoDto(c.id(), c.texto())).toList();
	}

	public List<MotivoRechazoDto> motivosDeRechazo() {
		return repositorio.motivosDeRechazo().stream().map(m -> new MotivoRechazoDto(m.id(), m.descripcion())).toList();
	}

	// --- estudiantes y profesores

	/** Lo publicado de los canales a los que está suscrita la persona, o solo de {@code canalId} (que debe seguir). */
	public List<IncidenciaDto> feed(UsuarioActual actor, String canalId, String orden, String prioridad) {
		exigirSuscripcion(canalId, actor);
		List<IncidenciaDto> lista = repositorio.visiblesParaSuscrito(actor.id(), canalId).stream()
				.map(f -> vista.de(f, actor, false)).toList();
		return ordenar(lista.stream()
				.filter(i -> prioridad == null || prioridad.isBlank() || prioridad.equals(i.prioridad())).toList(),
				orden);
	}

	/** Hasta cinco incidencias publicadas o en atención con confirmaciones, las más confirmadas primero. */
	public List<IncidenciaDto> destacadas(UsuarioActual actor) {
		return ordenar(repositorio.conConfirmacionesParaSuscrito(actor.id()).stream()
				.map(f -> vista.de(f, actor, false)).toList(), "prioridad").stream().limit(5).toList();
	}

	public List<IncidenciaDto> porCanal(UsuarioActual actor, String canalId, String orden) {
		exigirSuscripcion(canalId, actor);
		return ordenar(repositorio.visiblesDeCanal(canalId).stream().map(f -> vista.de(f, actor, false)).toList(),
				orden);
	}

	/** Las incidencias de la persona en cualquier estado, las más nuevas primero. */
	public List<IncidenciaDto> mias(UsuarioActual actor) {
		return repositorio.deAutor(actor.id()).stream().map(f -> vista.de(f, actor, false)).toList();
	}

	/**
	 * Detalle de una incidencia. Una que no es visible solo la ven su autora y el director (para los demás no existe);
	 * una visible exige estar suscrito al canal. El administrador usa el reporte.
	 */
	public IncidenciaDto detalle(UsuarioActual actor, String id) {
		FilaIncidencia i = buscar(id);
		boolean mia = actor.id().equals(i.autorId());
		boolean director = actor.esDirector();
		if (actor.esAdministrador()) {
			throw ExcepcionNegocio.prohibido("El administrador consulta incidencias desde la auditoría.");
		}
		if (!i.estado().visible() && !mia && !director) {
			throw ExcepcionNegocio.noEncontrado("La incidencia no existe.");
		}
		if (i.estado().visible() && !mia && !director && !canales.estaSuscrito(i.canalId(), actor.id())) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para ver esta incidencia.");
		}
		return vista.de(i, actor, true);
	}

	// --- director

	/**
	 * Las incidencias en un estado (el nombre de la API vale: PUBLICADA es APROBADA). Pendientes: las viejas primero.
	 */
	public List<IncidenciaDto> deDirector(UsuarioActual actor, String estado) {
		EstadoIncidencia buscado = EstadoIncidencia.deApi(estado)
				.orElseThrow(() -> ExcepcionNegocio.invalido("Estado desconocido."));
		return ordenar(repositorio.enEstado(buscado).stream().map(f -> vista.de(f, actor, false)).toList(),
				buscado == EstadoIncidencia.PENDIENTE ? "antiguas" : "prioridad");
	}

	public EstadisticasDto estadisticas(String desde, String hasta, String periodo) {
		LocalDate inicio = fecha(desde);
		LocalDate fin = fecha(hasta);
		if (inicio != null && fin != null && inicio.isAfter(fin)) {
			throw ExcepcionNegocio.invalido("La fecha inicial no puede ser posterior a la final.");
		}
		List<FilaIncidencia> filas = repositorio.todas().stream().filter(f -> enRango(f, inicio, fin))
				.filter(f -> periodo == null || periodo.isBlank() || periodo.equals(f.anio() + "-" + f.semestre()))
				.toList();

		Map<String, Long> cursos = new LinkedHashMap<>();
		Map<String, Long> categorias = new LinkedHashMap<>();
		Map<String, Long> estados = new LinkedHashMap<>();
		repositorio.categorias().forEach(nombre -> categorias.put(nombre, 0L));
		for (String estado : List.of("PENDIENTE", "OBSERVADA", "PUBLICADA", "EN_ATENCION", "RESUELTA", "RECHAZADA")) {
			estados.put(estado, 0L);
		}
		for (FilaIncidencia f : filas) {
			cursos.merge(nombreDelCanal(f), 1L, Long::sum);
			categorias.merge(f.categoria(), 1L, Long::sum);
			estados.merge(f.estado().api(), 1L, Long::sum);
		}
		List<Double> horas = filas.stream().filter(f -> f.resueltaEn() != null && f.aprobadaEn() != null)
				.map(f -> Duration.between(f.aprobadaEn(), f.resueltaEn()).toMinutes() / 60.0).toList();
		List<Destacada> masConfirmadas = filas.stream().filter(f -> f.estado().visible() && f.totalConfirmaciones() > 0)
				.sorted(Comparator.comparingInt(FilaIncidencia::totalConfirmaciones)
						.thenComparing(FilaIncidencia::creadoEn).reversed())
				.limit(5).map(f -> new Destacada(f.id(), f.titulo(), f.estado().api(), nombreDelCanal(f),
						f.totalConfirmaciones(), Prioridad.de(f.totalConfirmaciones()).name()))
				.toList();
		return new EstadisticasDto(filas.size(), horas.size(),
				horas.isEmpty() ? null : horas.stream().mapToDouble(Double::doubleValue).average().orElse(0),
				porNombre(cursos), porNombre(categorias), porEstado(estados), masConfirmadas);
	}

	// --- administrador

	/**
	 * Lista de reportes, los más nuevos primero. {@code usuario} coincide con el autor, un confirmante o quien cambió
	 * algo.
	 */
	public List<ReporteFilaDto> reportes(String desde, String hasta, String cursoId, String estado, String usuario) {
		LocalDate inicio = fecha(desde);
		LocalDate fin = fecha(hasta);
		String tecnico = "PUBLICADA".equals(estado) ? "APROBADA" : estado;
		return repositorio.todas().stream().filter(f -> enRango(f, inicio, fin))
				.filter(f -> cursoId == null || cursoId.isBlank() || cursoId.equals(f.cursoId()))
				.filter(f -> tecnico == null || tecnico.isBlank() || tecnico.equals(f.estado().name()))
				.filter(f -> involucrado(f, usuario)).sorted(Comparator.comparing(FilaIncidencia::creadoEn).reversed())
				.map(f -> new ReporteFilaDto(f.id(), f.titulo(), f.categoria(), f.estado().api(), f.creadoEn(),
						canal(f), f.totalConfirmaciones(), Prioridad.de(f.totalConfirmaciones()).name(),
						f.autorUsuario(), repositorio.versiones(f.id()).size()))
				.toList();
	}

	/** Historial completo de una incidencia, con el autor y su correo y quién confirmó. */
	public IncidenciaDto reporte(UsuarioActual administrador, String id) {
		return vista.deReporte(buscar(id), administrador);
	}

	// --- internos

	private FilaIncidencia buscar(String id) {
		return repositorio.porId(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La incidencia no existe."));
	}

	private void exigirSuscripcion(String canalId, UsuarioActual actor) {
		if (canalId != null && !canales.estaSuscrito(canalId, actor.id())) {
			throw ExcepcionNegocio.prohibido("Suscríbete al canal para ver sus incidencias.");
		}
	}

	private boolean involucrado(FilaIncidencia f, String busqueda) {
		if (busqueda == null || busqueda.isBlank()) {
			return true;
		}
		String texto = busqueda.strip().toLowerCase();
		return f.autorUsuario().toLowerCase().contains(texto) || repositorio.usuariosInvolucrados(f.id()).stream()
				.anyMatch(nombre -> nombre.toLowerCase().contains(texto));
	}

	private static CanalResumen canal(FilaIncidencia f) {
		return new CanalResumen(f.canalId(), nombreDelCanal(f), f.anio() + "-" + f.semestre());
	}

	private static String nombreDelCanal(FilaIncidencia f) {
		return f.cursoCodigo() + " · " + f.cursoNombre();
	}

	private static boolean enRango(FilaIncidencia f, LocalDate inicio, LocalDate fin) {
		LocalDate dia = f.creadoEn().toLocalDate();
		return (inicio == null || !dia.isBefore(inicio)) && (fin == null || !dia.isAfter(fin));
	}

	private static LocalDate fecha(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(valor);
		} catch (DateTimeParseException e) {
			throw ExcepcionNegocio.invalido("La fecha debe tener el formato AAAA-MM-DD.");
		}
	}

	private static List<ConteoPorNombre> porNombre(Map<String, Long> valores) {
		return valores.entrySet().stream().map(e -> new ConteoPorNombre(e.getKey(), e.getValue()))
				.sorted(Comparator.comparing(ConteoPorNombre::total).reversed()).toList();
	}

	private static List<ConteoPorEstado> porEstado(Map<String, Long> valores) {
		return valores.entrySet().stream().map(e -> new ConteoPorEstado(e.getKey(), e.getValue()))
				.sorted(Comparator.comparing(ConteoPorEstado::total).reversed()).toList();
	}

	/**
	 * "prioridad": más confirmadas primero (y entre iguales, las más nuevas); "antiguas"; otro valor: las más nuevas.
	 */
	private static List<IncidenciaDto> ordenar(List<IncidenciaDto> lista, String orden) {
		Comparator<IncidenciaDto> recientes = Comparator.comparing(IncidenciaDto::fechaCreacion);
		Comparator<IncidenciaDto> comparador = switch (orden) {
			case "prioridad" -> Comparator.comparingInt(IncidenciaDto::confirmaciones).thenComparing(recientes)
					.reversed();
			case "antiguas" -> recientes;
			default -> recientes.reversed();
		};
		return lista.stream().sorted(comparador).toList();
	}

}

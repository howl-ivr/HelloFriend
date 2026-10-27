package com.hellofriend.incidencias.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hellofriend.compartido.seguridad.UsuarioActual;
import com.hellofriend.incidencias.aplicacion.ComentarioPredefinidoDto;
import com.hellofriend.incidencias.aplicacion.ConfirmacionDto;
import com.hellofriend.incidencias.aplicacion.ConsultasIncidencias;
import com.hellofriend.incidencias.aplicacion.ContenidoIncidencia;
import com.hellofriend.incidencias.aplicacion.EstadisticasDto;
import com.hellofriend.incidencias.aplicacion.IncidenciaDto;
import com.hellofriend.incidencias.aplicacion.MotivoRechazoDto;
import com.hellofriend.incidencias.aplicacion.ReaccionDto;
import com.hellofriend.incidencias.aplicacion.ReporteFilaDto;
import com.hellofriend.incidencias.aplicacion.ServicioIncidencias;
import com.hellofriend.incidencias.dominio.ComentarioIncidencia;

/**
 * Incidencias (docs/API.md): estudiantes y profesores las consultan; los estudiantes las registran, editan, confirman,
 * reaccionan y comentan; el director las revisa; el administrador consulta los reportes. Solo traduce HTTP: las
 * reglas están en {@link ServicioIncidencias} y {@link ConsultasIncidencias}.
 */
@RestController
public class IncidenciasController {

	private final ServicioIncidencias servicio;
	private final ConsultasIncidencias consultas;

	public IncidenciasController(ServicioIncidencias servicio, ConsultasIncidencias consultas) {
		this.servicio = servicio;
		this.consultas = consultas;
	}

	// --- catálogos

	@GetMapping("/categorias")
	public List<String> categorias() {
		return consultas.categorias();
	}

	@GetMapping("/comentarios-predefinidos")
	public List<ComentarioPredefinidoDto> comentariosPredefinidos() {
		return consultas.comentariosPredefinidos();
	}

	@GetMapping("/motivos-rechazo")
	@PreAuthorize("hasRole('DIRECTOR')")
	public List<MotivoRechazoDto> motivos() {
		return consultas.motivosDeRechazo();
	}

	// --- consulta de estudiantes y profesores

	@GetMapping("/feed")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<IncidenciaDto> feed(@RequestParam(required = false) String canalId,
			@RequestParam(defaultValue = "recientes") String orden, @RequestParam(required = false) String prioridad,
			Authentication auth) {
		return consultas.feed(UsuarioActual.de(auth), canalId, orden, prioridad);
	}

	@GetMapping("/feed/destacadas")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<IncidenciaDto> destacadas(Authentication auth) {
		return consultas.destacadas(UsuarioActual.de(auth));
	}

	@GetMapping("/canales/{canalId}/incidencias")
	@PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR')")
	public List<IncidenciaDto> porCanal(@PathVariable String canalId,
			@RequestParam(defaultValue = "prioridad") String orden, Authentication auth) {
		return consultas.porCanal(UsuarioActual.de(auth), canalId, orden);
	}

	@GetMapping("/mis-incidencias")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public List<IncidenciaDto> mias(Authentication auth) {
		return consultas.mias(UsuarioActual.de(auth));
	}

	/** Cualquier rol con sesión; qué ve cada uno lo decide el servicio. */
	@GetMapping("/incidencias/{id}")
	public IncidenciaDto detalle(@PathVariable String id, Authentication auth) {
		return consultas.detalle(UsuarioActual.de(auth), id);
	}

	// --- estudiante

	@PostMapping("/incidencias")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public IncidenciaDto crear(@RequestBody ContenidoIncidencia body, Authentication auth) {
		return servicio.crear(UsuarioActual.de(auth), body);
	}

	@PutMapping("/incidencias/{id}")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public IncidenciaDto editar(@PathVariable String id, @RequestBody ContenidoIncidencia body,
			Authentication auth) {
		return servicio.editar(UsuarioActual.de(auth), id, body);
	}

	@PostMapping("/incidencias/{id}/reenviar")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public IncidenciaDto reenviar(@PathVariable String id, Authentication auth) {
		return servicio.reenviar(UsuarioActual.de(auth), id);
	}

	@PostMapping("/incidencias/{id}/confirmacion")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public ConfirmacionDto confirmar(@PathVariable String id, Authentication auth) {
		return servicio.confirmar(UsuarioActual.de(auth), id);
	}

	@PutMapping("/incidencias/{id}/reaccion")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public ReaccionDto reaccionar(@PathVariable String id, @RequestBody Map<String, String> body,
			Authentication auth) {
		return servicio.reaccionar(UsuarioActual.de(auth), id, body.get("tipo"));
	}

	@PostMapping("/incidencias/{id}/comentarios")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ESTUDIANTE')")
	public ComentarioIncidencia comentar(@PathVariable String id, @RequestBody Map<String, String> body,
			Authentication auth) {
		return servicio.comentar(UsuarioActual.de(auth), id, body.get("comentarioPredefinidoId"));
	}

	// --- director

	@GetMapping("/director/incidencias")
	@PreAuthorize("hasRole('DIRECTOR')")
	public List<IncidenciaDto> director(@RequestParam(defaultValue = "PENDIENTE") String estado,
			Authentication auth) {
		return consultas.deDirector(UsuarioActual.de(auth), estado);
	}

	/** {accion}: aprobar, observar, rechazar, atender o resolver. */
	@PostMapping("/director/incidencias/{id}/{accion}")
	@PreAuthorize("hasRole('DIRECTOR')")
	public IncidenciaDto decidir(@PathVariable String id, @PathVariable String accion,
			@RequestBody(required = false) Map<String, String> cuerpo, Authentication auth) {
		return servicio.decidir(UsuarioActual.de(auth), id, accion, cuerpo);
	}

	@GetMapping("/director/estadisticas")
	@PreAuthorize("hasRole('DIRECTOR')")
	public EstadisticasDto estadisticas(@RequestParam(required = false) String desde,
			@RequestParam(required = false) String hasta, @RequestParam(required = false) String periodo) {
		return consultas.estadisticas(desde, hasta, periodo);
	}

	// --- administrador

	@GetMapping("/admin/reportes")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public List<ReporteFilaDto> reportes(@RequestParam(required = false) String desde,
			@RequestParam(required = false) String hasta, @RequestParam(required = false) String cursoId,
			@RequestParam(required = false) String estado, @RequestParam(required = false) String usuario) {
		return consultas.reportes(desde, hasta, cursoId, estado, usuario);
	}

	@GetMapping("/admin/reportes/{id}")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public IncidenciaDto reporte(@PathVariable String id, Authentication auth) {
		return consultas.reporte(UsuarioActual.de(auth), id);
	}

}

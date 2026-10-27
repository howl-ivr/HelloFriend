package com.hellofriend.compartido.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte los errores en JSON {@code {"mensaje": "..."}} en espanol, que es el formato que espera el frontend
 * (docs/API.md). Los datos mal formados responden 400, nunca 500.
 */
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(ExcepcionNegocio.class)
	public ResponseEntity<Map<String, String>> negocio(ExcepcionNegocio e) {
		return respuesta(e.getEstado().value(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
		String mensaje = e.getBindingResult().getFieldErrors().stream().map(error -> error.getDefaultMessage())
				.sorted().findFirst().orElse("Los datos enviados no son válidos.");
		return respuesta(400, mensaje);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class})
	public ResponseEntity<Map<String, String>> malFormado(Exception e) {
		return respuesta(400, "La solicitud está mal formada.");
	}

	private ResponseEntity<Map<String, String>> respuesta(int estado, String mensaje) {
		return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
	}

}

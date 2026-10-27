package com.hellofriend.compartido.web;

import org.springframework.http.HttpStatus;

/**
 * Error esperado de negocio con el codigo HTTP que debe recibir el cliente. Los servicios lo lanzan con
 * {@link #invalido}, {@link #noEncontrado} o {@link #conflicto}; {@link ManejadorErrores} lo convierte en la respuesta.
 */
public class ExcepcionNegocio extends RuntimeException {

	private final HttpStatus estado;

	private ExcepcionNegocio(HttpStatus estado, String mensaje) {
		super(mensaje);
		this.estado = estado;
	}

	/** 400: dato invalido. */
	public static ExcepcionNegocio invalido(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.BAD_REQUEST, mensaje);
	}

	/** 401: no se pudo comprobar quién es el usuario (credenciales incorrectas, sesión inválida). */
	public static ExcepcionNegocio noAutorizado(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, mensaje);
	}

	/** 404: el recurso no existe. */
	public static ExcepcionNegocio noEncontrado(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.NOT_FOUND, mensaje);
	}

	/** 409: la operacion choca con el estado actual (duplicado, recurso en uso). */
	public static ExcepcionNegocio conflicto(String mensaje) {
		return new ExcepcionNegocio(HttpStatus.CONFLICT, mensaje);
	}

	public HttpStatus getEstado() {
		return estado;
	}

}

package com.hellofriend.incidencias.aplicacion;

/**
 * Contenido de una incidencia al registrarla o editarla. {@code canalId} solo se usa al registrar (no se cambia de
 * canal al editar). Las reglas de largo se validan en el servicio, con el mensaje que espera el frontend.
 */
public record ContenidoIncidencia(String canalId, String categoria, String titulo, String descripcion) {
}

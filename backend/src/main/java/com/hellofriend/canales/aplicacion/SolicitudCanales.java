package com.hellofriend.canales.aplicacion;

import java.util.List;

/** Alta de canales: un canal por cada curso para el periodo indicado (por ejemplo "2026-2"). */
public record SolicitudCanales(String periodo, List<String> cursoIds) {
}

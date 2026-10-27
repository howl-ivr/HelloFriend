package com.hellofriend.incidencias.aplicacion;

import java.util.Map;

/** Reacciones de una incidencia y la de la persona (null si no reaccionó o la retiró). */
public record ReaccionDto(Map<String, Integer> reacciones, String miReaccion) {
}

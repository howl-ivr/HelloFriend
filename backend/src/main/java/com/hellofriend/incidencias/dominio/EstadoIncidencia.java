package com.hellofriend.incidencias.dominio;

import java.util.Optional;

/**
 * Estados de una incidencia (CHECK de {@code incidencias_incidencia}). En la API, {@code APROBADA} se llama
 * {@code PUBLICADA} (docs/API.md); la base conserva el nombre técnico.
 */
public enum EstadoIncidencia {

	PENDIENTE, OBSERVADA, APROBADA, RECHAZADA, EN_ATENCION, RESUELTA;

	/** Los suscritos al canal pueden verla: publicada, en atención o resuelta. */
	public boolean visible() {
		return this == APROBADA || this == EN_ATENCION || this == RESUELTA;
	}

	/** Se puede confirmar, reaccionar y comentar. */
	public boolean interactuable() {
		return this == APROBADA || this == EN_ATENCION;
	}

	/** El autor todavía puede editarla. */
	public boolean editable() {
		return this == PENDIENTE || this == OBSERVADA;
	}

	/** El nombre que ve el frontend. */
	public String api() {
		return this == APROBADA ? "PUBLICADA" : name();
	}

	/** Entiende el nombre de la API ({@code PUBLICADA}) y el de la base; devuelve vacío si no existe. */
	public static Optional<EstadoIncidencia> deApi(String texto) {
		if (texto == null) {
			return Optional.empty();
		}
		String tecnico = "PUBLICADA".equals(texto) ? "APROBADA" : texto;
		for (EstadoIncidencia estado : values()) {
			if (estado.name().equals(tecnico)) {
				return Optional.of(estado);
			}
		}
		return Optional.empty();
	}

}

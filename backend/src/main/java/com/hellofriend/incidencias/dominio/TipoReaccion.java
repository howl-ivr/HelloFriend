package com.hellofriend.incidencias.dominio;

/** Reacciones posibles a una incidencia publicada. Coinciden con el CHECK de {@code incidencias_reaccion} (V4). */
public enum TipoReaccion {

	APOYO, PREOCUPANTE, URGENTE, LO_SIGO;

	public static boolean existe(String texto) {
		for (TipoReaccion tipo : values()) {
			if (tipo.name().equals(texto)) {
				return true;
			}
		}
		return false;
	}

}

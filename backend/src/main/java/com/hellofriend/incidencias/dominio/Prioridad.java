package com.hellofriend.incidencias.dominio;

/** Prioridad según las confirmaciones: 0 o 1 es BAJA, de 2 a 4 MEDIA y 5 o más ALTA. No se guarda: se calcula. */
public enum Prioridad {

	BAJA, MEDIA, ALTA;

	public static Prioridad de(int confirmaciones) {
		return confirmaciones >= 5 ? ALTA : confirmaciones >= 2 ? MEDIA : BAJA;
	}

}

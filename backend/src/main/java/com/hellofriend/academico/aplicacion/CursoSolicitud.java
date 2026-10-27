package com.hellofriend.academico.aplicacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear o modificar un curso. El identificador y la fecha de creacion no se reciben nunca. */
public record CursoSolicitud(
		@NotBlank(message = "El código es obligatorio.") @Pattern(regexp = "^[A-Za-z0-9]{3,10}$", message = "El código debe tener de 3 a 10 letras o números.") String codigo,
		@NotBlank(message = "El nombre es obligatorio.") @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.") String nombre,
		@Min(value = 1, message = "El ciclo debe estar entre 1 y 10.") @Max(value = 10, message = "El ciclo debe estar entre 1 y 10.") Integer ciclo) {
}

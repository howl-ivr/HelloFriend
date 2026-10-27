package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;

/** Pedido de un enlace de recuperación. El formato del correo no se valida: la respuesta es la misma exista o no. */
public record SolicitudRecuperacion(@NotBlank(message = "Ingresa tu correo.") String correo) {
}

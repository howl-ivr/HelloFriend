package com.hellofriend.usuarios.aplicacion;

import jakarta.validation.constraints.NotNull;

import com.hellofriend.usuarios.dominio.Rol;

/** Rol nuevo de un usuario. Un valor que no sea un rol responde 400 como cualquier JSON mal formado. */
public record SolicitudCambioRol(@NotNull(message = "Indica el rol.") Rol rol) {
}

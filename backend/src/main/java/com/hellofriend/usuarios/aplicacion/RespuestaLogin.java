package com.hellofriend.usuarios.aplicacion;

import com.hellofriend.usuarios.dominio.Rol;

/** Respuesta del login: el JWT y los datos que el frontend muestra. Nunca incluye el correo ni el hash. */
public record RespuestaLogin(String token, String usuario, Rol rol) {
}

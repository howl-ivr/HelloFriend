package com.hellofriend.usuarios.dominio;

/**
 * Roles de la plataforma. Los nombres coinciden con el CHECK de usuarios_usuario (V1) y con el claim "rol" del token.
 */
public enum Rol {

	ESTUDIANTE, PROFESOR, DIRECTOR, ADMINISTRADOR

}

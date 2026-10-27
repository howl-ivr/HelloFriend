package com.hellofriend.compartido.seguridad;

/**
 * Decide si un token todavía sirve. Se consulta en cada petición: así una cuenta desactivada, o una sesión cerrada,
 * pierde el acceso de inmediato aunque el token no haya vencido. La implementación vive en el módulo de usuarios, para
 * que este paquete no dependa de sus entidades.
 */
public interface VerificadorSesion {

	/** {@code version} es el valor del claim "ver" del token; debe ser igual al {@code version_sesion} guardado. */
	boolean sesionVigente(String usuarioId, int version);

}

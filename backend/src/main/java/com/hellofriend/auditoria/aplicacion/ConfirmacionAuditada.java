package com.hellofriend.auditoria.aplicacion;

import java.time.LocalDateTime;

/** Una confirmación de una incidencia, o su retiro, con quién la hizo: solo la ve el administrador. */
public record ConfirmacionAuditada(boolean retirada, LocalDateTime fecha, String usuario) {
}

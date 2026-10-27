package com.hellofriend.notificaciones.infraestructura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hellofriend.notificaciones.dominio.Notificacion;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, String> {

	/** La bandeja de una persona, las más nuevas primero. */
	List<Notificacion> findByDestinatarioIdOrderByCreadaEnDescIdDesc(String destinatarioId);

	/** Solo si es de esa persona: una ajena se trata como inexistente. */
	Optional<Notificacion> findByIdAndDestinatarioId(String id, String destinatarioId);

	/** Marca de una vez todas las no leídas de una persona; devuelve cuántas cambió. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Notificacion n set n.leidaEn = :ahora where n.destinatarioId = :destinatarioId and n.leidaEn is null")
	int marcarTodasLeidas(@Param("destinatarioId") String destinatarioId, @Param("ahora") LocalDateTime ahora);

}

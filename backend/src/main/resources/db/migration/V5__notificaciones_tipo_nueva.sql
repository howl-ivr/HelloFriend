-- V5 · notificaciones: agrega el tipo NUEVA, el aviso que reciben los directores cuando un estudiante registra una
-- incidencia (docs/API.md: "POST /incidencias ... notifica a directores"). El CHECK de V2 no lo admitia.
-- V1 a V4 no se modifican: se vuelve a crear la restriccion con el tipo nuevo.
ALTER TABLE notificaciones_notificacion DROP CHECK ck_notificaciones_notificacion_tipo;

ALTER TABLE notificaciones_notificacion
    ADD CONSTRAINT ck_notificaciones_notificacion_tipo CHECK (tipo IN
        ('NUEVA', 'APROBADA', 'OBSERVADA', 'RECHAZADA', 'REENVIADA', 'EN_ATENCION', 'RESUELTA'));

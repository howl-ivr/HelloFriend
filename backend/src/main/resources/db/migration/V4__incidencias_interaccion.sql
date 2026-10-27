-- Catálogos e interacciones que consume la aplicación web. V1-V3 no se modifican.
CREATE TABLE incidencias_motivo_rechazo (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    descripcion VARCHAR(200) NOT NULL,
    CONSTRAINT pk_incidencias_motivo_rechazo PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_motivo_rechazo_descripcion UNIQUE (descripcion)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO incidencias_motivo_rechazo (descripcion) VALUES
    ('Contiene ataques personales o lenguaje ofensivo'),
    ('Falta información para evaluarla'),
    ('No corresponde a una incidencia del curso'),
    ('Duplicada: ya existe una incidencia similar'),
    ('Otro motivo (ver comentario)');

CREATE TABLE incidencias_comentario_predefinido (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    texto VARCHAR(200) NOT NULL,
    CONSTRAINT pk_incidencias_comentario_predefinido PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_comentario_predefinido_texto UNIQUE (texto)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO incidencias_comentario_predefinido (texto) VALUES
    ('Me pasó lo mismo'), ('Confirmo, estoy en el mismo horario'),
    ('Sucedió otra vez esta semana'), ('Ya lo comenté con el delegado'),
    ('Se solucionó en mi caso');

CREATE TABLE incidencias_comentario (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    incidencia_id CHAR(36) NOT NULL,
    usuario_id CHAR(36) NOT NULL,
    predefinido_id CHAR(36) NOT NULL,
    creada_en DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_comentario PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_comentario_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_comentario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios_usuario (id),
    CONSTRAINT fk_incidencias_comentario_predefinido FOREIGN KEY (predefinido_id) REFERENCES incidencias_comentario_predefinido (id),
    INDEX ix_incidencias_comentario_incidencia (incidencia_id, creada_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE incidencias_reaccion (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    incidencia_id CHAR(36) NOT NULL,
    usuario_id CHAR(36) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    CONSTRAINT pk_incidencias_reaccion PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_reaccion_incidencia_usuario UNIQUE (incidencia_id, usuario_id),
    CONSTRAINT fk_incidencias_reaccion_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_reaccion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios_usuario (id),
    CONSTRAINT ck_incidencias_reaccion_tipo CHECK (tipo IN ('APOYO', 'PREOCUPANTE', 'URGENTE', 'LO_SIGO')),
    INDEX ix_incidencias_reaccion_incidencia (incidencia_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE incidencias_incidencia
    ADD COLUMN revision_comentario VARCHAR(500) NULL,
    ADD COLUMN revision_motivo_id CHAR(36) NULL,
    ADD CONSTRAINT fk_incidencias_incidencia_revision_motivo FOREIGN KEY (revision_motivo_id) REFERENCES incidencias_motivo_rechazo (id);

ALTER TABLE incidencias_historial_estado
    ADD COLUMN tipo_evento VARCHAR(20) NULL,
    ADD COLUMN detalle VARCHAR(1000) NULL;

CREATE TABLE auditoria_evento (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    accion VARCHAR(40) NOT NULL,
    actor_id CHAR(36) NULL,
    incidencia_id CHAR(36) NULL,
    detalle VARCHAR(1000) NULL,
    fecha DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_auditoria_evento PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_evento_actor FOREIGN KEY (actor_id) REFERENCES usuarios_usuario (id),
    CONSTRAINT fk_auditoria_evento_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    INDEX ix_auditoria_evento_fecha (fecha DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO auditoria_evento (id, accion, actor_id, incidencia_id, fecha)
    SELECT UUID(), 'INCIDENCIA_CONFIRMADA', estudiante_id, incidencia_id, creada_en
    FROM incidencias_confirmacion;

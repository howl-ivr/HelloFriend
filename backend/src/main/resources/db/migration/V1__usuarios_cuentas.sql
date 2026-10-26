-- V1 · usuarios: tabla de cuentas. Migracion ya subida = nunca se edita; los cambios van en una nueva.
-- Convenciones: prefijo {modulo}_{entidad}, id CHAR(36) con UUID(), estados VARCHAR + CHECK en MAYUSCULAS,
-- fechas DATETIME(6) en UTC, utf8mb4 (ver docs/guias/base-de-datos.md).
CREATE TABLE usuarios_usuario (
    id              CHAR(36)     NOT NULL DEFAULT (UUID()),
    correo          VARCHAR(255) NOT NULL,
    usuario         VARCHAR(15)  NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_usuarios_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_usuario_correo UNIQUE (correo),
    CONSTRAINT uq_usuarios_usuario_usuario UNIQUE (usuario),
    CONSTRAINT ck_usuarios_usuario_rol CHECK (rol IN ('ESTUDIANTE', 'PROFESOR', 'DIRECTOR', 'ADMINISTRADOR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

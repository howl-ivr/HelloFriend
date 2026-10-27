-- V2 · modelo de incidencias (DDL + categorias de referencia)

-- usuarios: cierre de sesion y recuperacion de contrasena dentro de usuarios_usuario (sin tablas aparte).
-- version_sesion: el token (JWT) lleva este numero; al cerrar sesion se suma 1 y los tokens anteriores dejan de valer.
--   En cada peticion se carga el usuario y se exige activo = TRUE y version_sesion igual a la del token (RF-02.3, RF-03.1).
-- token_recuperacion_*: hash del enlace vigente (nunca el token en claro) y su vencimiento (HU-04). Pedir otro enlace
--   sobrescribe el anterior; al usarlo se dejan en NULL (un solo uso). UNIQUE admite varios NULL en MySQL.
ALTER TABLE usuarios_usuario
    ADD COLUMN version_sesion INT NOT NULL DEFAULT 0,
    ADD COLUMN token_recuperacion_hash VARCHAR(100) NULL,
    ADD COLUMN token_recuperacion_expira_en DATETIME(6) NULL,
    ADD CONSTRAINT uq_usuarios_usuario_token_recuperacion_hash UNIQUE (token_recuperacion_hash);

-- academico: catalogo permanente de cursos. Se registran al crear canales (RF-06.2).
-- No hay tabla de periodos: el periodo (por ejemplo 2026-2) son las columnas anio y semestre de canales_canal.
CREATE TABLE academico_curso (
    id        CHAR(36)     NOT NULL DEFAULT (UUID()),
    codigo    VARCHAR(20)  NOT NULL,
    nombre    VARCHAR(150) NOT NULL,
    creado_en DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_academico_curso PRIMARY KEY (id),
    CONSTRAINT uq_academico_curso_codigo UNIQUE (codigo),
    CONSTRAINT ck_academico_curso_nombre CHECK (TRIM(nombre) <> '')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- canales: un canal es un curso en un periodo (por ejemplo 1INF36 en 2026-2). Unico por curso y periodo (RF-06.5).
-- estado ACTIVO/DESACTIVADO lo cambia el administrador (RF-06.6). Un canal desactivado no admite incidencias ni
-- suscripciones nuevas, pero conserva las existentes.
CREATE TABLE canales_canal (
    id             CHAR(36)    NOT NULL DEFAULT (UUID()),
    curso_id       CHAR(36)    NOT NULL,
    anio           SMALLINT    NOT NULL,
    semestre       TINYINT     NOT NULL,
    estado         VARCHAR(12) NOT NULL DEFAULT 'ACTIVO',
    creado_por     CHAR(36)    NOT NULL,
    creado_en      DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    desactivado_en DATETIME(6) NULL,
    CONSTRAINT pk_canales_canal PRIMARY KEY (id),
    CONSTRAINT uq_canales_canal_curso_anio_semestre UNIQUE (curso_id, anio, semestre),
    CONSTRAINT fk_canales_canal_curso_id FOREIGN KEY (curso_id) REFERENCES academico_curso (id),
    CONSTRAINT fk_canales_canal_creado_por FOREIGN KEY (creado_por) REFERENCES usuarios_usuario (id),
    CONSTRAINT ck_canales_canal_anio CHECK (anio BETWEEN 2000 AND 2100),
    -- Por decidir: incluir el ciclo de verano (0)?
    CONSTRAINT ck_canales_canal_semestre CHECK (semestre IN (1, 2)),
    CONSTRAINT ck_canales_canal_estado CHECK (estado IN ('ACTIVO', 'DESACTIVADO')),
    CONSTRAINT ck_canales_canal_desactivado_en CHECK ((estado = 'DESACTIVADO') = (desactivado_en IS NOT NULL)),
    INDEX ix_canales_canal_anio_semestre (anio, semestre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- canales: suscripcion de un usuario (estudiante o profesor) a un canal. Una por usuario y canal (RF-07.1).
-- Cancelar la suscripcion elimina la fila (RF-07.2).
CREATE TABLE canales_suscripcion (
    id         CHAR(36)    NOT NULL DEFAULT (UUID()),
    canal_id   CHAR(36)    NOT NULL,
    usuario_id CHAR(36)    NOT NULL,
    creada_en  DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_canales_suscripcion PRIMARY KEY (id),
    CONSTRAINT uq_canales_suscripcion_canal_usuario UNIQUE (canal_id, usuario_id),
    CONSTRAINT fk_canales_suscripcion_canal_id FOREIGN KEY (canal_id) REFERENCES canales_canal (id),
    CONSTRAINT fk_canales_suscripcion_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios_usuario (id),
    INDEX ix_canales_suscripcion_usuario_id (usuario_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- incidencias: catalogo de categorias (RF-08.2). Las HU no piden gestionarlo, por eso se carga aqui
-- (datos de referencia que tambien necesita produccion).
CREATE TABLE incidencias_categoria (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_incidencias_categoria PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO incidencias_categoria (id, nombre, descripcion) VALUES
    ('00000005-0000-0000-0000-000000000001', 'Evaluaciones y calificaciones',
        'Notas, criterios de evaluación, plazos de publicación o revisión de exámenes.'),
    ('00000005-0000-0000-0000-000000000002', 'Cumplimiento del sílabo',
        'Contenidos, cronograma, material o actividades que no siguen lo anunciado.'),
    ('00000005-0000-0000-0000-000000000003', 'Metodología de enseñanza',
        'Forma de dictar las clases, ritmo, claridad de las explicaciones o carga de trabajo.'),
    ('00000005-0000-0000-0000-000000000004', 'Trato y comunicación',
        'Clima en el aula, respeto, canales de atención y respuesta a consultas.'),
    ('00000005-0000-0000-0000-000000000005', 'Infraestructura y recursos',
        'Aulas, laboratorios, equipos, plataforma virtual y otros recursos del curso.'),
    ('00000005-0000-0000-0000-000000000006', 'Integridad académica',
        'Plagio, copia u otras situaciones que afectan la honestidad académica.'),
    ('00000005-0000-0000-0000-000000000007', 'Accesibilidad e inclusión',
        'Barreras que dificultan la participación de algún estudiante en el curso.'),
    ('00000005-0000-0000-0000-000000000008', 'Otros',
        'Cualquier situación que no encaje en las categorías anteriores.');

-- incidencias: estado actual de un reporte. El historial del contenido vive en incidencias_version.
-- Transiciones (las valida el dominio, no la base de datos):
--   PENDIENTE -> OBSERVADA | APROBADA | RECHAZADA ; OBSERVADA -> PENDIENTE ; APROBADA -> EN_ATENCION -> RESUELTA
-- La busqueda de incidencias similares (RF-08.5) se resuelve en el servicio; no se usa FULLTEXT (ver guia de BD).
CREATE TABLE incidencias_incidencia (
    id                   CHAR(36)     NOT NULL DEFAULT (UUID()),
    codigo_seguimiento   VARCHAR(16)  NOT NULL,
    canal_id             CHAR(36)     NOT NULL,
    autor_id             CHAR(36)     NOT NULL,
    categoria_id         CHAR(36)     NOT NULL,
    titulo               VARCHAR(150) NOT NULL,
    descripcion          TEXT         NOT NULL,
    estado               VARCHAR(12)  NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo       TEXT         NULL,
    solucion             TEXT         NULL,
    total_confirmaciones INT          NOT NULL DEFAULT 0,
    aprobada_en          DATETIME(6)  NULL,
    resuelta_en          DATETIME(6)  NULL,
    creado_en            DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    actualizado_en       DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_incidencia PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_incidencia_codigo_seguimiento UNIQUE (codigo_seguimiento),
    CONSTRAINT fk_incidencias_incidencia_canal_id FOREIGN KEY (canal_id) REFERENCES canales_canal (id),
    CONSTRAINT fk_incidencias_incidencia_autor_id FOREIGN KEY (autor_id) REFERENCES usuarios_usuario (id),
    CONSTRAINT fk_incidencias_incidencia_categoria_id FOREIGN KEY (categoria_id) REFERENCES incidencias_categoria (id),
    CONSTRAINT ck_incidencias_incidencia_estado CHECK (estado IN
        ('PENDIENTE', 'OBSERVADA', 'APROBADA', 'RECHAZADA', 'EN_ATENCION', 'RESUELTA')),
    CONSTRAINT ck_incidencias_incidencia_titulo CHECK (TRIM(titulo) <> ''),
    CONSTRAINT ck_incidencias_incidencia_descripcion CHECK (TRIM(descripcion) <> ''),
    CONSTRAINT ck_incidencias_incidencia_total_confirmaciones CHECK (total_confirmaciones >= 0),
    -- motivo_rechazo obligatorio si y solo si RECHAZADA (RF-15.3)
    CONSTRAINT ck_incidencias_incidencia_motivo_rechazo CHECK
        ((estado = 'RECHAZADA') = (motivo_rechazo IS NOT NULL AND TRIM(motivo_rechazo) <> '')),
    -- solucion obligatoria si y solo si RESUELTA (RF-16.2)
    CONSTRAINT ck_incidencias_incidencia_solucion CHECK
        ((estado = 'RESUELTA') = (solucion IS NOT NULL AND TRIM(solucion) <> '')),
    CONSTRAINT ck_incidencias_incidencia_aprobada_en CHECK
        ((estado IN ('APROBADA', 'EN_ATENCION', 'RESUELTA')) = (aprobada_en IS NOT NULL)),
    CONSTRAINT ck_incidencias_incidencia_resuelta_en CHECK
        ((estado = 'RESUELTA') = (resuelta_en IS NOT NULL)),
    INDEX ix_incidencias_incidencia_canal_estado (canal_id, estado),
    INDEX ix_incidencias_incidencia_autor_id (autor_id),
    INDEX ix_incidencias_incidencia_estado_creado_en (estado, creado_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- incidencias: fotografia del contenido en cada registro o edicion (RF-13.3).
-- Solo se agregan filas; el servicio no las modifica ni las borra (MySQL sin triggers, por convencion del repo).
CREATE TABLE incidencias_version (
    id            CHAR(36)     NOT NULL DEFAULT (UUID()),
    incidencia_id CHAR(36)     NOT NULL,
    numero        INT          NOT NULL,
    titulo        VARCHAR(150) NOT NULL,
    descripcion   TEXT         NOT NULL,
    categoria_id  CHAR(36)     NOT NULL,
    creada_en     DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_version PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_version_incidencia_numero UNIQUE (incidencia_id, numero),
    CONSTRAINT fk_incidencias_version_incidencia_id FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_version_categoria_id FOREIGN KEY (categoria_id) REFERENCES incidencias_categoria (id),
    CONSTRAINT ck_incidencias_version_numero CHECK (numero >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- incidencias: alertas del director al observar un reporte (RF-15.2), ligadas a la version observada.
-- Al menos una por cada paso a OBSERVADA: lo valida el servicio.
CREATE TABLE incidencias_observacion (
    id            CHAR(36)    NOT NULL DEFAULT (UUID()),
    incidencia_id CHAR(36)    NOT NULL,
    version_id    CHAR(36)    NOT NULL,
    texto         TEXT        NOT NULL,
    creada_por    CHAR(36)    NOT NULL,
    creada_en     DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_observacion PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_observacion_incidencia_id FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_observacion_version_id FOREIGN KEY (version_id) REFERENCES incidencias_version (id),
    CONSTRAINT fk_incidencias_observacion_creada_por FOREIGN KEY (creada_por) REFERENCES usuarios_usuario (id),
    CONSTRAINT ck_incidencias_observacion_texto CHECK (TRIM(texto) <> ''),
    INDEX ix_incidencias_observacion_incidencia_id (incidencia_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- incidencias: respaldo de un estudiante a una incidencia aprobada (HU-11). Una por estudiante e incidencia.
-- Que no confirme su propia incidencia lo valida el servicio. La identidad solo la ve el administrador (auditoria).
CREATE TABLE incidencias_confirmacion (
    id            CHAR(36)    NOT NULL DEFAULT (UUID()),
    incidencia_id CHAR(36)    NOT NULL,
    estudiante_id CHAR(36)    NOT NULL,
    creada_en     DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_confirmacion PRIMARY KEY (id),
    CONSTRAINT uq_incidencias_confirmacion_incidencia_estudiante UNIQUE (incidencia_id, estudiante_id),
    CONSTRAINT fk_incidencias_confirmacion_incidencia_id FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_confirmacion_estudiante_id FOREIGN KEY (estudiante_id) REFERENCES usuarios_usuario (id),
    INDEX ix_incidencias_confirmacion_estudiante_id (estudiante_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- incidencias: cada cambio de estado con fecha y responsable (RF-12.2, RF-15.4, RF-16.3).
-- estado_anterior es NULL en el registro inicial. Solo se agregan filas.
CREATE TABLE incidencias_historial_estado (
    id              CHAR(36)    NOT NULL DEFAULT (UUID()),
    incidencia_id   CHAR(36)    NOT NULL,
    estado_anterior VARCHAR(12) NULL,
    estado_nuevo    VARCHAR(12) NOT NULL,
    cambiado_por    CHAR(36)    NOT NULL,
    cambiado_en     DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_incidencias_historial_estado PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_historial_estado_incidencia_id FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_incidencias_historial_estado_cambiado_por FOREIGN KEY (cambiado_por) REFERENCES usuarios_usuario (id),
    CONSTRAINT ck_incidencias_historial_estado_anterior CHECK (estado_anterior IS NULL OR estado_anterior IN
        ('PENDIENTE', 'OBSERVADA', 'APROBADA', 'RECHAZADA', 'EN_ATENCION', 'RESUELTA')),
    CONSTRAINT ck_incidencias_historial_estado_nuevo CHECK (estado_nuevo IN
        ('PENDIENTE', 'OBSERVADA', 'APROBADA', 'RECHAZADA', 'EN_ATENCION', 'RESUELTA')),
    INDEX ix_incidencias_historial_estado_incidencia_fecha (incidencia_id, cambiado_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- notificaciones: una fila por destinatario. El contenido nunca incluye el nombre del profesor
-- ni la identidad del autor (RF-17.3). "No leida" = leida_en IS NULL.
CREATE TABLE notificaciones_notificacion (
    id              CHAR(36)     NOT NULL DEFAULT (UUID()),
    destinatario_id CHAR(36)     NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    incidencia_id   CHAR(36)     NULL,
    canal_id        CHAR(36)     NULL,
    titulo          VARCHAR(150) NOT NULL,
    mensaje         VARCHAR(500) NOT NULL,
    leida_en        DATETIME(6)  NULL,
    creada_en       DATETIME(6)  NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    CONSTRAINT pk_notificaciones_notificacion PRIMARY KEY (id),
    CONSTRAINT fk_notificaciones_notificacion_destinatario_id FOREIGN KEY (destinatario_id) REFERENCES usuarios_usuario (id),
    CONSTRAINT fk_notificaciones_notificacion_incidencia_id FOREIGN KEY (incidencia_id) REFERENCES incidencias_incidencia (id),
    CONSTRAINT fk_notificaciones_notificacion_canal_id FOREIGN KEY (canal_id) REFERENCES canales_canal (id),
    CONSTRAINT ck_notificaciones_notificacion_tipo CHECK (tipo IN
        ('APROBADA', 'OBSERVADA', 'RECHAZADA', 'REENVIADA', 'EN_ATENCION', 'RESUELTA')),
    INDEX ix_notificaciones_notificacion_bandeja (destinatario_id, leida_en, creada_en DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- auditoria: linea de tiempo de solo lectura de cada reporte (HU-19), como vista sobre las tablas de
-- incidencias (sin tabla propia). Solo la consulta el administrador; incluye la identidad de quienes actuan y confirman.
CREATE VIEW auditoria_historial_incidencia AS
    SELECT v.incidencia_id                       AS incidencia_id,
           v.creada_en                           AS ocurrido_en,
           CAST('VERSION' AS CHAR(15))           AS tipo_evento,
           i.autor_id                            AS actor_id,
           CAST(CONCAT('Versión ', v.numero) AS CHAR(600)) AS detalle
      FROM incidencias_version v
      JOIN incidencias_incidencia i ON i.id = v.incidencia_id
    UNION ALL
    SELECT o.incidencia_id, o.creada_en, CAST('OBSERVACION' AS CHAR(15)), o.creada_por, CAST(o.texto AS CHAR(600))
      FROM incidencias_observacion o
    UNION ALL
    SELECT c.incidencia_id, c.creada_en, CAST('CONFIRMACION' AS CHAR(15)), c.estudiante_id, CAST('Confirmación' AS CHAR(600))
      FROM incidencias_confirmacion c
    UNION ALL
    SELECT h.incidencia_id, h.cambiado_en, CAST('CAMBIO_ESTADO' AS CHAR(15)), h.cambiado_por,
           CAST(CONCAT(COALESCE(h.estado_anterior, '(inicio)'), ' -> ', h.estado_nuevo) AS CHAR(600))
      FROM incidencias_historial_estado h;

-- =====================================================================
-- R__datos_prueba.sql  (migración repetible, idempotente)
-- Datos de prueba para local y servidor de pruebas. Fechas en UTC (DATETIME(6)); los comentarios de hora local son -05. NO se carga en producción
-- (se controla con FLYWAY_LOCATIONS).
--
-- Contraseña de TODOS los usuarios demo: Prueba#2026
-- Los correos y nombres de usuario llevan el prefijo "demo" para no chocar con
-- otros datos de prueba. Los códigos de curso (salvo 1INF36) son ilustrativos.
--
-- Convención de ids fijos legibles: el primer grupo identifica la tabla
--   00000001 usuario        00000002 curso          00000003 canal
--   00000004 suscripcion    00000005 categoria (ver V2)
--   00000006 incidencia     00000007 incidencia_version
--   00000008 observacion    00000009 confirmacion   0000000a historial_estado
--   0000000b notificacion
-- Todos los INSERT usan ON DUPLICATE KEY UPDATE id = id (no INSERT IGNORE, que ocultaría otros errores), así que el script se puede
-- ejecutar varias veces sin duplicar nada.
-- =====================================================================


-- ---------------------------------------------------------------------
-- Usuarios (uno por rol, seis estudiantes y una cuenta desactivada)
-- ---------------------------------------------------------------------
INSERT INTO usuarios_usuario (id, correo, usuario, contrasena_hash, rol, activo, creado_en) VALUES
    ('00000001-0000-0000-0000-000000000001', 'demo.admin@pucp.edu.pe',     'demo_admin',     '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ADMINISTRADOR', TRUE,  '2026-08-01 14:00:00'),
    ('00000001-0000-0000-0000-000000000002', 'demo.director@pucp.edu.pe',  'demo_director',  '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'DIRECTOR',      TRUE,  '2026-08-01 14:05:00'),
    ('00000001-0000-0000-0000-000000000003', 'demo.profesor1@pucp.edu.pe', 'demo_profesor1', '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'PROFESOR',      TRUE,  '2026-08-01 14:10:00'),
    ('00000001-0000-0000-0000-000000000004', 'demo.profesor2@pucp.edu.pe', 'demo_profesor2', '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'PROFESOR',      TRUE,  '2026-08-01 14:15:00'),
    ('00000001-0000-0000-0000-000000000011', 'demo.est1@pucp.edu.pe',      'demo_est1',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:00:00'),
    ('00000001-0000-0000-0000-000000000012', 'demo.est2@pucp.edu.pe',      'demo_est2',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:05:00'),
    ('00000001-0000-0000-0000-000000000013', 'demo.est3@pucp.edu.pe',      'demo_est3',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:10:00'),
    ('00000001-0000-0000-0000-000000000014', 'demo.est4@pucp.edu.pe',      'demo_est4',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:15:00'),
    ('00000001-0000-0000-0000-000000000015', 'demo.est5@pucp.edu.pe',      'demo_est5',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:20:00'),
    ('00000001-0000-0000-0000-000000000016', 'demo.est6@pucp.edu.pe',      'demo_est6',      '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    TRUE,  '2026-08-10 15:25:00'),
    ('00000001-0000-0000-0000-000000000021', 'demo.inactivo@pucp.edu.pe',  'demo_inactivo',  '$2a$10$Xc0CswGV0F7QbxtApSXYsOTh//P.x.d.qi5LIWRg5Z69B81Nh0E8C', 'ESTUDIANTE',    FALSE, '2026-08-10 15:30:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Cursos y canales
-- ---------------------------------------------------------------------
INSERT INTO academico_curso (id, codigo, nombre, creado_en) VALUES
    ('00000002-0000-0000-0000-000000000001', '1INF36', 'Ética y RSU en Ingeniería Informática', '2026-08-02 14:00:00'),
    ('00000002-0000-0000-0000-000000000002', '1INF01', 'Fundamentos de Programación',            '2026-08-02 14:00:00'),
    ('00000002-0000-0000-0000-000000000003', '1INF03', 'Bases de Datos',                         '2026-08-02 14:00:00'),
    ('00000002-0000-0000-0000-000000000004', '1INF04', 'Ingeniería de Software',                 '2026-08-02 14:00:00')
ON DUPLICATE KEY UPDATE id = id;

-- Cuatro canales activos de 2026-2 y uno desactivado de 2026-1 (para probar RF-06.6).
INSERT INTO canales_canal (id, curso_id, anio, semestre, estado, creado_por, creado_en, desactivado_en) VALUES
    ('00000003-0000-0000-0000-000000000001', '00000002-0000-0000-0000-000000000001', 2026, 2, 'ACTIVO',      '00000001-0000-0000-0000-000000000001', '2026-08-03 15:00:00', NULL),
    ('00000003-0000-0000-0000-000000000002', '00000002-0000-0000-0000-000000000003', 2026, 2, 'ACTIVO',      '00000001-0000-0000-0000-000000000001', '2026-08-03 15:00:00', NULL),
    ('00000003-0000-0000-0000-000000000003', '00000002-0000-0000-0000-000000000002', 2026, 2, 'ACTIVO',      '00000001-0000-0000-0000-000000000001', '2026-08-03 15:00:00', NULL),
    ('00000003-0000-0000-0000-000000000004', '00000002-0000-0000-0000-000000000003', 2026, 1, 'DESACTIVADO', '00000001-0000-0000-0000-000000000001', '2026-03-02 15:00:00', '2026-07-20 17:00:00'),
    ('00000003-0000-0000-0000-000000000005', '00000002-0000-0000-0000-000000000004', 2026, 2, 'ACTIVO',      '00000001-0000-0000-0000-000000000001', '2026-08-03 15:00:00', NULL)
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Suscripciones
--   canal 1 (Ética y RSU)        : estudiantes 1 a 6 y profesor 1
--   canal 2 (Bases de Datos)     : estudiantes 1, 2 y 4 y profesor 1
--   canal 3 (Fundamentos)        : estudiantes 1 y 4 y profesor 2
--   canal 5 (Ing. de Software)   : estudiante 5
-- ---------------------------------------------------------------------
INSERT INTO canales_suscripcion (id, canal_id, usuario_id, creada_en) VALUES
    ('00000004-0000-0000-0000-000000000001', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000011', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000002', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000012', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000013', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000004', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000014', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000005', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000015', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000006', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000016', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000007', '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000003', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000008', '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000011', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000009', '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000012', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000010', '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000014', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000011', '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000003', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000012', '00000003-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000011', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000013', '00000003-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000014', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000014', '00000003-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000004', '2026-08-12 14:00:00'),
    ('00000004-0000-0000-0000-000000000015', '00000003-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000015', '2026-08-12 14:00:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Incidencias: una en cada estado
--   1 PENDIENTE  (canal Bases de Datos, estudiante 1)
--   2 OBSERVADA  (canal Bases de Datos, estudiante 2)
--   3 APROBADA   (canal Bases de Datos, estudiante 1) con 2 confirmaciones y un ciclo observar-editar-reenviar
--   4 RECHAZADA  (canal Ética y RSU, estudiante 2)
--   5 EN_ATENCION(canal Ética y RSU, estudiante 3) con 5 confirmaciones (prioridad alta)
--   6 RESUELTA   (canal Fundamentos, estudiante 4) con 1 confirmación
-- La incidencia guarda el contenido vigente; las versiones, las observaciones, las
-- confirmaciones y el historial se insertan después.
-- ---------------------------------------------------------------------
INSERT INTO incidencias_incidencia
    (id, codigo_seguimiento, canal_id, autor_id, categoria_id, titulo, descripcion, estado,
     motivo_rechazo, solucion, total_confirmaciones, aprobada_en, resuelta_en, creado_en, actualizado_en) VALUES
    ('00000006-0000-0000-0000-000000000001', 'INC-4H7Q2M9X',
     '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000011', '00000005-0000-0000-0000-000000000001',
     'Las notas del parcial no se publicaron en el plazo anunciado',
     'El sílabo indicaba que las notas del parcial se publicarían a los siete días de rendido el examen, pero han pasado tres semanas y todavía no hay resultados en la plataforma.',
     'PENDIENTE', NULL, NULL, 0, NULL, NULL,
     '2026-10-20 14:15:00', '2026-10-20 14:15:00'),
    ('00000006-0000-0000-0000-000000000002', 'INC-8K3T5WZ1',
     '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000012', '00000005-0000-0000-0000-000000000002',
     'El cronograma del curso no coincide con las clases dictadas',
     'Varias semanas se han dictado temas distintos a los del cronograma publicado, lo que dificulta prepararse para las evaluaciones.',
     'OBSERVADA', NULL, NULL, 0, NULL, NULL,
     '2026-10-18 21:40:00', '2026-10-19 15:00:00'),
    ('00000006-0000-0000-0000-000000000003', 'INC-2P9D6RFA',
     '00000003-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000011', '00000005-0000-0000-0000-000000000005',
     'El laboratorio no cuenta con equipos suficientes para todos los estudiantes',
     'En las sesiones de laboratorio hay 20 equipos operativos para unos 30 estudiantes, por lo que deben compartirlos y no logran completar las prácticas en el tiempo asignado.',
     'APROBADA', NULL, NULL, 2, '2026-10-14 15:30:00', NULL,
     '2026-10-10 16:00:00', '2026-10-14 15:30:00'),
    ('00000006-0000-0000-0000-000000000004', 'INC-7C1M4XBN',
     '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000012', '00000005-0000-0000-0000-000000000004',
     'Reclamo sobre el trato en clase',
     'Siento que el trato en las clases no es el adecuado y quisiera que se revise.',
     'RECHAZADA',
     'La descripción no detalla hechos concretos ni fechas, por lo que no es posible evaluarla. Puedes registrar un nuevo reporte con más detalle.',
     NULL, 0, NULL, NULL,
     '2026-10-08 13:20:00', '2026-10-09 16:15:00'),
    ('00000006-0000-0000-0000-000000000005', 'INC-5J8V3GQE',
     '00000003-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000013', '00000005-0000-0000-0000-000000000003',
     'Las clases no cuentan con material de apoyo previo',
     'El material de cada sesión se comparte solo después de la clase, por lo que no es posible revisarlo con anticipación ni llegar con dudas preparadas.',
     'EN_ATENCION', NULL, NULL, 5, '2026-10-01 14:00:00', NULL,
     '2026-09-28 19:05:00', '2026-10-06 17:00:00'),
    ('00000006-0000-0000-0000-000000000006', 'INC-9R2L7HYD',
     '00000003-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000014', '00000005-0000-0000-0000-000000000001',
     'La rúbrica de la práctica calificada no se entregó antes de la evaluación',
     'No se conocían los criterios con los que se calificaría la práctica, lo que impidió prepararse correctamente.',
     'RESUELTA', NULL,
     'Se publicó la rúbrica en la plataforma virtual y se acordó entregarla una semana antes de cada evaluación.',
     1, '2026-09-17 17:00:00', '2026-10-05 22:30:00',
     '2026-09-15 15:10:00', '2026-10-05 22:30:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Versiones del contenido (la última coincide con la incidencia vigente)
-- ---------------------------------------------------------------------
-- La incidencia 3 tuvo dos versiones: la inicial (observada) y la corregida.
INSERT INTO incidencias_version (id, incidencia_id, numero, titulo, descripcion, categoria_id, creada_en) VALUES
    ('00000007-0000-0000-0000-000000000003', '00000006-0000-0000-0000-000000000003', 1,
     'El laboratorio no tiene equipos',
     'Faltan equipos en el laboratorio y no se puede trabajar bien.',
     '00000005-0000-0000-0000-000000000005', '2026-10-10 16:00:00')
ON DUPLICATE KEY UPDATE id = id;

-- Las demás versiones (y la versión 2 de la incidencia 3) se copian de la incidencia vigente.
INSERT INTO incidencias_version (id, incidencia_id, numero, titulo, descripcion, categoria_id, creada_en)
SELECT v.id, i.id, v.numero, i.titulo, i.descripcion, i.categoria_id, v.creada_en
  FROM (VALUES
        ROW('00000007-0000-0000-0000-000000000001', '00000006-0000-0000-0000-000000000001', 1, '2026-10-20 14:15:00'),
        ROW('00000007-0000-0000-0000-000000000002', '00000006-0000-0000-0000-000000000002', 1, '2026-10-18 21:40:00'),
        ROW('00000007-0000-0000-0000-000000000004', '00000006-0000-0000-0000-000000000003', 2, '2026-10-13 20:00:00'),
        ROW('00000007-0000-0000-0000-000000000005', '00000006-0000-0000-0000-000000000004', 1, '2026-10-08 13:20:00'),
        ROW('00000007-0000-0000-0000-000000000006', '00000006-0000-0000-0000-000000000005', 1, '2026-09-28 19:05:00'),
        ROW('00000007-0000-0000-0000-000000000007', '00000006-0000-0000-0000-000000000006', 1, '2026-09-15 15:10:00')
       ) AS v (id, incidencia_id, numero, creada_en)
  JOIN incidencias_incidencia i ON i.id = v.incidencia_id
ON DUPLICATE KEY UPDATE incidencias_version.id = incidencias_version.id;


-- ---------------------------------------------------------------------
-- Observaciones del director (la incidencia 3 recibió dos sobre su versión 1)
-- ---------------------------------------------------------------------
INSERT INTO incidencias_observacion (id, incidencia_id, version_id, texto, creada_por, creada_en) VALUES
    ('00000008-0000-0000-0000-000000000001', '00000006-0000-0000-0000-000000000002', '00000007-0000-0000-0000-000000000002',
     'Indica en qué semanas el cronograma publicado no coincidió con los temas dictados.',
     '00000001-0000-0000-0000-000000000002', '2026-10-19 15:00:00'),
    ('00000008-0000-0000-0000-000000000002', '00000006-0000-0000-0000-000000000003', '00000007-0000-0000-0000-000000000003',
     'Especifica cuántos equipos hay disponibles y cuántos estudiantes asisten al laboratorio.',
     '00000001-0000-0000-0000-000000000002', '2026-10-11 14:30:00'),
    ('00000008-0000-0000-0000-000000000003', '00000006-0000-0000-0000-000000000003', '00000007-0000-0000-0000-000000000003',
     'Explica cómo afecta esta situación al desarrollo de las prácticas.',
     '00000001-0000-0000-0000-000000000002', '2026-10-11 14:30:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Confirmaciones (coinciden con total_confirmaciones de cada incidencia)
-- ---------------------------------------------------------------------
INSERT INTO incidencias_confirmacion (id, incidencia_id, estudiante_id, creada_en) VALUES
    ('00000009-0000-0000-0000-000000000001', '00000006-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000012', '2026-10-15 15:00:00'),
    ('00000009-0000-0000-0000-000000000002', '00000006-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000014', '2026-10-15 23:20:00'),
    ('00000009-0000-0000-0000-000000000003', '00000006-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000011', '2026-10-02 14:10:00'),
    ('00000009-0000-0000-0000-000000000004', '00000006-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000012', '2026-10-02 16:30:00'),
    ('00000009-0000-0000-0000-000000000005', '00000006-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000014', '2026-10-03 13:45:00'),
    ('00000009-0000-0000-0000-000000000006', '00000006-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000015', '2026-10-03 21:00:00'),
    ('00000009-0000-0000-0000-000000000007', '00000006-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000016', '2026-10-04 15:20:00'),
    ('00000009-0000-0000-0000-000000000008', '00000006-0000-0000-0000-000000000006', '00000001-0000-0000-0000-000000000011', '2026-09-18 14:00:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Historial de estados
-- ---------------------------------------------------------------------
INSERT INTO incidencias_historial_estado (id, incidencia_id, estado_anterior, estado_nuevo, cambiado_por, cambiado_en) VALUES
    -- Incidencia 1: registrada
    ('0000000a-0000-0000-0000-000000000001', '00000006-0000-0000-0000-000000000001', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000011', '2026-10-20 14:15:00'),
    -- Incidencia 2: registrada y observada
    ('0000000a-0000-0000-0000-000000000002', '00000006-0000-0000-0000-000000000002', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000012', '2026-10-18 21:40:00'),
    ('0000000a-0000-0000-0000-000000000003', '00000006-0000-0000-0000-000000000002', 'PENDIENTE',   'OBSERVADA',   '00000001-0000-0000-0000-000000000002', '2026-10-19 15:00:00'),
    -- Incidencia 3: registrada, observada, reenviada y aprobada
    ('0000000a-0000-0000-0000-000000000004', '00000006-0000-0000-0000-000000000003', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000011', '2026-10-10 16:00:00'),
    ('0000000a-0000-0000-0000-000000000005', '00000006-0000-0000-0000-000000000003', 'PENDIENTE',   'OBSERVADA',   '00000001-0000-0000-0000-000000000002', '2026-10-11 14:30:00'),
    ('0000000a-0000-0000-0000-000000000006', '00000006-0000-0000-0000-000000000003', 'OBSERVADA',   'PENDIENTE',   '00000001-0000-0000-0000-000000000011', '2026-10-13 20:00:00'),
    ('0000000a-0000-0000-0000-000000000007', '00000006-0000-0000-0000-000000000003', 'PENDIENTE',   'APROBADA',    '00000001-0000-0000-0000-000000000002', '2026-10-14 15:30:00'),
    -- Incidencia 4: registrada y rechazada
    ('0000000a-0000-0000-0000-000000000008', '00000006-0000-0000-0000-000000000004', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000012', '2026-10-08 13:20:00'),
    ('0000000a-0000-0000-0000-000000000009', '00000006-0000-0000-0000-000000000004', 'PENDIENTE',   'RECHAZADA',   '00000001-0000-0000-0000-000000000002', '2026-10-09 16:15:00'),
    -- Incidencia 5: aprobada y en atención
    ('0000000a-0000-0000-0000-000000000010', '00000006-0000-0000-0000-000000000005', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000013', '2026-09-28 19:05:00'),
    ('0000000a-0000-0000-0000-000000000011', '00000006-0000-0000-0000-000000000005', 'PENDIENTE',   'APROBADA',    '00000001-0000-0000-0000-000000000002', '2026-10-01 14:00:00'),
    ('0000000a-0000-0000-0000-000000000012', '00000006-0000-0000-0000-000000000005', 'APROBADA',    'EN_ATENCION', '00000001-0000-0000-0000-000000000002', '2026-10-06 17:00:00'),
    -- Incidencia 6: ciclo completo hasta resuelta
    ('0000000a-0000-0000-0000-000000000013', '00000006-0000-0000-0000-000000000006', NULL,          'PENDIENTE',   '00000001-0000-0000-0000-000000000014', '2026-09-15 15:10:00'),
    ('0000000a-0000-0000-0000-000000000014', '00000006-0000-0000-0000-000000000006', 'PENDIENTE',   'APROBADA',    '00000001-0000-0000-0000-000000000002', '2026-09-17 17:00:00'),
    ('0000000a-0000-0000-0000-000000000015', '00000006-0000-0000-0000-000000000006', 'APROBADA',    'EN_ATENCION', '00000001-0000-0000-0000-000000000002', '2026-09-20 14:00:00'),
    ('0000000a-0000-0000-0000-000000000016', '00000006-0000-0000-0000-000000000006', 'EN_ATENCION', 'RESUELTA',    '00000001-0000-0000-0000-000000000002', '2026-10-05 22:30:00')
ON DUPLICATE KEY UPDATE id = id;


-- ---------------------------------------------------------------------
-- Notificaciones (algunas leídas y otras no). Sin nombre del profesor ni del autor.
-- ---------------------------------------------------------------------
INSERT INTO notificaciones_notificacion
    (id, destinatario_id, tipo, incidencia_id, canal_id, titulo, mensaje, leida_en, creada_en) VALUES
    ('0000000b-0000-0000-0000-000000000001', '00000001-0000-0000-0000-000000000011', 'OBSERVADA',   '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Tu incidencia fue observada', 'El director dejó observaciones sobre tu incidencia INC-2P9D6RFA. Edítala y reenvíala.', '2026-10-11 17:00:00', '2026-10-11 14:30:00'),
    ('0000000b-0000-0000-0000-000000000002', '00000001-0000-0000-0000-000000000002', 'REENVIADA',   '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Incidencia reenviada', 'Una incidencia observada fue reenviada y está nuevamente pendiente de evaluación.', '2026-10-13 21:00:00', '2026-10-13 20:00:00'),
    ('0000000b-0000-0000-0000-000000000003', '00000001-0000-0000-0000-000000000011', 'APROBADA',    '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Tu incidencia fue aprobada', 'Tu incidencia INC-2P9D6RFA fue aprobada y ya es visible para los suscritos del canal.', NULL, '2026-10-14 15:30:00'),
    ('0000000b-0000-0000-0000-000000000004', '00000001-0000-0000-0000-000000000012', 'APROBADA',    '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Nueva incidencia aprobada en tu canal', 'Se aprobó una incidencia en el canal de Bases de Datos 2026-2. Puedes revisarla y confirmarla.', NULL, '2026-10-14 15:30:00'),
    ('0000000b-0000-0000-0000-000000000005', '00000001-0000-0000-0000-000000000014', 'APROBADA',    '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Nueva incidencia aprobada en tu canal', 'Se aprobó una incidencia en el canal de Bases de Datos 2026-2. Puedes revisarla y confirmarla.', '2026-10-15 23:00:00', '2026-10-14 15:30:00'),
    ('0000000b-0000-0000-0000-000000000006', '00000001-0000-0000-0000-000000000003', 'APROBADA',    '00000006-0000-0000-0000-000000000003', '00000003-0000-0000-0000-000000000002',
     'Nueva incidencia aprobada en tu canal', 'Se aprobó una incidencia en el canal de Bases de Datos 2026-2.', NULL, '2026-10-14 15:30:00'),
    ('0000000b-0000-0000-0000-000000000007', '00000001-0000-0000-0000-000000000012', 'RECHAZADA',   '00000006-0000-0000-0000-000000000004', '00000003-0000-0000-0000-000000000001',
     'Tu incidencia fue rechazada', 'Tu incidencia INC-7C1M4XBN fue rechazada. Revisa el motivo en el seguimiento de tus incidencias.', '2026-10-09 23:00:00', '2026-10-09 16:15:00'),
    ('0000000b-0000-0000-0000-000000000008', '00000001-0000-0000-0000-000000000012', 'OBSERVADA',   '00000006-0000-0000-0000-000000000002', '00000003-0000-0000-0000-000000000002',
     'Tu incidencia fue observada', 'El director dejó observaciones sobre tu incidencia INC-8K3T5WZ1. Edítala y reenvíala.', NULL, '2026-10-19 15:00:00'),
    ('0000000b-0000-0000-0000-000000000009', '00000001-0000-0000-0000-000000000013', 'EN_ATENCION', '00000006-0000-0000-0000-000000000005', '00000003-0000-0000-0000-000000000001',
     'Tu incidencia está en atención', 'Tu incidencia INC-5J8V3GQE pasó a En atención.', NULL, '2026-10-06 17:00:00'),
    ('0000000b-0000-0000-0000-000000000010', '00000001-0000-0000-0000-000000000011', 'EN_ATENCION', '00000006-0000-0000-0000-000000000005', '00000003-0000-0000-0000-000000000001',
     'Una incidencia de tu canal está en atención', 'Una incidencia del canal de Ética y RSU 2026-2 pasó a En atención.', NULL, '2026-10-06 17:00:00'),
    ('0000000b-0000-0000-0000-000000000011', '00000001-0000-0000-0000-000000000003', 'EN_ATENCION', '00000006-0000-0000-0000-000000000005', '00000003-0000-0000-0000-000000000001',
     'Una incidencia de tu canal está en atención', 'Una incidencia del canal de Ética y RSU 2026-2 pasó a En atención.', '2026-10-07 13:30:00', '2026-10-06 17:00:00'),
    ('0000000b-0000-0000-0000-000000000012', '00000001-0000-0000-0000-000000000014', 'RESUELTA',    '00000006-0000-0000-0000-000000000006', '00000003-0000-0000-0000-000000000003',
     'Tu incidencia fue resuelta', 'Tu incidencia INC-9R2L7HYD fue resuelta. Revisa la solución aplicada.', '2026-10-06 00:00:00', '2026-10-05 22:30:00'),
    ('0000000b-0000-0000-0000-000000000013', '00000001-0000-0000-0000-000000000011', 'RESUELTA',    '00000006-0000-0000-0000-000000000006', '00000003-0000-0000-0000-000000000003',
     'Una incidencia de tu canal fue resuelta', 'Una incidencia del canal de Fundamentos de Programación 2026-2 fue resuelta.', NULL, '2026-10-05 22:30:00'),
    ('0000000b-0000-0000-0000-000000000014', '00000001-0000-0000-0000-000000000004', 'RESUELTA',    '00000006-0000-0000-0000-000000000006', '00000003-0000-0000-0000-000000000003',
     'Una incidencia de tu canal fue resuelta', 'Una incidencia del canal de Fundamentos de Programación 2026-2 fue resuelta.', NULL, '2026-10-05 22:30:00')
ON DUPLICATE KEY UPDATE id = id;

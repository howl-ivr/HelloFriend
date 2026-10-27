-- Datos de DEMOSTRACION para probar todos los roles en una base LOCAL. No es una migracion normal:
--  * Vive en db/dev y Flyway solo la ve si arrancas con FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/dev
--    (ver backend/README.md). Nunca se carga en pruebas, CI ni RDS.
--  * Es repetible (R__): se vuelve a ejecutar cuando cambia su contenido y no duplica nada (INSERT IGNORE, ids fijos).
-- Todas las cuentas tienen la contrasena Prueba123 (hash BCrypt).

INSERT IGNORE INTO usuarios_usuario (id, correo, usuario, contrasena_hash, rol) VALUES
    ('00000000-0000-4000-8000-000000000001', 'admin@pucp.edu.pe',    'admin',   '$2a$10$BY/MlPsIsQd2bWQtwVpNOeHrxpTOWmLhOdRTs75PEph0oKGNEUJEK', 'ADMINISTRADOR'),
    ('00000000-0000-4000-8000-000000000002', 'director@pucp.edu.pe', 'director','$2a$10$BY/MlPsIsQd2bWQtwVpNOeHrxpTOWmLhOdRTs75PEph0oKGNEUJEK', 'DIRECTOR'),
    ('00000000-0000-4000-8000-000000000003', 'profesor@pucp.edu.pe', 'profesor','$2a$10$BY/MlPsIsQd2bWQtwVpNOeHrxpTOWmLhOdRTs75PEph0oKGNEUJEK', 'PROFESOR'),
    ('00000000-0000-4000-8000-000000000004', 'ana@pucp.edu.pe',      'ana',     '$2a$10$BY/MlPsIsQd2bWQtwVpNOeHrxpTOWmLhOdRTs75PEph0oKGNEUJEK', 'ESTUDIANTE'),
    ('00000000-0000-4000-8000-000000000005', 'luis@pucp.edu.pe',     'luis',    '$2a$10$BY/MlPsIsQd2bWQtwVpNOeHrxpTOWmLhOdRTs75PEph0oKGNEUJEK', 'ESTUDIANTE');

INSERT IGNORE INTO academico_curso (id, codigo, nombre) VALUES
    ('00000000-0000-4000-8000-0000000000c1', 'INF263', 'Ingenieria de Software'),
    ('00000000-0000-4000-8000-0000000000c2', 'INF245', 'Bases de Datos');

INSERT IGNORE INTO canales_canal (id, curso_id, anio, semestre, creado_por) VALUES
    ('00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-0000000000c1', 2026, 2,
     '00000000-0000-4000-8000-000000000001');

INSERT IGNORE INTO canales_suscripcion (id, canal_id, usuario_id) VALUES
    ('00000000-0000-4000-8000-0000000000e1', '00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-000000000003'),
    ('00000000-0000-4000-8000-0000000000e2', '00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-000000000004'),
    ('00000000-0000-4000-8000-0000000000e3', '00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-000000000005');

-- Tres incidencias de ana: una pendiente de revision, una publicada con una confirmacion de luis y una rechazada.
INSERT IGNORE INTO incidencias_incidencia
    (id, codigo_seguimiento, canal_id, autor_id, categoria_id, titulo, descripcion, estado, motivo_rechazo,
     total_confirmaciones, aprobada_en) VALUES
    ('00000000-0000-4000-8000-0000000000f1', 'DEMO-0001', '00000000-0000-4000-8000-0000000000d1',
     '00000000-0000-4000-8000-000000000004', (SELECT id FROM incidencias_categoria ORDER BY nombre LIMIT 1),
     'Notas sin publicar', 'Las notas de la practica 2 siguen sin publicarse pasado el plazo.', 'PENDIENTE', NULL, 0, NULL),
    ('00000000-0000-4000-8000-0000000000f2', 'DEMO-0002', '00000000-0000-4000-8000-0000000000d1',
     '00000000-0000-4000-8000-000000000004', (SELECT id FROM incidencias_categoria ORDER BY nombre LIMIT 1),
     'Aula sin proyector', 'El proyector del aula lleva dos semanas sin funcionar.', 'APROBADA', NULL, 1,
     UTC_TIMESTAMP(6)),
    ('00000000-0000-4000-8000-0000000000f3', 'DEMO-0003', '00000000-0000-4000-8000-0000000000d1',
     '00000000-0000-4000-8000-000000000004', (SELECT id FROM incidencias_categoria ORDER BY nombre LIMIT 1),
     'Queja general', 'No me gusta el curso.', 'RECHAZADA', 'Descripcion sin hechos verificables.', 0, NULL);

INSERT IGNORE INTO incidencias_version (id, incidencia_id, numero, titulo, descripcion, categoria_id)
    SELECT CONCAT('00000000-0000-4000-8000-0000000001', RIGHT(i.id, 2)), i.id, 1, i.titulo, i.descripcion, i.categoria_id
      FROM incidencias_incidencia i
     WHERE i.id IN ('00000000-0000-4000-8000-0000000000f1', '00000000-0000-4000-8000-0000000000f2',
                    '00000000-0000-4000-8000-0000000000f3');

INSERT IGNORE INTO incidencias_confirmacion (id, incidencia_id, estudiante_id) VALUES
    ('00000000-0000-4000-8000-0000000002f2', '00000000-0000-4000-8000-0000000000f2', '00000000-0000-4000-8000-000000000005');

INSERT IGNORE INTO incidencias_historial_estado (id, incidencia_id, estado_anterior, estado_nuevo, cambiado_por, tipo_evento) VALUES
    ('00000000-0000-4000-8000-0000000003f1', '00000000-0000-4000-8000-0000000000f1', NULL, 'PENDIENTE', '00000000-0000-4000-8000-000000000004', 'CREADA'),
    ('00000000-0000-4000-8000-0000000003f2', '00000000-0000-4000-8000-0000000000f2', NULL, 'PENDIENTE', '00000000-0000-4000-8000-000000000004', 'CREADA'),
    ('00000000-0000-4000-8000-0000000003f3', '00000000-0000-4000-8000-0000000000f3', NULL, 'PENDIENTE', '00000000-0000-4000-8000-000000000004', 'CREADA'),
    ('00000000-0000-4000-8000-0000000004f2', '00000000-0000-4000-8000-0000000000f2', 'PENDIENTE', 'APROBADA', '00000000-0000-4000-8000-000000000002', 'APROBADA'),
    ('00000000-0000-4000-8000-0000000004f3', '00000000-0000-4000-8000-0000000000f3', 'PENDIENTE', 'RECHAZADA', '00000000-0000-4000-8000-000000000002', 'RECHAZADA');

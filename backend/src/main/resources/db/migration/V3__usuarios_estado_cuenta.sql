-- V3 · usuarios: estado de la cuenta (docs/API.md). Una cuenta nueva queda PENDIENTE hasta que el administrador
-- la aprueba (ACTIVA) o la rechaza (RECHAZADA). El DEFAULT 'ACTIVA' cubre a admin, director y profesor, que no se
-- registran solos, y a los datos de prueba; el servicio de registro fija PENDIENTE de forma explicita.
-- "activo" sigue siendo la desactivacion de una cuenta ya aprobada; para entrar hacen falta estado ACTIVA y activo.
ALTER TABLE usuarios_usuario
    ADD COLUMN estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
    ADD CONSTRAINT ck_usuarios_usuario_estado CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'RECHAZADA'));

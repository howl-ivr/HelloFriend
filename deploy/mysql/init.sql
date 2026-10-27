-- Inicialización del MySQL local de Docker (solo se ejecuta la primera vez, con el volumen vacío).
-- Lo monta deploy/docker-compose.local.yml. SIN VERIFICAR EN ESTA MÁQUINA (no hay Docker instalado).
-- La base hellofriend y el usuario hellofriend ya los crea la imagen con MYSQL_DATABASE y MYSQL_USER;
-- aquí se crea la base de pruebas y se le da acceso al mismo usuario. No lleva contraseñas.
CREATE DATABASE IF NOT EXISTS hellofriend_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON hellofriend_test.* TO 'hellofriend'@'%';

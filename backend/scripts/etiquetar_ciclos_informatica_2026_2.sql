-- Ejecutar solo en tu MySQL local, después de aplicar V6 y de importar los cursos.
-- Plan de Ingeniería Informática vigente en 2026-2, ciclos 5 a 10.
-- Es repetible: solo modifica los códigos del plan con ciclo fijo.
-- Los cursos sin correspondencia y sin ciclo asignado se muestran como Electivos (ciclo NULL).
-- No cambia ciclos que un administrador haya asignado manualmente a otros cursos.
UPDATE academico_curso
SET ciclo = CASE
    WHEN codigo IN ('1GES92', '1IND90', '1INF25', '1INF27', '1INF28', '1INF33', '1INF49') THEN 5
    WHEN codigo IN ('1CON27', '1ELE01', '1EST22', '1INF29', '1INF30', '1INF31', '1INF32') THEN 6
    WHEN codigo IN ('1EST14', '1INF24', '1INF34', '1INF35', '1INF50', 'IND231', 'INF238') THEN 7
    WHEN codigo IN ('1INF26', '1INF36', '1INF37', '1INF38', '1INF39', '1INF41', '1INF65') THEN 8
    WHEN codigo IN ('1INF42', '1INF44', '1INF45', '1INF54', 'IND328', 'INF008') THEN 9
    WHEN codigo IN ('1INF46', '1INF47', '1INF48') THEN 10
    ELSE ciclo
END
WHERE codigo IN (
    '1GES92', '1IND90', '1INF25', '1INF27', '1INF28', '1INF33', '1INF49',
    '1CON27', '1ELE01', '1EST22', '1INF29', '1INF30', '1INF31', '1INF32',
    '1EST14', '1INF24', '1INF34', '1INF35', '1INF50', 'IND231', 'INF238',
    '1INF26', '1INF36', '1INF37', '1INF38', '1INF39', '1INF41', '1INF65',
    '1INF42', '1INF44', '1INF45', '1INF54', 'IND328', 'INF008',
    '1INF46', '1INF47', '1INF48'
);

SELECT codigo, nombre, ciclo,
       CASE WHEN ciclo IS NULL THEN 'Electivos' ELSE CONCAT('Ciclo ', ciclo) END AS etiqueta
FROM academico_curso
ORDER BY ciclo, codigo;

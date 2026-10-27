# Ciclo académico del catálogo de Informática

Fecha: 2026-10-27

## Decisión

- `academico_curso.ciclo` guarda el ciclo sugerido del plan de Ingeniería Informática (`1` a `10`).
- `NULL` identifica a los **Electivos**. Los cursos del catálogo sin correspondencia en los ciclos del plan se muestran con esa etiqueta.
- La migración V6 añade la columna y etiqueta por código los cursos obligatorios de los ciclos 5 a 10 del plan vigente en 2026-2.
- Un SQL repetible en `backend/scripts/etiquetar_ciclos_informatica_2026_2.sql` permite etiquetar los cursos importados después de aplicar V6.
- El administrador puede asignar el ciclo al registrar o modificar un curso. La creación de canales agrupa el catálogo por ciclo; el año y semestre del canal siguen siendo conceptos distintos.

## Fuente y alcance

Plan de estudios `INFORMATICA_ppee_FCI-2026-2.pdf`, proporcionado por el usuario. El PDF lista cursos de especialidad desde el ciclo 5; los demás cursos del catálogo se presentan como Electivos hasta que un administrador les asigne un ciclo.

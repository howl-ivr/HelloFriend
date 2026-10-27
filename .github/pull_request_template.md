## Qué cambia

<!-- 1–3 líneas. Qué historia de usuario cubre y qué hace ahora el sistema que antes no hacía. -->

- **HU:** HU-xx
- **Módulo:** <usuarios, academico, canales, incidencias…>
- **Endpoints o pantallas:** <si aplica>

## Cómo probarlo

<!-- Pasos para que quien revise lo vea funcionar: qué ejecutar, qué enviar, qué debe pasar. -->
1.

## Checklist

- [ ] Cumple las HU y RF del módulo (`docs/API.md` si cambia el contrato con el frontend).
- [ ] Pruebas nuevas para lo que agregué, y **todas** pasan en mi PC (`.\mvnw.cmd verify` en `backend/`).
- [ ] Si cambia la BD: migración **nueva** `V{n}__{modulo}_{tema}.sql` (no edité una existente) y avisé el número al equipo antes de crearla.
- [ ] Los nombres siguen `docs/guias/base-de-datos.md` (prefijo `{modulo}_{entidad}`, identificadores sin tildes ni ñ).
- [ ] Si agregué endpoints: actualicé la tabla de `backend/README.md`.
- [ ] Si tomé una decisión que afecta a otros: entrada en `docs/decisiones/`.
- [ ] Sin contraseñas, tokens ni datos personales reales; nada apunta a RDS.

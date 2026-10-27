# HelloFriend · Frontend

Portal web de reporte anónimo de incidencias por curso para la facultad. React 19, Vite y Tailwind 4.

## Cómo correrlo

Requiere Node 20.19 o superior.

```bash
cd frontend
npm install
npm run dev
```

La app queda en http://localhost:5173.

## Modo demostración y modo backend

El archivo `.env.development` define `VITE_MOCK`:

- `VITE_MOCK=true`: los datos se simulan en el navegador (se guardan en localStorage). No hace falta backend.
- `VITE_MOCK=false`: las llamadas a `/api` van al backend Spring Boot en http://localhost:8080 (ver `vite.config.js`).

El contrato que debe respetar el backend está en `../docs/API.md`.

Cuentas del modo demostración:

| Usuario | Contraseña | Rol |
|---|---|---|
| alumno1 a alumno5 | alumno123 | Estudiante |
| cachimbo26 | nuevo123 | Estudiante recién registrado |
| profesor | profesor123 | Profesor |
| director | director123 | Director de carrera |
| admin | admin123 | Administrador |
| exalumno | alumno123 | Cuenta desactivada (no puede ingresar) |

Para reiniciar los datos simulados: `localStorage.removeItem('hf_mock_db_v3')` en la consola del navegador.

## Scripts

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Compila a `dist/` |
| `npm run preview` | Sirve la compilación |
| `npm run lint` | Revisa el código con ESLint |

## Estructura

```
src/
  api/         cliente axios (client.js) y backend simulado (mock.js)
  components/  piezas reutilizables (tarjetas, botones, navbar, paneles)
  context/     sesión del usuario (AuthContext)
  pages/       una pantalla por ruta, agrupadas por rol
  utils/       formato de fechas, estados y textos compartidos
```

Rutas por rol: estudiante y profesor usan `/inicio`; el estudiante además `/incidencias/nueva`, `/incidencias/:id/editar` y `/mis-incidencias`; el director `/director/*` (pendientes, publicadas y estadísticas); el administrador `/admin/*` (usuarios, canales y auditoría). La protección está en `components/RutaProtegida.jsx`.

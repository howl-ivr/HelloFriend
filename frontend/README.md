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

## Backend y datos

Todas las llamadas a `/api` van al backend Spring Boot. Durante el desarrollo, Vite las dirige a `http://localhost:8080` mediante `vite.config.js`. Los cursos, canales, incidencias, interacciones, notificaciones y usuarios se leen de MySQL a través del backend; `localStorage` solo guarda el token de sesión. El contrato está en `../docs/API.md`.

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
  api/         cliente axios para el backend
  components/  piezas reutilizables (tarjetas, botones, navbar, paneles)
  context/     sesión del usuario (AuthContext)
  pages/       una pantalla por ruta, agrupadas por rol
  utils/       formato de fechas, estados y textos compartidos
```

Rutas por rol: estudiante y profesor usan `/inicio`; el estudiante además `/incidencias/nueva`, `/incidencias/:id/editar` y `/mis-incidencias`; el director `/director/*` (pendientes, publicadas y estadísticas); el administrador `/admin/*` (usuarios, canales y auditoría). La protección está en `components/RutaProtegida.jsx`.

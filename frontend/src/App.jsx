import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import RutaProtegida from './components/RutaProtegida'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Registro from './pages/Registro'
import Inicio from './pages/Inicio'
import Canales from './pages/Canales'
import Canal from './pages/Canal'
import DetalleIncidencia from './pages/DetalleIncidencia'
import NuevaIncidencia from './pages/NuevaIncidencia'
import EditarIncidencia from './pages/EditarIncidencia'
import MisIncidencias from './pages/MisIncidencias'
import Notificaciones from './pages/Notificaciones'
import DirectorPendientes from './pages/DirectorPendientes'
import DirectorPublicadas from './pages/DirectorPublicadas'
import DirectorEstadisticas from './pages/DirectorEstadisticas'
import AdminCuentas from './pages/AdminCuentas'
import AdminCanales from './pages/AdminCanales'
import AdminAuditoria from './pages/AdminAuditoria'
import AdminReporte from './pages/AdminReporte'

const INICIO = {
  ESTUDIANTE: '/inicio',
  PROFESOR: '/inicio',
  DIRECTOR: '/director/pendientes',
  ADMIN: '/admin/cuentas'
}

function Raiz() {
  const { token, rol } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return <Navigate to={INICIO[rol] ?? '/login'} replace />
}

const P = ({ roles, children, ancho = false }) => (
  <RutaProtegida roles={roles}>
    {ancho ? children : <div className="max-w-3xl mx-auto">{children}</div>}
  </RutaProtegida>
)

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen">
          <Navbar />
          <div className="max-w-6xl mx-auto px-4 py-6">
            <Routes>
              <Route path="/" element={<Raiz />} />
              <Route path="/login" element={<Login />} />
              <Route path="/registro" element={<Registro />} />

              <Route path="/inicio" element={<P roles={['ESTUDIANTE', 'PROFESOR']} ancho><Inicio /></P>} />
              <Route path="/canales" element={<P roles={['ESTUDIANTE', 'PROFESOR']}><Canales /></P>} />
              <Route path="/canales/:id" element={<P roles={['ESTUDIANTE', 'PROFESOR']}><Canal /></P>} />
              <Route path="/incidencias/nueva" element={<P roles={['ESTUDIANTE']}><NuevaIncidencia /></P>} />
              <Route path="/incidencias/:id/editar" element={<P roles={['ESTUDIANTE']}><EditarIncidencia /></P>} />
              <Route path="/incidencias/:id" element={<P roles={['ESTUDIANTE', 'PROFESOR', 'DIRECTOR']}><DetalleIncidencia /></P>} />
              <Route path="/mis-incidencias" element={<P roles={['ESTUDIANTE']}><MisIncidencias /></P>} />
              <Route path="/notificaciones" element={<P roles={['ESTUDIANTE', 'PROFESOR', 'DIRECTOR']}><Notificaciones /></P>} />

              <Route path="/director/pendientes" element={<P roles={['DIRECTOR']}><DirectorPendientes /></P>} />
              <Route path="/director/publicadas" element={<P roles={['DIRECTOR']}><DirectorPublicadas /></P>} />
              <Route path="/director/estadisticas" element={<P roles={['DIRECTOR']}><DirectorEstadisticas /></P>} />

              <Route path="/admin/cuentas" element={<P roles={['ADMIN']} ancho><AdminCuentas /></P>} />
              <Route path="/admin/canales" element={<P roles={['ADMIN']} ancho><AdminCanales /></P>} />
              <Route path="/admin/auditoria" element={<P roles={['ADMIN']} ancho><AdminAuditoria /></P>} />
              <Route path="/admin/auditoria/:id" element={<P roles={['ADMIN']}><AdminReporte /></P>} />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}

import { Navigate } from 'react-router-dom'

// La exploración de canales ahora es una ventana dentro del inicio.
export default function Canales() {
  return <Navigate to="/inicio?explorar=1" replace />
}

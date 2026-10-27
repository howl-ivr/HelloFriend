import { Navigate, useParams } from 'react-router-dom'

// Los canales ahora se ven dentro del inicio; esta ruta se mantiene para los enlaces antiguos.
export default function Canal() {
  const { id } = useParams()
  return <Navigate to={`/inicio?canal=${id}`} replace />
}

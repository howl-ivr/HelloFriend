import { EVENTOS, fechaHora } from '../utils/formato'

export default function Historial({ eventos, titulo = 'Historial' }) {
  if (!eventos?.length) return null
  return (
    <div className="bg-white border border-linea rounded-2xl p-6 mt-4">
      <h2 className="font-semibold text-tinta mb-4">{titulo}</h2>
      <ol className="relative border-l border-linea ml-1.5 space-y-4">
        {eventos.map((e, idx) => (
          <li key={idx} className="ml-5">
            <span className="absolute -left-[5px] mt-1.5 h-2.5 w-2.5 rounded-full bg-marca" aria-hidden="true" />
            <p className="text-sm font-medium text-tinta">{EVENTOS[e.tipo] ?? e.tipo}</p>
            <p className="text-xs text-suave">
              {fechaHora(e.fecha)}
              {e.responsable && <> · {e.responsable}</>}
            </p>
            {e.detalle && <p className="text-sm text-tinta/80 mt-1 whitespace-pre-line">{e.detalle}</p>}
          </li>
        ))}
      </ol>
    </div>
  )
}

export function Versiones({ versiones }) {
  if (!versiones || versiones.length < 2) return null
  return (
    <div className="bg-white border border-linea rounded-2xl p-6 mt-4">
      <h2 className="font-semibold text-tinta mb-3">Versiones del reporte</h2>
      <ul className="space-y-2">
        {[...versiones].reverse().map(v => (
          <li key={v.n}>
            <details className="rounded-xl border border-linea px-3 py-2">
              <summary className="cursor-pointer text-sm font-medium text-tinta">
                Versión {v.n} <span className="text-suave font-normal">· {fechaHora(v.fecha)}</span>
              </summary>
              <p className="text-xs text-suave mt-2">{v.categoria}</p>
              <p className="text-sm font-semibold text-tinta mt-1">{v.titulo}</p>
              <p className="text-sm text-tinta whitespace-pre-line mt-1">{v.descripcion}</p>
            </details>
          </li>
        ))}
      </ul>
    </div>
  )
}

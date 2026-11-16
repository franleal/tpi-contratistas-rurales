# Formato de error y tabla de códigos HTTP

Todos los microservicios responden los errores con el mismo cuerpo, definido en agro-commons (`ApiError` + `GlobalExceptionHandler`). El Gateway usa el mismo formato para sus 401 y 403.

```json
{
  "codigo": "REGLA_NEGOCIO",
  "mensaje": "El lote está en zona de EXCLUSION: no admite órdenes de pulverización (RN-06)",
  "detalles": [],
  "timestamp": "2026-11-16T15:04:05.123Z",
  "path": "/api/ordenes",
  "correlationId": "5b1f0c1e-..."
}
```

## Excepciones de agro-commons

| Excepción | HTTP | `codigo` | Uso típico |
|---|---|---|---|
| (Bean Validation) `MethodArgumentNotValidException` | 400 | `DATOS_INVALIDOS` | Campos obligatorios, rangos, formatos. `detalles` lista `campo: mensaje` |
| `DatosInvalidosException` | 400 | `DATOS_INVALIDOS` | Validaciones cruzadas (`desde >= hasta`, distancias de zona inválidas) |
| (Spring Security) | 401 | `NO_AUTENTICADO` | Sin token, token vencido o con firma inválida |
| `AccesoRecursoException` | 403 | `ACCESO_DENEGADO` | Rol incorrecto (Gateway) o recurso de otro `sub` (servicio) |
| `RecursoNoEncontradoException` | 404 | `NO_ENCONTRADO` | Recurso inexistente; ruta no publicada (`/internal/**` por el Gateway) |
| `ConflictoEstadoException` | 409 | `CONFLICTO` | Transición inválida (RN-18), habilitación ya resuelta, bloque superpuesto, reserva tomada |
| `ReglaNegocioException` | 422 | `REGLA_NEGOCIO` | Violación de una RN con su motivo |
| `DependenciaNoDisponibleException` | 503 | `DEPENDENCIA_NO_DISPONIBLE` | Georef, Open-Meteo, ms-campos o ms-contratistas no responden o responden algo inesperado |
| (cualquier otra) | 500 | `ERROR_INTERNO` | Error no previsto; el detalle va solo al log |

## Códigos por regla de negocio

| Regla | Rechazo | HTTP |
|---|---|---|
| RN-01 | Matrícula inexistente o no VALIDADA | 403 |
| RN-01 | Matrícula que vence antes de la fecha límite | 422 |
| RN-02 / RN-03 / RN-04 | Carnet o inspección no VALIDADA, vencida en la fecha del trabajo, máquina no PULVERIZADORA | 422 |
| RN-05 | Coordenadas fuera de Argentina; provincia sin parámetros de zona | 422 |
| RN-05 | Georef no disponible (no se guarda un lote sin clasificar) | 503 |
| RN-06 | Lote en zona de EXCLUSION | 422 |
| RN-07 | Categoría toxicológica no permitida en AMORTIGUAMIENTO | 422 |
| RN-08 | Distancias inválidas o categoría inexistente | 400 |
| RN-09 | Ventana no apta (indica la primera hora que falla) | 422 |
| RN-09 | Open-Meteo no responde o respuesta incompleta (falla segura) | 503 |
| RN-10 | Inicio en el pasado, fin fuera del horizonte o posterior a la fecha límite | 422 |
| RN-13 | Bloque de disponibilidad superpuesto (F-06) | 409 |
| RN-13 | Máquina sin disponibilidad para el intervalo (F-13) | 422 |
| RN-13 | Reserva tomada mientras tanto (F-15) | 409 |
| RN-15 | No es el contratista asignado | 403 |
| RN-15 | Hectáreas o tiempos inválidos | 422 |
| RN-17 | Orden ajena | 403 |
| RN-17 | Sin ejecución registrada | 409 |
| RN-18 | Operación no permitida en el estado actual de la orden | 409 |

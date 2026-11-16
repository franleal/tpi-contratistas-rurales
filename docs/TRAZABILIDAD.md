# Trazabilidad de reglas y auditoría final

Matriz de cada regla de negocio con la clase donde se aplica y las pruebas que la verifican.
La suite completa (`.\mvnw.cmd test`) se ejecuta sin Docker ni red.

## Reglas de negocio

| Regla | Dónde se aplica | Pruebas |
| --- | --- | --- |
| RN-01 matrícula del agrónomo validada y vigente | `RecetaService`, `Agronomo` (ms-trabajos) | `RecetaControllerTest`, `LimitesRecetaTest` |
| RN-02 habilitaciones vigentes a la fecha del trabajo | `EvaluadorHabilitacion`, `HabilitacionInternaService` (ms-contratistas); `PostulacionService` | `EvaluadorHabilitacionTest`, `PostulacionControllerTest`, `DatosDemoTest` |
| RN-03 tipos de habilitación por titular | `EvaluadorHabilitacion`, `HabilitacionService` | `EvaluadorHabilitacionTest`, `HabilitacionFechaFuturaTest` |
| RN-04 solo cuentan habilitaciones validadas | `EvaluadorHabilitacion`; revalidación en `AsignacionService` | `HabilitacionFechaFuturaTest`, `PostulacionControllerTest`, `AsignacionControllerTest` |
| RN-05 clasificación del lote por distancia (Georef + Haversine); localidades sin centroide = respuesta incompleta (503) | `ClasificadorZona`, `LoteService`, `GeorefClient` (ms-campos) | `LimitesGeorefHaversineTest`, `FalloSeguroGeorefTest`, `FalloParcialGeorefTest`, `GeorefClientTest`, `LocalidadesSinCentroideTest` |
| RN-06 sin órdenes en zona de exclusión | `OrdenService` (usa `admiteOrdenes` del contrato interno) | `OrdenControllerTest` |
| RN-07 categorías toxicológicas permitidas por zona | `RecetaService` con la copia de categorías de la orden | `RecetaControllerTest`, `LimitesRecetaTest` |
| RN-08 parámetros de zona por provincia | `ParametroZonaService`, `ClasificadorZona`, `GeorefClient` | `AdminZonaControllerTest`, `LimitesGeorefHaversineTest`, `LocalidadesSinCentroideTest` |
| RN-09 ventana climática apta (Open-Meteo; lluvia y ráfaga de la marca H+1 se asignan a la hora [H, H+1)) | `OpenMeteoClient`, `EvaluadorClima`, `VentanaService`, `PostulacionService` | `OpenMeteoClientTest`, `EvaluadorClimaTest`, `VentanaControllerTest`, `FallaSeguraTest` |
| RN-10 horizonte y fecha límite de la postulación | `PostulacionService` | `PostulacionControllerTest` |
| RN-11 duración de la aplicación: superficie / rendimiento (rendimiento > 0 con hasta 2 decimales) | `CalculadoraPostulacion`; `MaquinaRequest` (ms-contratistas) | `CalculadoraPostulacionTest`, `MaquinaControllerTest` |
| RN-12 traslado desde la base: Haversine × factor de ruta / velocidad; la agenda se ocupa desde inicio − traslado | `CalculadoraPostulacion`, `GeoUtils` | `CalculadoraPostulacionTest` |
| RN-13 disponibilidad y reservas de agenda sin superposición; liberación idempotente con reintento acotado | `AgendaService`, `ReservaService` (bloqueo pesimista de la máquina), `AsignacionService.compensar` | `AgendaControllerTest`, `ConcurrenciaAgendaTest`, `AsignacionControllerTest`, `CompensacionAsignacionTest` |
| RN-14 orden de las postulaciones | `ConsultaPostulacionService`, `PostulacionRepository` | `ConsultaPostulacionTest` |
| RN-15 registro de ejecución por el contratista asignado (hectáreas > 0 con hasta 2 decimales) | `EjecucionService` | `EjecucionControllerTest` |
| RN-16 foto climática por hora (con el mismo desfase de lluvia y ráfaga que RN-09); sin clima se registra igual | `EjecucionService.completarClima`, `FotoClima`, `OpenMeteoClient` | `FotoClimaTest`, `FallaSeguraTest` |
| RN-17 confirmación del productor con ejecución registrada | `ConfirmacionService` | `ConfirmacionControllerTest` |
| RN-18 transiciones de estado válidas (409) | `EstadoOrden`, `OrdenTrabajo.avanzarA`, `EstadoPostulacion` | `TransicionesOrdenTest`, `TransicionesApiTest` |

## Fallas obligatorias

| Falla | Comportamiento | Prueba |
| --- | --- | --- |
| Georef no disponible al crear un lote | `503`, el lote no se guarda | `FalloSeguroGeorefTest` |
| Georef devuelve localidades sin centroide | `503`, el lote no se guarda (no se clasifica como LIBRE) | `GeorefClientTest`, `LocalidadesSinCentroideTest` |
| Open-Meteo o ms-contratistas no disponible al postularse | `503`, la postulación no se guarda | `FallaSeguraTest`, `PostulacionControllerTest` |
| Ejecución sin consulta climática | Se registra con `climaDisponible = false` y sin fotos | `FotoClimaTest`, `FallaSeguraTest` |
| Reserva confirmada y falla local | Se libera la reserva (compensación) y la orden queda ABIERTA | `CompensacionAsignacionTest`, `CompensacionIntegracionTest` |
| Falla local y la liberación también falla | Hasta 3 intentos de `DELETE` (idempotente); si siguen fallando, log `LIBERACION_PENDIENTE reservaId=…` y `503` que informa la reserva pendiente | `CompensacionAsignacionTest` |

## Seguridad

- **Gateway:** rol por ruta y método (`SecurityConfig`), `401` sin token y `403` con rol insuficiente; `/internal/**` no se enruta (`GatewaySecurityTest`, `GatewayRoutesTest`).
- **Microservicios:** cada uno valida el JWT y controla la propiedad con el `sub` (`PropiedadRecursosTest`, `OrdenControllerTest`, `ConfirmacionControllerTest`).
- **SEG-01, visibilidad de órdenes:** el detalle aplica la misma política que el listado. Un contratista solo ve órdenes ABIERTAS o asignadas a él; otra orden responde `403` (`ConsultaOrdenService.detalle`, `ConsultaOrdenTest`).
- **Entre servicios:** el JWT del usuario se propaga a `/internal` (`PropagacionJwtInterceptor`).

## Revisión de consistencia

- Rutas del Gateway contra controladores: todas las rutas `/api` documentadas en el README existen y tienen regla de rol.
- Referencias a reglas en el README y en la colección Postman revisadas contra las clases (por ejemplo, la agenda es RN-13 y la clasificación del lote es RN-05/RN-08).
- RN-11 y RN-12: la matriz tenía invertidas duración y traslado respecto de `CalculadoraPostulacion`, el README y los diagramas P4b/P4c. Se corrigió: RN-11 es la duración por rendimiento y RN-12 el traslado.
- Los `data.sql` usan los `sub` fijos del realm y se cargan en pruebas (`DatosDemoTest` en cada servicio).
- Cada contexto de prueba usa su propia base H2 en memoria, para que al cerrar uno no se borren las tablas de otro.

## Pendiente fuera del código

- Levantar el Compose completo en un entorno limpio y correr la colección Postman (no verificado en esta etapa).

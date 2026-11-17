# Decisiones de diseño

Cada decisión indica la alternativa simple elegida, por qué, qué se descartó y a qué regla o criterio responde.

## D-01. Microservicios por dueño de datos

- **Decisión:** tres servicios (ms-campos, ms-contratistas, ms-trabajos) más el Gateway. ms-trabajos orquesta la orden; los otros dos son dueños de sus datos y exponen `/internal`.
- **Por qué:** cada regla vive en el servicio dueño de los datos que necesita (RN-05 a RN-08 en campos, RN-02 a RN-04 y RN-13 en contratistas, el resto en trabajos).
- **Descartado:** un servicio por entidad (demasiadas llamadas síncronas) y un monolito (no cumple la consigna).

## D-02. Firma agronómica sin criptografía

- **Decisión:** "firmar" la receta es registrarla autenticado como AGRONOMO con matrícula VALIDADA y vigente hasta la fecha límite (RN-01). Se guardan el agrónomo y la marca de tiempo (`firmada_en`).
- **Por qué:** la consigna no exige firma digital; la identidad la garantiza el JWT de Keycloak.
- **Descartado:** firma con certificado X.509 (fuera de alcance).

## D-03. Horas climáticas frente a horas de traslado

- **Decisión:** el clima se evalúa solo en las horas de **aplicación** (`inicio` a `fin`) más las N horas sin lluvia posteriores (RN-09). El traslado (RN-12) ocupa la agenda de la máquina pero no se evalúa climáticamente.
- **Por qué:** durante el traslado no se pulveriza; la deriva solo importa al aplicar.

## D-04. Zona horaria y redondeo

- **Decisión:** todas las fechas son `LocalDateTime` en `America/Argentina/Buenos_Aires` (fijada por `JAVA_TOOL_OPTIONS` en cada contenedor y pedida explícitamente a Open-Meteo con `timezone=`). Argentina no tiene horario de verano, así que no hay horas ambiguas.
- **Redondeo:** las horas de aplicación y de traslado se calculan en minutos y se redondean **hacia arriba** al minuto. La evaluación climática toma cada hora que se superpone con el intervalo (la hora del inicio truncada hasta la hora anterior al fin).

## D-05. Contratos REST internos

- **Decisión:** ver [CONTRATOS.md](CONTRATOS.md). Cada servicio define sus DTOs (records) y no se comparten por agro-commons.
- **Por qué:** compartir DTOs acopla los despliegues: un cambio en uno obliga a recompilar el otro.

## D-06. Vigencia calculada, no guardada

- **Decisión:** habilitaciones y matrículas solo guardan `PENDIENTE`, `VALIDADA` o `RECHAZADA` y su vencimiento. Si están vigentes **en la fecha del trabajo** se calcula en cada operación (`Vigencia.cubre(vencimiento, fecha)`), y se revalida al asignar (RN-04).
- **Descartado:** un estado `VENCIDA` actualizado por un job diario (quedaría desfasado y depende de la fecha de hoy, no de la del trabajo).

## D-07. Reservas con estado

- **Decisión:** la reserva no se borra al compensar: pasa a `LIBERADA`. Solo las `ACTIVA` ocupan agenda. El `DELETE` interno es idempotente.
- **Por qué:** deja trazabilidad de la compensación y permite reintentar sin errores.

## D-08. Control de concurrencia en la agenda

- **Decisión:** la reserva se crea en una transacción que toma un bloqueo pesimista (`SELECT ... FOR UPDATE`) sobre la fila de la máquina antes de verificar superposiciones. Dos asignaciones simultáneas sobre la misma máquina se serializan y la segunda recibe 409.
- **Descartado:** constraint de exclusión de PostgreSQL con `tsrange` (más robusto pero ata el código a Postgres y complica las pruebas con H2).

## D-09. Falla segura

- **Decisión:** sin pronóstico no se registra una postulación (503). Sin clima real la ejecución se registra igual con `climaDisponible = false` (RN-16). Sin Georef no se guarda el lote (503).
- **Por qué:** nunca se habilita una pulverización sin datos; pero un trabajo ya hecho debe quedar registrado.

## D-10. Clasificación de zona aproximada

- **Decisión:** distancia Haversine al **centroide** de las localidades del mismo departamento (RN-08).
- **Limitación declarada:** no es el límite real del ejido ni considera localidades de departamentos vecinos.

## D-11. Seguridad repartida

- **Decisión:** el Gateway valida el JWT y el **rol** por ruta y método; cada servicio vuelve a validar el JWT y aplica la **propiedad** por `sub`. Las rutas `/internal/**` no se rutean en el Gateway.
- **Por qué:** el Gateway no conoce los datos para decidir propiedad; los servicios no repiten la matriz de roles.

## D-12. Validación documental de habilitaciones

- **Decisión:** el ADMIN aprueba o rechaza carnets, inspecciones y matrículas. No hay API pública de registros oficiales; la lógica queda detrás de la interfaz `ValidadorHabilitaciones` para poder sumar otra implementación.

## Pendiente con la cátedra

- Confirmar el alcance V2 y la rúbrica propuesta (tarjeta 00-02).

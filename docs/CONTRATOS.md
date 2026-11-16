# Contratos REST internos

Contratos que ms-trabajos consume de ms-campos y ms-contratistas. Cada servicio define **su propio DTO** para el contrato que consume o expone: no se comparten clases (ni entidades JPA ni DTOs) a través de agro-commons.

## Reglas generales

- Las rutas `/internal/**` **no se publican en el Gateway**. Solo se invocan dentro de la red interna de Docker Compose.
- ms-trabajos **propaga el JWT del usuario** (`Authorization: Bearer ...`) y el `X-Correlation-Id`. El servicio llamado valida el token, pero no exige un rol: devuelve el `sub` del dueño del recurso para que ms-trabajos aplique la regla de propiedad.
- Fechas y horas en ISO-8601 **sin zona** (`2026-11-20T08:00:00`), interpretadas en `America/Argentina/Buenos_Aires` (variable `JAVA_TOOL_OPTIONS` del compose).
- Los errores usan el formato común de agro-commons (ver [ERRORES.md](ERRORES.md)).
- Timeouts del cliente: conexión 2 s y lectura 5 s (configurables). Un timeout, un error de conexión o un 5xx se traducen en ms-trabajos a **503**.

---

## 1. ms-trabajos → ms-campos

### `GET /internal/lotes/{id}`

Datos del lote para crear la orden (F-10, RN-06). ms-trabajos copia estos datos en la orden.

Respuesta `200`:

```json
{
  "id": 7,
  "productorSub": "11111111-0000-0000-0000-000000000001",
  "nombre": "Lote Norte",
  "lat": -31.512345,
  "lon": -63.987654,
  "superficieHa": 120.50,
  "provincia": "Córdoba",
  "departamento": "Río Segundo",
  "zona": "AMORTIGUAMIENTO",
  "categoriasPermitidas": ["III", "IV"]
}
```

| Código | Cuándo | Qué hace ms-trabajos |
|---|---|---|
| 200 | Lote encontrado | Verifica `productorSub == sub` (si no, 403) y que `zona != EXCLUSION` (si no, 422) |
| 401 | Token inválido | 503 (no debería ocurrir: el token ya fue validado) |
| 404 | Lote inexistente | 404 |
| timeout / 5xx | ms-campos caído | 503, la orden no se crea |

---

## 2. ms-trabajos → ms-contratistas

### `GET /internal/maquinas/{id}/habilitacion?aplicadorId={id}&fecha={yyyy-MM-dd}`

Evalúa RN-02, RN-03 y RN-04 en la fecha **del trabajo** (no la de hoy). Se usa al postularse (F-13) y se repite al asignar (F-15).

Respuesta `200` (siempre 200 si máquina y aplicador existen; la decisión va en `habilitado`):

```json
{
  "maquinaId": 3,
  "aplicadorId": 5,
  "fecha": "2026-11-20",
  "habilitado": false,
  "motivo": "El carnet del aplicador vence el 2026-11-18, antes de la fecha del trabajo",
  "tipoMaquina": "PULVERIZADORA",
  "rendimientoHaH": 25.00,
  "baseLat": -31.40,
  "baseLon": -64.18,
  "contratistaSub": "22222222-0000-0000-0000-000000000001"
}
```

Motivos posibles (`habilitado = false`): máquina de tipo distinto a PULVERIZADORA; aplicador de otra empresa; inspección o carnet inexistente, PENDIENTE, RECHAZADA o vencido antes de la fecha.

| Código | Cuándo | Qué hace ms-trabajos |
|---|---|---|
| 200 | Evaluado | Si `contratistaSub != sub` → 403. Si `habilitado = false` → 422 con el motivo |
| 404 | Máquina o aplicador inexistente | 404 |
| timeout / 5xx | Caído | 503, la postulación no se registra |

### `GET /internal/maquinas/{id}/disponibilidad?desde={t}&hasta={t}`

RN-13. El intervalo `[desde, hasta)` es el bloque ocupado: `inicio − traslado` hasta `fin`.

```json
{ "maquinaId": 3, "desde": "2026-11-20T06:30:00", "hasta": "2026-11-20T13:00:00", "disponible": false,
  "motivo": "El intervalo se superpone con la reserva 12" }
```

| Código | Cuándo | Qué hace ms-trabajos |
|---|---|---|
| 200 | Evaluado | `disponible = false` → 422 |
| 400 | `desde >= hasta` | 503 (error de programación del llamador) |
| 404 | Máquina inexistente | 404 |
| timeout / 5xx | Caído | 503 |

### `POST /internal/agenda/reservas`

Reserva el intervalo al asignar (F-15). La verificación y la inserción ocurren en una transacción con bloqueo pesimista sobre la máquina, de modo que dos asignaciones simultáneas no pueden reservar intervalos superpuestos.

Pedido:

```json
{ "maquinaId": 3, "ordenId": 10, "desde": "2026-11-20T06:30:00", "hasta": "2026-11-20T13:00:00" }
```

Respuesta `201`:

```json
{ "id": 12, "maquinaId": 3, "ordenId": 10, "desde": "2026-11-20T06:30:00", "hasta": "2026-11-20T13:00:00", "estado": "ACTIVA" }
```

| Código | Cuándo | Qué hace ms-trabajos |
|---|---|---|
| 201 | Reserva creada | Guarda la asignación; si el guardado falla, **compensa** con DELETE |
| 409 | Se superpone con otra reserva activa | 409, la orden sigue ABIERTA |
| 422 | El intervalo no cae dentro de un bloque de disponibilidad | 409 (la agenda cambió desde la postulación), la orden sigue ABIERTA |
| 404 | Máquina inexistente | 404 |
| timeout / 5xx | Caído | 503, la orden sigue ABIERTA |

### `DELETE /internal/agenda/reservas/{id}`

Compensación: libera la reserva (pasa a estado `LIBERADA`, no se borra la fila, para dejar trazabilidad).

| Código | Cuándo |
|---|---|
| 204 | Liberada, o ya estaba liberada (idempotente) |
| 404 | Reserva inexistente |

Si la compensación también falla, ms-trabajos registra el error en el log con el `reservaId` (queda una reserva huérfana a liberar manualmente) y responde 503 igual.

---

## 3. Integraciones externas

| Servicio | API | Uso | Falla |
|---|---|---|---|
| ms-campos | Georef `GET /ubicacion?lat&lon` | Provincia y departamento del punto | 503, el lote no se guarda |
| ms-campos | Georef `GET /localidades?provincia&departamento&campos=id,nombre,centroide&max=5000` | Centroides para Haversine | 503, el lote no se guarda |
| ms-trabajos | Open-Meteo `GET /v1/forecast?hourly=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_gusts_10m&start_date&end_date&timezone=America/Argentina/Buenos_Aires` | Pronóstico (ventanas, postulación) y clima de horas pasadas (ejecución) | Postulación y ventanas: 503 (falla segura). Ejecución: se registra con `climaDisponible = false` |

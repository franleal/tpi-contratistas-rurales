# Contratos REST internos (borrador)

Definir las entradas, salidas y errores antes de comenzar la implementación en paralelo. No compartir DTOs o entidades JPA.

- ms-trabajos → ms-campos: GET /internal/lotes/{id} (propiedad del productor, superficie, coordenadas, zona).
- ms-trabajos → ms-contratistas: GET /internal/maquinas/{id}/habilitacion?fecha=&aplicadorId=.
- ms-trabajos → ms-contratistas: POST /internal/agenda/reservas.
- ms-trabajos → ms-contratistas: DELETE /internal/agenda/reservas/{id} (compensación).

Propagar JWT del usuario en las llamadas internas y validar identidad/propiedad donde aplique. Las rutas /internal/** no se publican en Gateway.

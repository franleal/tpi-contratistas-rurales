# Diagramas

Fuentes PlantUML (`.puml`). Se pueden ver con el plugin PlantUML de IntelliJ/VS Code o en
https://www.plantuml.com/plantuml pegando el contenido.

| Archivo | Entrega | Contenido |
|---|---|---|
| `P4a-estados-orden.puml` | P4a | Máquina de estados de la orden (RECETA_PENDIENTE → ABIERTA → ASIGNADA → EJECUTADA → COMPLETADA) y RN-18. |
| `P4b-proceso-actividad.puml` | P4b | Proceso completo de punta a punta con los actores (productor, agrónomo, contratista, ADMIN). |
| `P4c-secuencia-postulacion.puml` | P4c | Secuencia de la postulación (F-13): Gateway, ms-trabajos, ms-contratistas y Open-Meteo, con la falla segura. |
| `P5-c1-contexto.puml` | P5 | C4 nivel 1: sistema, actores y servicios externos (Keycloak, Georef, Open-Meteo). |
| `P6-c2-contenedores.puml` | P6 | C4 nivel 2: Gateway, microservicios, PostgreSQL con un esquema por servicio. |
| `P8-er-persistencia.puml` | P8 | Modelo de datos por esquema; las referencias entre servicios son ids sin FK. |

# TPI Backend 2026 — Contratistas rurales

Proyecto de Backend de Aplicaciones — UTN FRC.

Java 21 · Spring Boot 3 · Maven · Spring Cloud Gateway · Keycloak · PostgreSQL · Docker Compose

> Esta es la estructura inicial para organizar el trabajo en equipo. Todavía no contiene microservicios funcionales ni contratos internos definitivos.

## Objetivo

Desarrollar una plataforma para gestionar trabajos de pulverización terrestre: lotes, clasificación de zonas, habilitaciones, recetas agronómicas, postulaciones, validación meteorológica, disponibilidad de agenda, ejecución y confirmación.

## Arquitectura

- `ms-campos`: productores, establecimientos, lotes y clasificación Georef (RN-05 a RN-08).
- `ms-contratistas`: contratistas, máquinas, aplicadores, habilitaciones y agenda (RN-02 a RN-04, RN-13).
- `ms-trabajos`: agrónomos, órdenes, recetas, postulaciones, clima, asignaciones y ejecuciones (RN-01, RN-09 a RN-12, RN-14 a RN-17).
- `api-gateway`: ruteo, autenticación y control de rol.
- `agro-commons`: errores y utilidades técnicas compartidas, nunca entidades JPA, DTOs ni negocio.
- `docs`: contratos, decisiones y diagramas.

Una instancia de PostgreSQL con esquema y usuario por servicio. Servicios y base sin puertos externos; Gateway como entrada.

## Equipo de cinco

1. Responsable ms-campos
2. Responsable ms-contratistas
3. Responsable ms-trabajos A: órdenes, agrónomos, recetas, postulaciones
4. Responsable infraestructura: Gateway, Keycloak, Compose, PostgreSQL, commons
5. Responsable ms-trabajos B: Open-Meteo, ventana climática, agenda, ejecución e integración

## Trabajo en equipo y pull requests

- Una rama por tarea: `feature/<area>-<tarea>`.
- Proponer cada cambio mediante PR hacia `main`; no hacer push directo.
- Revisión cruzada y aprobación final por `@franleal` antes del merge.
- Sin merges automáticos.
- Cada PR indica regla RN, criterio de rúbrica, justificación, cómo se probó y riesgos.
- La aprobación obligatoria **debe configurarse en GitHub Settings → Rules → Rulesets**; este README y CODEOWNERS solo documentan la política.

## Fallas obligatorias

- Postulación sin Open-Meteo o ms-contratistas: responder 503 sin guardarla.
- Lote sin clasificar por falla de Georef: responder 503 sin guardarlo.
- Ejecución sin consulta climática: registrar ejecución con clima NO_DISPONIBLE.
- Reserva de agenda confirmada seguida de falla local: compensar liberándola.

## Pendientes iniciales

Acordar contratos REST internos, matrices de roles, modelos JPA y condiciones de aceptación de tarjetas. Confirmar con docente el alcance V2 y la rúbrica propuesta (105 puntos, pendiente de aprobación formal).

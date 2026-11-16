# Modelo de datos de los tres servicios

Una instancia de PostgreSQL 16 con **un esquema y un usuario por microservicio**. Cada usuario es dueño solo de su esquema (`infra/db/init.sql`), así que un servicio no puede leer tablas de otro aunque compartan el motor. Keycloak usa su propio esquema.

Diagrama editable: [`diagramas/P8-er-persistencia.puml`](diagramas/P8-er-persistencia.puml).

| Esquema | Administrado por | Entidades |
|---|---|---|
| campos | ms-campos (`campos_app`) | productor, establecimiento, lote, parametro_zona |
| contratistas | ms-contratistas (`contratistas_app`) | contratista, maquina, aplicador, habilitacion, bloque_disponibilidad, reserva |
| trabajos | ms-trabajos (`trabajos_app`) | agronomo, orden_trabajo, receta, postulacion, asignacion, registro_ejecucion, foto_clima, umbral_climatico |
| keycloak | Keycloak | tablas propias |

## Esquema campos

| Tabla | Columnas clave | Notas |
|---|---|---|
| productor | id PK, sub UK, nombre, email | Se crea en el primer `POST /api/establecimientos` con los datos del JWT |
| establecimiento | id PK, productor_id FK, nombre, provincia | |
| lote | id PK, establecimiento_id FK, nombre, lat, lon, superficie_ha, cultivo, provincia, departamento, zona, localidad_cercana, distancia_localidad_m | `zona` ∈ EXCLUSION, AMORTIGUAMIENTO, LIBRE. Nunca se guarda un lote sin zona |
| parametro_zona | provincia PK, distancia_exclusion_m, distancia_amortiguamiento_m, categorias_permitidas | Categorías como texto `"III,IV"` |

## Esquema contratistas

| Tabla | Columnas clave | Notas |
|---|---|---|
| contratista | id PK, sub UK, razon_social, cuit UK | |
| maquina | id PK, contratista_id FK, tipo, descripcion, ancho_m, rendimiento_ha_h, base_lat, base_lon | `tipo` ∈ PULVERIZADORA, OTRA |
| aplicador | id PK, contratista_id FK, nombre, documento | |
| habilitacion | id PK, tipo, aplicador_id FK nulo, maquina_id FK nulo, numero, vencimiento, estado, motivo_rechazo | Exactamente un titular según `tipo`. `estado` ∈ PENDIENTE, VALIDADA, RECHAZADA |
| bloque_disponibilidad | id PK, maquina_id FK, desde, hasta | Sin superposición entre bloques de la misma máquina |
| reserva | id PK, maquina_id FK, orden_id (externo), desde, hasta, estado | `estado` ∈ ACTIVA, LIBERADA. Solo las ACTIVA ocupan agenda |

## Esquema trabajos

| Tabla | Columnas clave | Notas |
|---|---|---|
| agronomo | id PK, sub UK, nombre, matricula, provincia, vencimiento, estado, motivo_rechazo | |
| orden_trabajo | id PK, productor_sub, lote_id (externo), lote_lat, lote_lon, superficie_ha, zona, provincia, categorias_permitidas, fecha_limite, estado, creada_en | Copia mínima del lote |
| receta | id PK, orden_id FK UK, agronomo_id FK, producto, categoria_toxicologica, dosis, firmada_en | |
| postulacion | id PK, orden_id FK, contratista_sub, maquina_id, aplicador_id (externos), inicio, fin, ocupado_desde, horas_aplicacion, traslado_h, precio_ha, estado, creada_en | `estado` ∈ VALIDA, ASIGNADA, DESCARTADA |
| asignacion | id PK, orden_id FK UK, postulacion_id FK UK, reserva_id (externo), asignada_en | |
| registro_ejecucion | id PK, orden_id FK UK, inicio, fin, hectareas, clima_disponible, fuera_de_condiciones, registrada_en, confirmada_en | |
| foto_clima | id PK, ejecucion_id FK, hora, viento_kmh, rafaga_kmh, precipitacion_mm, temperatura_c, humedad_pct, apta | Una fila por hora trabajada |
| umbral_climatico | id PK (fila única), viento_min_kmh, viento_max_kmh, rafaga_max_kmh, temperatura_max_c, humedad_min_pct, horas_sin_lluvia_despues, horizonte_dias | `horizonte_dias` ≤ 16 |

## Cardinalidades

productor 1–N establecimiento · establecimiento 1–N lote · contratista 1–N maquina · contratista 1–N aplicador · maquina 0..1–N habilitacion · aplicador 0..1–N habilitacion · maquina 1–N bloque_disponibilidad · maquina 1–N reserva · orden_trabajo 1–0..1 receta · agronomo 1–N receta · orden_trabajo 1–N postulacion · orden_trabajo 1–0..1 asignacion · postulacion 1–0..1 asignacion · orden_trabajo 1–0..1 registro_ejecucion · registro_ejecucion 1–N foto_clima.

## Decisiones de persistencia

- **Referencias entre servicios solo por id** (`orden_trabajo.lote_id`, `postulacion.maquina_id`, `asignacion.reserva_id`, `reserva.orden_id`), sin claves foráneas entre esquemas. La integridad se verifica con las llamadas `/internal` en cada operación.
- **Copia mínima del lote en la orden**: coordenadas, superficie, provincia, zona y categorías permitidas. La orden se lee y la receta se firma sin depender de ms-campos, y queda registrado con qué datos se decidió.
- **La vigencia no se guarda**: se calcula comparando el vencimiento con la fecha del trabajo (`Vigencia` de agro-commons).
- **Disponibilidad y reservas son tablas distintas**: el contratista declara bloques; ms-trabajos crea reservas dentro de ellos. La superposición se controla con bloqueo pesimista sobre la fila de la máquina.
- **Generación del esquema**: Hibernate crea las tablas (`ddl-auto`, por defecto `create` en la demo) y cada servicio carga `data.sql` con fechas relativas a `CURRENT_DATE`.

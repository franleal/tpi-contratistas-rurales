# TPI 2026 — Contratistas rurales

Backend de Aplicaciones — UTN FRC. Proyecto de backend basado en microservicios para la gestión de contratistas rurales.

> **Estado:** funcionalidades F-01 a F-17 y reglas RN-01 a RN-18 implementadas con pruebas automatizadas. Entorno de **desarrollo local**, no apto para producción.

## Stack

- Java 21, Spring Boot 3.5.16 y Maven.
- Spring Cloud Gateway reactivo (WebFlux).
- PostgreSQL 16, una base con esquemas independientes.
- Keycloak 26.4.0 (OIDC/JWT).
- Docker y Docker Compose.
- `agro-commons`: JAR Maven para componentes técnicos transversales; no comparte entidades ni reglas de negocio.

## Arquitectura

| Componente | Responsabilidad | Acceso desde la PC |
| --- | --- | --- |
| `gateway` | Entrada HTTP, rutas y validación JWT básica | http://localhost:8080 |
| `keycloak` | Identidad, realm `agro`, emisión de JWT | http://localhost:8081 |
| `ms-campos` | Dominio de campos y lotes | Solo red Docker |
| `ms-contratistas` | Dominio de contratistas | Solo red Docker |
| `ms-trabajos` | Dominio de trabajos | Solo red Docker |
| `postgres` | Persistencia, esquemas aislados | Solo red Docker (`localhost:5432` solo con `docker-compose.dev.yml`) |

PostgreSQL contiene cuatro esquemas con usuarios propios: `campos`/`campos_app`, `contratistas`/`contratistas_app`, `trabajos`/`trabajos_app` y `keycloak`/`keycloak`.

**Trusted zone:** solo el Gateway (8080) y Keycloak (8081) publican puertos, y solo en `127.0.0.1`. Los tres microservicios y PostgreSQL quedan en la red interna de Docker sin puertos expuestos. El puerto de PostgreSQL se publica únicamente con `docker-compose.dev.yml`, para el modo IntelliJ.

Para demostrar las fallas de integración hay dos overrides en `infra/compose/`: `docker-compose.falla-meteo.yml` (Open-Meteo inaccesible) y `docker-compose.falla-georef.yml` (Georef inaccesible).

## Requisitos previos

- Git.
- Docker Desktop con Docker Engine activo y Docker Compose.
- Puertos locales **8080** y **8081** disponibles (y **5432** solo para el modo IntelliJ).

**No es necesario instalar Java ni Maven para levantar el proyecto con Docker**: las imágenes construyen los JAR dentro de sus etapas Maven. Para ejecutar proyectos desde IntelliJ sí se necesita JDK 21.

## Primer arranque (Windows / PowerShell)

1. Clonar el repositorio y entrar a su raíz:

   ```powershell
   git clone https://github.com/franleal/tpi-contratistas-rurales.git
   cd tpi-contratistas-rurales
   ```

   Para verificar un PR antes del merge, cambiar a su rama antes de continuar.

2. Crear el archivo local de variables:

   ```powershell
   Copy-Item .env.example .env
   ```

   Abrir `.env` y reemplazar los valores `change-me` con contraseñas de desarrollo **distintas**. Variables requeridas:

   | Variable | Uso |
   | --- | --- |
   | `POSTGRES_PASSWORD` | Usuario administrador de PostgreSQL |
   | `CAMPOS_DB_PASSWORD` | Usuario `campos_app` |
   | `CONTRATISTAS_DB_PASSWORD` | Usuario `contratistas_app` |
   | `TRABAJOS_DB_PASSWORD` | Usuario `trabajos_app` |
   | `KC_DB_PASSWORD` | Usuario de base de datos `keycloak` |
   | `KEYCLOAK_ADMIN_PASSWORD` | Usuario `admin` del panel Keycloak |

   Variables opcionales (tienen valor por defecto en `docker-compose.yml`):

   | Variable | Defecto | Uso |
   | --- | --- | --- |
   | `GEOREF_URL` | `https://apis.datos.gob.ar/georef/api` | API de Georef (ms-campos) |
   | `OPEN_METEO_URL` | `https://api.open-meteo.com` | API de Open-Meteo (ms-trabajos) |
   | `HTTP_CONNECT_TIMEOUT_MS` / `HTTP_READ_TIMEOUT_MS` | `2000` / `8000` | Timeouts de los clientes HTTP |
   | `TRASLADO_FACTOR_RUTA` / `TRASLADO_VELOCIDAD_KMH` | `1.3` / `40` | Cálculo del traslado (RN-12) |
   | `DDL_AUTO` / `SQL_INIT_MODE` | `create` / `always` | Recrear el esquema y cargar `data.sql` en cada arranque |

   Cada servicio lee su configuración de `application.properties` con placeholders `${VARIABLE:defecto}`: los defectos sirven para IntelliJ (`localhost`) y Compose los reemplaza con los nombres de la red interna.

   **No publicar `.env` ni contraseñas reales.** Verificar que Git lo ignore:

   ```powershell
   git check-ignore .env
   ```

3. Validar la configuración:

   ```powershell
   docker compose config --quiet
   ```

4. Construir y levantar todos los servicios:

   ```powershell
   docker compose up -d --build
   ```

5. Consultar el estado:

   ```powershell
   docker compose ps
   ```

   Se esperan **seis servicios** en estado `healthy`. El arranque es reproducible porque cada contenedor tiene healthcheck y el orden se basa en ellos: `postgres` → `keycloak` y los tres microservicios (`/actuator/health`, que incluye la conexión a la base) → `gateway`. El primer arranque tarda unos minutos mientras se compilan las imágenes. Ante dudas, revisar los logs:

   ```powershell
   docker compose logs --tail 50 gateway
   docker compose logs --tail 50 keycloak
   docker compose logs --tail 50 ms-campos
   ```

### Keycloak

Panel: http://localhost:8081/admin/

- Usuario administrador inicial: `admin`.
- Contraseña: valor local de `KEYCLOAK_ADMIN_PASSWORD`.
- Realm de negocio: **`agro`**.
- Roles: `PRODUCTOR`, `CONTRATISTA`, `AGRONOMO`, `ADMIN`.
- Cliente de pruebas: `agro-test` (Direct access grants; solo desarrollo).

Al iniciar sobre una base vacía, Compose crea los esquemas de PostgreSQL y Keycloak importa la configuración desde `keycloak/agro-realm.json`. **Si el realm ya existe, Keycloak omite la importación en el arranque.**

El realm se importa con **siete usuarios de prueba** con `id` fijo (así los `data.sql` pueden referenciar su `sub`). Todos usan la contraseña de demostración `agro2026`, que **solo existe para la demo local** (enunciado, pregunta 10):

| Usuario | Rol | `sub` |
| --- | --- | --- |
| `productor1` | PRODUCTOR | `11111111-0000-0000-0000-000000000001` |
| `productor2` | PRODUCTOR | `11111111-0000-0000-0000-000000000002` |
| `contratista1` | CONTRATISTA | `22222222-0000-0000-0000-000000000001` |
| `contratista2` | CONTRATISTA | `22222222-0000-0000-0000-000000000002` |
| `agronomo1` | AGRONOMO | `33333333-0000-0000-0000-000000000001` |
| `agronomo2` | AGRONOMO (matrícula pendiente en `data.sql`) | `33333333-0000-0000-0000-000000000002` |
| `admin1` | ADMIN | `44444444-0000-0000-0000-000000000001` |

El token dura 30 minutos (`accessTokenLifespan = 1800`) y lleva los roles en `realm_access.roles`. El export **no incluye material de claves**: Keycloak genera claves propias en cada instalación nueva (SEG-01). Si se cambia el realm, borrar el volumen (`docker compose down -v`) para que se vuelva a importar.

## Pruebas rápidas

### 1. Gateway saludable

```powershell
curl.exe -s -i http://localhost:8080/actuator/health
```

Esperado: `HTTP 200` y `{"status":"UP"}`.

### 2. Endpoint protegido sin JWT

```powershell
curl.exe -s -i http://localhost:8080/api/lotes/15
```

Esperado: `401 Unauthorized` con `WWW-Authenticate: Bearer`.

### 3. Obtener un JWT de prueba

En PowerShell, con uno de los usuarios de prueba del realm (ejemplo: `productor1`):

```powershell
$credencial = Get-Credential -UserName "productor1" -Message "Contraseña de Keycloak"

$respuesta = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8081/realms/agro/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id  = "agro-test"
        username   = $credencial.UserName
        password   = $credencial.GetNetworkCredential().Password
    }

$token = $respuesta.access_token
$token.Split('.').Count
```

La última instrucción debe mostrar `3` (formato JWT). No pegar el token ni la contraseña en chats, issues o capturas. El token de prueba observado expira en aproximadamente cinco minutos.

### 4. Pasar por el Gateway con JWT

En la **misma sesión de PowerShell**:

```powershell
curl.exe -s -i -H "Authorization: Bearer $token" "http://localhost:8080/api/lotes/15"
```

Esperado: `200` con el lote 15 si pertenece a `productor1`, `404` si no existe y `403` si es de otro productor. Con los datos de demostración, probar con `/api/lotes/1` (propio) y `/api/lotes/4` (de `productor2`).

Si el Gateway responde `invalid_token`/`Bearer token is malformed`, verificar que `$token` esté definido en esa sesión y obtener otro si expiró. Si rechaza un token bien formado, revisar los logs y la coherencia entre el `iss` público (`http://localhost:8081/realms/agro`) y el JWKS interno de Docker.

## Detener, iniciar y reconstruir

```powershell
# Detener, conservando los datos
docker compose down

# Volver a iniciar
docker compose up -d

# Reconstruir las imágenes después de modificar código Java
docker compose up -d --build

# Inspeccionar logs de un servicio
docker compose logs --tail 80 gateway
```

**No ejecutar `docker compose down -v` salvo que se quiera eliminar deliberadamente el volumen de PostgreSQL.** Al destruir ese volumen se pierden datos, usuarios y configuración persistida de Keycloak.

Cambiar `.env` después de la primera inicialización **no cambia automáticamente las contraseñas de los roles ya creados dentro de PostgreSQL**. Reconciliar las credenciales existentes si hace falta cambiarlas.

## Ejecutar desde IntelliJ (desarrollo local)

1. Preparar `.env` con el paso 2 de [Primer arranque](#primer-arranque-windows--powershell). Si está corriendo el Compose completo, detenerlo primero con `docker compose down` para liberar el puerto `8080` y conservar los datos. Levantar PostgreSQL y Keycloak desde la raíz del repositorio:

   ```powershell
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres keycloak
   docker compose ps
   ```

   Esperar a que PostgreSQL figure como `healthy` y que Keycloak responda en `http://localhost:8081`. El puerto local `5432` tiene que estar libre.

2. Abrir la raíz del repositorio en IntelliJ, elegir **JDK 21** en **File → Project Structure → Project** y agregar los `pom.xml` de `agro-commons`, `api-gateway`, `ms-campos`, `ms-contratistas` y `ms-trabajos` desde la ventana **Maven → Add Maven Project**.
3. Instalar `agro-commons` en el repositorio Maven local con **Maven → agro-commons → Lifecycle → install** y luego recargar los proyectos Maven. Los tres microservicios usan `ar.edu.tpi:agro-commons:1.0.0`. Como alternativa al paso `install` de IntelliJ, desde PowerShell con JDK 21:

   ```powershell
   .\api-gateway\mvnw.cmd -f .\agro-commons\pom.xml install
   ```

   La raíz del repositorio tiene un `pom.xml` agregador (multi-proyecto) y su propio Maven Wrapper. Para compilar y probar todo el monorepo de una vez (agro-commons se construye primero):

   ```powershell
   .\mvnw.cmd install            # compila, prueba e instala agro-commons en .m2
   .\mvnw.cmd -pl ms-campos -am verify   # solo un servicio y lo que necesita
   ```

   En IntelliJ alcanza con abrir el `pom.xml` de la raíz: importa los cinco módulos.

4. Crear una configuración de ejecución para cada clase `*Application`. En **Run → Edit Configurations**, usar `-Duser.timezone=America/Argentina/Buenos_Aires` en **VM options** y copiar la contraseña correspondiente del `.env` a `DB_PASSWORD` en **Environment variables**. Compose lee `.env` desde la raíz y las configuraciones de IntelliJ se completan por separado.

   | Clase | Puerto | Valor para `DB_PASSWORD` |
   | --- | ---: | --- |
   | `MsCamposApplication` | `8082` | `CAMPOS_DB_PASSWORD` |
   | `MsContratistasApplication` | `8083` | `CONTRATISTAS_DB_PASSWORD` |
   | `MsTrabajosApplication` | `8084` | `TRABAJOS_DB_PASSWORD` |
   | `ApiGatewayApplication` | `8080` | — |

   Los tres servicios tienen `localhost:5432` como destino por defecto. Cada uno ya define su propio esquema y usuario de base.

5. Ejecutar el microservicio desde IntelliJ y comprobar `/actuator/health` en su puerto. Por ejemplo, para `ms-campos`:

   ```powershell
   curl.exe -s -i http://localhost:8082/actuator/health
   ```

   El resultado esperado es `HTTP 200` con `{"status":"UP"}`. Para cerrar el entorno local, detener las aplicaciones en IntelliJ y ejecutar `docker compose down`.

## Estructura principal

```text
api-gateway/             # Enrutamiento y seguridad de entrada
ms-campos/               # Dominio de campos
ms-contratistas/         # Dominio de contratistas
ms-trabajos/             # Dominio de trabajos
agro-commons/            # Utilidades técnicas compartidas
infra/db/init.sql        # Creación inicial de usuarios/esquemas
keycloak/agro-realm.json # Realm agro con roles y usuarios de prueba
docs/                    # Contratos, decisiones, errores, modelo de datos y diagramas
postman/                 # Colección y entorno de Postman
.env.example             # Plantilla de configuración sin secretos
docker-compose.yml       # Orquestación de los seis contenedores
```

## Endpoints por el Gateway

Todas las rutas requieren `Authorization: Bearer <token>`. El Gateway controla el rol (`401` sin token, `403` con rol insuficiente) y cada microservicio vuelve a validar el JWT y la propiedad con el `sub` (`403` si el recurso es de otro usuario). Los errores siguen el formato común de `docs/ERRORES.md`.

| Método y ruta | Rol | Funcionalidad / reglas |
| --- | --- | --- |
| `POST /api/establecimientos`, `GET /api/establecimientos` | PRODUCTOR | F-02 |
| `POST /api/lotes`, `GET /api/lotes/{id}` | PRODUCTOR | F-03, RN-05, RN-08 (Georef; `503` sin guardar si falla) |
| `GET /api/admin/zonas`, `PUT /api/admin/zonas/{provincia}` | ADMIN | Parámetros de zona por provincia (RN-08) |
| `POST /api/contratistas`, `GET /api/contratistas/me` | CONTRATISTA | F-04 |
| `POST /api/maquinas`, `GET /api/maquinas` | CONTRATISTA | F-05 |
| `POST /api/aplicadores`, `GET /api/aplicadores` | CONTRATISTA | F-05 |
| `POST /api/habilitaciones` | CONTRATISTA | F-06, RN-02 a RN-04 |
| `GET /api/admin/habilitaciones?estado=`, `POST …/{id}/aprobar`, `POST …/{id}/rechazar` | ADMIN | F-08 |
| `POST /api/agenda/bloques`, `GET /api/agenda/bloques?maquinaId=` | CONTRATISTA | F-09, RN-13 |
| `POST /api/agronomos`, `GET /api/agronomos/me` | AGRONOMO | F-07 |
| `GET /api/admin/matriculas?estado=`, `POST …/{id}/aprobar`, `POST …/{id}/rechazar` | ADMIN | F-08 |
| `GET /api/admin/umbrales`, `PUT /api/admin/umbrales` | ADMIN | Umbrales climáticos (RN-09) |
| `POST /api/ordenes` | PRODUCTOR | F-10, RN-06 |
| `GET /api/ordenes?estado=`, `GET /api/ordenes/{id}` | PRODUCTOR, AGRONOMO, CONTRATISTA | Consulta según rol |
| `POST /api/ordenes/{id}/receta` | AGRONOMO | F-11, RN-01, RN-07 |
| `GET /api/ordenes/{id}/ventanas` | PRODUCTOR, CONTRATISTA | F-12, RN-09 (Open-Meteo) |
| `POST /api/ordenes/{id}/postulaciones` | CONTRATISTA | F-13, RN-02 a RN-04, RN-09 a RN-13 |
| `GET /api/ordenes/{id}/postulaciones` | PRODUCTOR | F-14, RN-14 |
| `POST /api/ordenes/{id}/asignacion` | PRODUCTOR | F-15 (reserva de agenda con compensación) |
| `POST /api/ordenes/{id}/ejecucion` | CONTRATISTA | F-16, RN-15, RN-16 |
| `POST /api/ordenes/{id}/confirmacion` | PRODUCTOR | F-17, RN-17 |

Las rutas `/internal/**` (contratos en `docs/CONTRATOS.md`) solo se usan entre microservicios y el Gateway no las publica.

## Documentación OpenAPI (Swagger)

Swagger UI unificado en http://localhost:8080/swagger-ui.html: el selector superior cambia entre `ms-campos`, `ms-contratistas` y `ms-trabajos`. Para probar desde ahí, obtener un token (paso 3 de *Pruebas rápidas*) y cargarlo con **Authorize**. Cada servicio también publica `/v3/api-docs` en su puerto cuando se ejecuta desde IntelliJ.

## Colección Postman

En `postman/` están la colección `tpi-contratistas.postman_collection.json` y el entorno `tpi-local.postman_environment.json`. Importar ambos, cargar la contraseña de demostración en la variable `password` del entorno y ejecutar primero la carpeta **0. Tokens**. La carpeta **1. Caso de uso principal** recorre orden → receta → ventanas → postulación → asignación con los datos de demostración; la **2** muestra los rechazos por regla y por seguridad.

## Datos de demostración

Cada servicio carga su `data.sql` en cada arranque, con fechas relativas al día en que se levanta:

- **ms-campos:** lote 1 *LIBRE*, lote 2 *AMORTIGUAMIENTO* y lote 3 *EXCLUSION* de `productor1`; lote 4 de `productor2`; parámetros de zona por provincia.
- **ms-contratistas:** `contratista1` con máquina 1 y aplicador 1 habilitados por un año; `contratista2` con máquina 2, aplicador 2 cuyo carnet **vence a los 2 días** (RN-02) y aplicador 3 con carnet **pendiente** de validación; disponibilidad de ambas máquinas de mañana a 7 días.
- **ms-trabajos:** umbrales climáticos; `agronomo1` con matrícula validada y `agronomo2` pendiente (RN-01).

Con la base recién creada los ids son los indicados. Para volver al estado inicial: `docker compose down -v` y levantar de nuevo.

## Pruebas automatizadas

```powershell
.\mvnw.cmd test
```

Las pruebas usan H2 en modo PostgreSQL y simulan Keycloak, Georef, Open-Meteo y los otros microservicios, así que no requieren Docker ni red. Entre ellas: transiciones de estado (RN-18), concurrencia de reservas, falla segura con dependencias caídas (`FallaSeguraTest`, `FalloSeguroGeorefTest`), compensación de reservas (`CompensacionIntegracionTest`) y el flujo completo de la orden (`FlujoCompletoTest`).

## Pendiente

- Verificar el arranque con Docker Compose **desde un entorno limpio** en otra PC del equipo (incluida la importación del realm).
- Ejecutar la colección Postman contra el entorno levantado; los pasos que usan Open-Meteo dependen del pronóstico real.

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
- Cada PR indica la regla RN cuando corresponda, el criterio de rúbrica, la justificación, cómo se probó y los riesgos.

## Fallas obligatorias

- Postulación sin Open-Meteo o ms-contratistas: responder 503 sin guardarla.
- Lote sin clasificar por falla de Georef: responder 503 sin guardarlo.
- Ejecución sin consulta climática: registrar ejecución con clima NO_DISPONIBLE.
- Reserva de agenda confirmada seguida de falla local: compensar liberándola.

## Referencias de evaluación

- **ARQ-01:** estructura y límites de microservicios, Gateway y comunicación.
- **SEG-01:** identidad y seguridad; roles por ruta y método en el Gateway, JWT y propiedad por `sub` en cada servicio.
- **INF-01:** persistencia, configuración y despliegue reproducible; falta la prueba independiente sobre una instalación limpia.

Documentación principal: *TPI Backend: Contratistas rurales* y *Rúbrica General TPI 2026*, disponibles en los materiales del proyecto.

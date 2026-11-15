# TPI 2026 — Contratistas rurales

Backend de Aplicaciones — UTN FRC. Proyecto de backend basado en microservicios para la gestión de contratistas rurales.

> **Estado:** configuración inicial de infraestructura. Los servicios arrancan, pero las funcionalidades de negocio y la autorización completa todavía están pendientes. Entorno de **desarrollo local**, no apto para producción.

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
| `postgres` | Persistencia, esquemas aislados | Solo red Docker |

PostgreSQL contiene cuatro esquemas con usuarios propios: `campos`/`campos_app`, `contratistas`/`contratistas_app`, `trabajos`/`trabajos_app` y `keycloak`/`keycloak`.

Solo el Gateway y Keycloak publican puertos al host (en `127.0.0.1`). No se accede a los microservicios directamente desde la PC cuando se utiliza el Compose completo.

## Requisitos previos

- Git.
- Docker Desktop con Docker Engine activo y Docker Compose.
- Puertos locales **8080** y **8081** disponibles.

**No es necesario instalar Java ni Maven para levantar el proyecto con Docker**: las imágenes construyen los JAR dentro de sus etapas Maven. Para ejecutar proyectos desde IntelliJ sí se necesita JDK 21.

## Primer arranque (Windows / PowerShell)

1. Clonar el repositorio y entrar a su raíz:

   ```powershell
   git clone https://github.com/franleal/tpi-contratistas-rurales.git
   cd tpi-contratistas-rurales
   ```

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

   Se esperan **seis servicios** levantados: `postgres` (healthy), `keycloak`, `ms-campos`, `ms-contratistas`, `ms-trabajos` y `gateway`. `Up` no siempre indica que una aplicación terminó de iniciar; revisar logs ante dudas:

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

El archivo del realm fue exportado **sin usuarios**. Por eso, en una instalación nueva se debe crear manualmente un usuario de prueba para solicitar JWT: en el realm `agro`, ir a **Users → Add user**, asignar el rol `PRODUCTOR` en **Role mapping**, establecer una contraseña **no temporal** en **Credentials** y completar las acciones requeridas del perfil (por ejemplo, nombre y correo de prueba). No incorporar contraseñas de prueba al repositorio.

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

En PowerShell, con un usuario previamente creado en `agro` (ejemplo: `productor_test`):

```powershell
$credencial = Get-Credential -UserName "productor_test" -Message "Contraseña de Keycloak"

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

**Estado actual:** el Gateway valida el JWT y reenvía la petición a `ms-campos`, que todavía puede responder `401` con `WWW-Authenticate: Basic realm="Realm"`, debido a que aún no tiene configurado su Resource Server. **Ese `401` no equivale a una funcionalidad de lotes implementada.** Los controladores y reglas de negocio siguen pendientes.

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

## Ejecutar desde IntelliJ (alternativa de desarrollo)

El flujo recomendado para comprobar el entorno compartido es Docker Compose. Para ejecutar las aplicaciones Java individualmente desde IntelliJ:

- Usar JDK 21 y ejecutar las clases `*Application` desde su módulo Maven correcto.
- En cada Run Configuration de los microservicios, asignar `DB_PASSWORD` a la contraseña de su propio esquema; usar `-Duser.timezone=America/Argentina/Buenos_Aires` en **VM options**.
- Los puertos locales establecidos son `8082` (`ms-campos`), `8083` (`ms-contratistas`), `8084` (`ms-trabajos`) y `8080` (Gateway).
- La configuración **Docker Compose completa no publica PostgreSQL en el host**. Para ejecutar Java localmente será necesario habilitar temporalmente una conexión a PostgreSQL desde el host, sin cambiar el diseño final de la red.
- Detener el Gateway de IntelliJ antes de levantar el Gateway en Docker: ambos ocuparían el puerto 8080.

## Estructura principal

```text
api-gateway/             # Enrutamiento y seguridad de entrada
ms-campos/               # Dominio de campos
ms-contratistas/         # Dominio de contratistas
ms-trabajos/             # Dominio de trabajos
agro-commons/            # Utilidades técnicas compartidas
infra/db/init.sql        # Creación inicial de usuarios/esquemas
keycloak/agro-realm.json # Configuración inicial del realm, sin usuarios
docs/                    # Contratos y decisiones
.env.example             # Plantilla de configuración sin secretos
docker-compose.yml       # Orquestación de los seis contenedores
```

## Estado y próximos desarrollos

**Preparado y probado en el entorno del equipo:** compilación de proyectos, PostgreSQL con esquemas independientes, Keycloak con realm y roles, emisión de JWT, Gateway con rutas y validación básica de JWT, y levantamiento conjunto en Docker Compose.

**Pendiente (obligatorio antes de considerar funcional el backend):**

- Autorización por **rol y método HTTP** en el Gateway (`401` sin token y `403` cuando el rol sea insuficiente).
- Validación del JWT también en **cada microservicio**, y control de propiedad con el `sub` del token.
- Implementación de entidades, DTOs, controladores, servicios, reglas RN-01 a RN-17, integraciones externas, errores comunes y pruebas.
- Verificación del arranque **desde un entorno limpio** por otro integrante del equipo. La importación inicial del realm no se probó todavía sobre una segunda instalación vacía.

Los endpoints `/internal/**` son solo para llamadas entre microservicios autenticadas y **no deben enrutarse públicamente** en el Gateway. Ningún microservicio debe depender de entidades JPA, DTOs de negocio o reglas internas de otro servicio.

## Referencias de evaluación

- **ARQ-01:** estructura y límites de microservicios, Gateway y comunicación.
- **SEG-01:** identidad y seguridad; configuración inicial parcial, controles de roles y propiedad pendientes.
- **INF-01:** persistencia, configuración y despliegue reproducible; falta la prueba independiente sobre una instalación limpia.

Documentación principal: *TPI Backend: Contratistas rurales* y *Rúbrica General TPI 2026*, disponibles en los materiales del proyecto.

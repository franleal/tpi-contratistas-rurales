-- Inicialización de PostgreSQL: un esquema y un usuario por microservicio.
-- Se ejecuta una sola vez, cuando el volumen pgdata está vacío.
-- Cada usuario es dueño únicamente de su esquema: no puede leer ni escribir
-- tablas de otro servicio aunque compartan la misma instancia.

-- Obtener las contraseñas desde las variables de entorno del contenedor
\getenv campos_pwd CAMPOS_DB_PASSWORD
\getenv contratistas_pwd CONTRATISTAS_DB_PASSWORD
\getenv trabajos_pwd TRABAJOS_DB_PASSWORD
\getenv keycloak_pwd KC_DB_PASSWORD

-- Crear usuarios con sus respectivas contraseñas
SELECT format('CREATE ROLE campos_app LOGIN PASSWORD %L', :'campos_pwd') \gexec
SELECT format('CREATE ROLE contratistas_app LOGIN PASSWORD %L', :'contratistas_pwd') \gexec
SELECT format('CREATE ROLE trabajos_app LOGIN PASSWORD %L', :'trabajos_pwd') \gexec
SELECT format('CREATE ROLE keycloak LOGIN PASSWORD %L', :'keycloak_pwd') \gexec

-- Nadie crea objetos en el esquema public (endurecimiento por defecto de PG 15+, explícito acá)
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE postgres FROM PUBLIC;
GRANT CONNECT ON DATABASE postgres TO campos_app, contratistas_app, trabajos_app, keycloak;

-- Crear esquemas y asignar sus propietarios
CREATE SCHEMA campos AUTHORIZATION campos_app;
CREATE SCHEMA contratistas AUTHORIZATION contratistas_app;
CREATE SCHEMA trabajos AUTHORIZATION trabajos_app;
CREATE SCHEMA keycloak AUTHORIZATION keycloak;

-- Cada usuario busca primero en su propio esquema
ALTER ROLE campos_app SET search_path = campos;
ALTER ROLE contratistas_app SET search_path = contratistas;
ALTER ROLE trabajos_app SET search_path = trabajos;
ALTER ROLE keycloak SET search_path = keycloak;

-- Verificación manual del aislamiento (debe fallar con "permission denied for schema"):
--   docker compose exec postgres psql -U campos_app -d postgres -c "SELECT * FROM trabajos.orden_trabajo"

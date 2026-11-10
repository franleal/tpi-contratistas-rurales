
-- Obtener las contraseñas desde las variables de entorno
\getenv campos_pwd CAMPOS_DB_PASSWORD
\getenv contratistas_pwd CONTRATISTAS_DB_PASSWORD
\getenv trabajos_pwd TRABAJOS_DB_PASSWORD
\getenv keycloak_pwd KC_DB_PASSWORD

-- Crear usuarios con sus respectivas contraseñas
SELECT format('CREATE ROLE campos_app LOGIN PASSWORD %L', :'campos_pwd') \gexec
SELECT format('CREATE ROLE contratistas_app LOGIN PASSWORD %L', :'contratistas_pwd') \gexec
SELECT format('CREATE ROLE trabajos_app LOGIN PASSWORD %L', :'trabajos_pwd') \gexec
SELECT format('CREATE ROLE keycloak LOGIN PASSWORD %L', :'keycloak_pwd') \gexec

-- Crear esquemas y asignar sus propietarios
CREATE SCHEMA campos AUTHORIZATION campos_app;
CREATE SCHEMA contratistas AUTHORIZATION contratistas_app;
CREATE SCHEMA trabajos AUTHORIZATION trabajos_app;
CREATE SCHEMA keycloak AUTHORIZATION keycloak;

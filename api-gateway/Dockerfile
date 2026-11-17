# syntax=docker/dockerfile:1
# Imagen de api-gateway. El contexto de build es la raiz del repositorio porque la etapa
# Maven necesita instalar primero agro-commons (dependencia de los servicios).

# ---- Etapa 1: compilacion ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY agro-commons ./agro-commons
COPY api-gateway ./api-gateway

# La cache de ~/.m2 se reutiliza entre builds (BuildKit), asi no se descargan
# las dependencias cada vez que cambia el codigo.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -f agro-commons/pom.xml -DskipTests install \
 && mvn -B -q -f api-gateway/pom.xml -DskipTests package \
 && cp api-gateway/target/*.jar /app/app.jar

# ---- Etapa 2: ejecucion (solo JRE y el .jar) ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /app/app.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

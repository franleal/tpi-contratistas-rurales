FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY agro-commons ./agro-commons
COPY ms-trabajos ./ms-trabajos

RUN mvn -f agro-commons/pom.xml -B -DskipTests install
RUN mvn -f ms-trabajos/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/ms-trabajos/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

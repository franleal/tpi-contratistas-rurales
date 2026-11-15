FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY agro-commons ./agro-commons
COPY ms-campos ./ms-campos

RUN mvn -f agro-commons/pom.xml -B -DskipTests install
RUN mvn -f ms-campos/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/ms-campos/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

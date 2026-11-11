# Imagem da aplicação (§26): o primeiro estágio compila com o Maven Wrapper, o segundo só roda o jar.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 continuum
COPY --from=build /app/target/*.jar app.jar
USER continuum
ENV TZ=UTC
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

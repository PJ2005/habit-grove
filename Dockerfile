# Stage 1: compile, test, and package with Maven on Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B package

# Stage 2: small Java 25 runtime, non-root user
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 grove
WORKDIR /app
COPY --from=build /src/target/habit-grove-*.jar app.jar
USER grove
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

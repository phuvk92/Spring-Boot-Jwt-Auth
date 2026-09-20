# Stage 1: Build application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /data/svg && chown -R appuser:appgroup /data/svg

COPY --chown=appuser:appgroup --from=build /app/target/cutting-admin-*.jar app.jar

USER appuser

ENV SERVER_PORT=8080
ENV FILE_STORAGE_PATH=/data/svg

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

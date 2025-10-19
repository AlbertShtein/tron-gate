# --- Build stage ---
FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /app

COPY . .

RUN gradle build --no-daemon

# --- Run stage ---
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

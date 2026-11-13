# Multi-stage Dockerfile for Zensa Spring Boot API
# Build with: docker build -t zensa-api .
# Run locally: docker run -p 8080:8080 -e JWT_SECRET=... -e DB_PASSWORD=... zensa-api

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and config first (better layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

# Make gradlew executable and build the fat jar (skip tests for faster deploys)
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S zensa && adduser -S zensa -G zensa
USER zensa

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Render (and most PaaS) set the PORT environment variable
EXPOSE 8080

# Health-friendly JVM flags for free tiers (limited memory)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

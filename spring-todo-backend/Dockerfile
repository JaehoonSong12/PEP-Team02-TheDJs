# Stage 1: Build
FROM gradle:8-jdk21 AS build

WORKDIR /workspace

# Copy Gradle wrapper and build config files first for layer caching
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

# Make gradlew executable and download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy full source code and build the application
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN adduser -D -u 1001 appuser

# Create application and data directories with proper ownership
RUN mkdir -p /app/data && chown -R appuser:appuser /app

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /workspace/build/libs/*.jar app.jar

# Set ownership to appuser
RUN chown appuser:appuser /app/app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Run the application with datasource URL pointing to the volume-mounted SQLite database
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:sqlite:/app/data/todo.db"]

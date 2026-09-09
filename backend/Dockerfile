# ==============================================================================
# Multi-Stage Dockerfile for Spring Boot 3.3 (Java 21) on Render
# Stage 1: Build Application Artifact with Maven caching
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build optimized fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Production JRE Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as non-root user for enterprise container security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Render dynamic port binding (defaults to 8080, overridden by Render's $PORT at runtime)
ENV PORT=8080
EXPOSE 8080

# Production container JVM flags:
# - UseContainerSupport: respects cgroup limits (crucial for Render 512MB RAM tier)
# - MaxRAMPercentage=75.0: allocates max 75% container memory to heap, leaving 25% for Metaspace/OS
# - ExitOnOutOfMemoryError: prevents hung/zombie container state
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=35.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]

# ==============================================================================
# Multi-Stage Dockerfile for Warehouse Management System (WMS)
# Stage 1: Build React Frontend
# Stage 2: Build Spring Boot Application JAR
# Stage 3: Lightweight Production JRE Runtime
# ==============================================================================

# --- Stage 1: Build Frontend (React + Vite) ---
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# --- Stage 2: Build Backend (Java / Maven) ---
FROM eclipse-temurin:17-jdk-jammy AS backend-builder
WORKDIR /app

# Copy Maven wrapper and POM for dependency pre-fetching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and compiled frontend assets from Stage 1
COPY src/ src/
COPY --from=frontend-builder /app/src/main/resources/static/ src/main/resources/static/

# Package executable JAR (skip tests during container build; tested in CI)
RUN ./mvnw clean package -DskipTests -B

# --- Stage 3: Production Runtime ---
FROM eclipse-temurin:17-jre AS runtime

LABEL maintainer="Dennysa-Maria Popescu"
LABEL description="Warehouse Management System (WMS) Production Image"

WORKDIR /app

# Security: Create non-root user
RUN groupadd --system wmsgroup && useradd --system --gid wmsgroup wmsuser

# Install curl for container health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy fat JAR from Stage 2
COPY --from=backend-builder /app/target/wms-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R wmsuser:wmsgroup /app

USER wmsuser:wmsgroup

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

# Build context is the REPOSITORY ROOT, not backend/ — build.gradle.kts resolves the
# OpenAPI spec at ../openapi/cookingchallenge-api.yaml, which sits outside backend/.
#   docker build -f docker/backend.Dockerfile .

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Warm the Gradle dependency cache in its own layer so source edits don't re-download.
COPY backend/gradlew backend/gradlew
COPY backend/gradle backend/gradle
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/
WORKDIR /workspace/backend
RUN ./gradlew --no-daemon --console=plain dependencies > /dev/null

COPY openapi /workspace/openapi
COPY backend/src src
RUN ./gradlew --no-daemon --console=plain bootJar -x test


FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Set by the publish workflow to <BASE_VERSION>.<run number>; `dev` for a local build. ARG is
# stage-scoped, hence the re-declaration. Spring's relaxed binding reads APP_VERSION as
# `app.version`, which ConfigService reports on GET /api/v1/config.
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}

# curl is for the HEALTHCHECK below; the temurin JRE image ships no HTTP client.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Never run the app as root.
RUN useradd --system --no-create-home --uid 10001 cookoff
COPY --from=build --chown=cookoff:cookoff /workspace/backend/build/libs/*.jar /app/app.jar
USER cookoff

EXPOSE 8080
# Respect the container memory limit instead of the host's total RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# /actuator/health answers 503 while any component is DOWN — including the Postgres
# connection — so `curl -f` alone is the whole check. start-period covers Liquibase
# applying the changelog on a cold first boot.
HEALTHCHECK --interval=15s --timeout=3s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

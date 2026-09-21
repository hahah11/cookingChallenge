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

# Never run the app as root.
RUN useradd --system --no-create-home --uid 10001 cookoff
COPY --from=build --chown=cookoff:cookoff /workspace/backend/build/libs/*.jar /app/app.jar
USER cookoff

EXPOSE 8080
# Respect the container memory limit instead of the host's total RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1

# ---- Stage 1: build the jar with the full JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy the build definition first: Docker reuses this layer while pom.xml is unchanged.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# BuildKit cache mount keeps downloaded dependencies between builds.
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI (they need Docker for Testcontainers), so the image build skips them.
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q package -DskipTests \
    && cp target/tickettriage-*.jar app.jar

# ---- Stage 2: small runtime image with only the JRE and the jar ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user, not root.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

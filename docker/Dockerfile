# Stage 1: Build
FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY Misc/pom.xml Misc/pom.xml
RUN mvn -f Misc/pom.xml dependency:go-offline
COPY src src
COPY ["SQL files", "SQL files"]
COPY SupportConfigFiles/rls-contract.json SupportConfigFiles/rls-contract.json
RUN mvn -f Misc/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.revision="${GIT_SHA}"
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/*.jar app.jar
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && chown app:app /app/app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=10s --start-period=30s --retries=12 \
    CMD curl --fail --silent --show-error --max-time 10 http://127.0.0.1:8080/ || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

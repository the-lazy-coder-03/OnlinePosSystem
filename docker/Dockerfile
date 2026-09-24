# Stage 1: Build
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /workspace
COPY Misc/pom.xml Misc/pom.xml
RUN mvn -f Misc/pom.xml dependency:go-offline
COPY src src
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
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

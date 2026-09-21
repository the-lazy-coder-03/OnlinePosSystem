# Stage 1: Build
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /workspace

COPY Misc/pom.xml Misc/pom.xml

RUN mvn -f Misc/pom.xml dependency:go-offline

COPY src src

# TEMPORARY: Skip test compilation and execution
RUN mvn -f Misc/pom.xml clean package -Dmaven.test.skip=true


# Stage 2: Run
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
# Match the Playwright version in pom.xml (1.49.0)
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app

COPY --from=build /app/target/JobAutomation-0.0.1-SNAPSHOT.jar app.jar

# Use preinstalled browsers; don't download at runtime
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

RUN mkdir -p /app/apply-screenshots
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
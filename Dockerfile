# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY libs ./libs
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && BOOT_JAR="$(ls build/libs/*.jar | grep -v '\-plain\.jar$' | head -n 1)" \
    && cp "${BOOT_JAR}" /workspace/application.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data/license \
    && chown -R app:app /app

COPY --from=build /workspace/application.jar /app/app.jar

USER app
EXPOSE 8082

ENV JAVA_OPTS="" \
    LICENSE_STORAGE_PATH=/app/data/license/active.lic

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

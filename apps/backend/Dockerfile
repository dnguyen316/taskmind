# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the Maven reactor structure first for dependency caching.
COPY pom.xml ./
COPY libs/events/pom.xml libs/events/pom.xml
COPY libs/ai-contracts/pom.xml libs/ai-contracts/pom.xml
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/relay/pom.xml apps/relay/pom.xml
COPY apps/ai/pom.xml apps/ai/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl apps/backend -am dependency:go-offline

# Copy only the source needed to build Core and its dependent libraries.
COPY libs libs
COPY apps/backend apps/backend

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl apps/backend -am package -DskipTests && \
    find apps/backend/target \
      -maxdepth 1 \
      -name "taskmind-backend-*.jar" \
      ! -name "*.original" \
      -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:17-jre-alpine AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN addgroup -S -g "${APP_GID}" taskmind && \
    adduser -S -D -H -u "${APP_UID}" -G taskmind taskmind

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

USER taskmind:taskmind

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
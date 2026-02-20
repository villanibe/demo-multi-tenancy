# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache bash

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Build a minimized Java runtime with jlink.
# Note: This uses a conservative module list that works for typical Spring Boot apps.
RUN jlink \
    --module-path "$JAVA_HOME/jmods" \
    --add-modules java.base,java.logging,java.xml,java.naming,java.management,java.security.jgss,java.instrument,java.desktop,java.sql,java.transaction.xa,java.net.http,java.rmi,jdk.crypto.ec,jdk.unsupported,jdk.attach,jdk.jcmd \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre


FROM alpine:3.20
WORKDIR /app

# Install dependencies needed for the script
RUN apk add --no-cache bash curl

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /jre /opt/java
COPY --from=build /workspace/target/*.jar /app/app.jar

# COPY the script from your host to the container
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Define JVM Options for a 512MB Container
# - Multi-line format for readability
ENV PATH="/opt/java/bin:${PATH}"
ENV JAVA_TOOL_OPTIONS="
    -XX:+UseContainerSupport 
    -Xms256m 
    -Xmx256m 
    -Xss256k 
    -XX:MaxMetaspaceSize=128m 
    -XX:ReservedCodeCacheSize=48m 
    -XX:MaxDirectMemorySize=64m 
    -XX:ParallelGCThreads=1 
    -XX:ConcGCThreads=1 
    -XX:NativeMemoryTracking=summary 
    -XX:+UseSerialGC 
    -XX:+ExitOnOutOfMemoryError 
    -Djava.awt.headless=true 
    -Dfile.encoding=UTF-8
    "

EXPOSE 8080
USER app

# Run the script
ENTRYPOINT ["/bin/bash", "/app/entrypoint.sh"]

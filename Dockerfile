# Java version (same value as Dockerfile.dev). How to update it: docs/arquitectura/06-infraestructura.md
ARG JAVA_VERSION=21.0.12.1_1

# build: dependencies in a cached layer, then the JAR. Never reaches the final image.
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# runtime: the image CI publishes
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -D -H -G app app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75 -Xss512k -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=192m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

# Uploaded product images land here (see ProductImageService) - owned by appuser since
# it writes to it at runtime, not just the root-owned files copied in above.
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads

EXPOSE 8080

USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]

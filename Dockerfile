# Build the final image
FROM openjdk:21-ea-17-slim

WORKDIR /app

# Copy application JAR file
COPY target/BottleCapCollector-2.2.0.jar /app/BottleCapCollector-2.2.0.jar

COPY config /app/config/


CMD ["java", "-jar", "BottleCapCollector-2.2.0.jar"]

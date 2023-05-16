# Build the final image
FROM openjdk:8-jre

WORKDIR /app

# Copy application JAR file
COPY target/BottleCapCollector-2.1.1.jar /app/BottleCapCollector-2.1.1.jar

COPY config /app/config/

RUN apt-get update && \
    apt-get install -y libopencv-dev

CMD ["java", "-jar", "BottleCapCollector-2.1.1.jar"]

 
FROM alpine:3.20

# Download and install JDK 21 in the image.
RUN apk add --no-cache openjdk21-jdk

WORKDIR /app
COPY target/MaDemo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
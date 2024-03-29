FROM maven:3.5.3-jdk-11

COPY . /app
WORKDIR /app
RUN mvn -B -q clean package

FROM openjdk:11-jdk-slim
COPY --from=0 /app/target/renderer*.jar /app/renderer.jar

ENTRYPOINT ["/usr/bin/java", "-Djavax.net.ssl.trustStorePassword=changeit", "-Xms128M", "-Xmx512M", "-jar", "/app/renderer.jar"]
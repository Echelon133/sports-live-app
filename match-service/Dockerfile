FROM amazoncorretto:11
EXPOSE 9001
WORKDIR /app
ADD /target/match-service-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar" ]
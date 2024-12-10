FROM amazoncorretto:17.0.13
EXPOSE 80
WORKDIR /app
ADD /target/competition-service-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT [ "sh", "-c", "java --enable-preview -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar" ]

FROM amazoncorretto:8-alpine
ENV context ""
ENV port 8020
ADD /src/main/resources/application.properties //
COPY /build/libs/*-1.0-SNAPSHOT.jar demo-1.0-SNAPSHOT.jar
RUN mkdir /temp
ENTRYPOINT ["java", "-jar", "/demo-1.0-SNAPSHOT.jar","--server.port=${port}"]


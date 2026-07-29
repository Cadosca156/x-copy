FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/*.jar notebookbackend-0.0.1-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "notebookbackend-0.0.1-SNAPSHOT.jar"]
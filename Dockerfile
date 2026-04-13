FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

COPY . .

RUN mvn clean package

CMD ["java", "-jar", "target/Test-1.0-SNAPSHOT.jar"]
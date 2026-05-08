FROM maven:3.9.11-eclipse-temurin-11 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests clean package

FROM eclipse-temurin:11-jre

WORKDIR /app

COPY --from=build /app/target/campus-trade-hub-1.0.0.jar /app/app.jar

ENV PORT=8081
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8081

CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar /app/app.jar"]
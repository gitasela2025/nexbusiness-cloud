# Stage 1: Build the application using Maven with Cache
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. pom.xml copy කර dependencies download කර cache කරගැනීම
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B || true

# 2. source code copy කර build කිරීම
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -o || mvn clean package -DskipTests

# Stage 2: Run the application on a lightweight JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar \
    && find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /app/app.jar \;

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/app.jar ./app.jar

EXPOSE 8081 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

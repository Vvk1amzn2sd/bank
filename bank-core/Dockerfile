FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
COPY lib /app/lib
RUN mvn clean package -Dmaven.test.skip=true -Dmaven.compiler.release=21

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/bank-core-*.jar /app/bank-core.jar
COPY lib /app/lib
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*:/app/bank-core.jar", "com.vvk.banque.cli.BankShellDemo"]

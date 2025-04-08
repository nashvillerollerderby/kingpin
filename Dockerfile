FROM maven:3.9.9-amazoncorretto-23 AS build

WORKDIR /kingpin
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve-plugins dependency:resolve
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/kingpin/kingpin-app/node \
    --mount=type=cache,target=/kingpin/kingpin-app/node_modules \
    mvn -pl kingpin-app clean package
RUN --mount=type=cache,target=/root/.m2 mvn clean package

FROM amazoncorretto:23-alpine

WORKDIR /scoreboard
COPY --from=build /kingpin/scoreboard/config/penalties config/penalties
COPY --from=build /kingpin/scoreboard/html html
COPY --from=build /kingpin/kingpin-app/target/dist html/new-ui
COPY --from=build /kingpin/scoreboard/target target
RUN mkdir logs
RUN chmod +x /scoreboard/target/scoreboard-*-jar-with-dependencies.jar

ENTRYPOINT ["sh", "-c", "java -jar /scoreboard/target/scoreboard-*-jar-with-dependencies.jar"]

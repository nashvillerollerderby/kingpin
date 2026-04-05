FROM node:22-slim AS frontend-build

ARG CONFIGURATION="production"

WORKDIR /kingpin-ui
COPY angular .

RUN --mount=type=cache,target=/root/.npm npm install
RUN npm run build -- --output-hashing all -c ${CONFIGURATION} --output-path dist

FROM ubuntu:oracular AS rust-build

WORKDIR /rust

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install curl build-essential libssl-dev pkg-config libclang-dev -y
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

RUN apt-get install openssl cmake -y

COPY Cargo.toml .
COPY server server
COPY ndi-bindings ndi-bindings

ENV NDI_SDK_DIR=/lib/ndi
RUN --mount=from=ndi,target=$NDI_SDK_DIR /root/.cargo/bin/cargo build -p ndi_bindings -r
RUN --mount=from=ndi,target=$NDI_SDK_DIR /root/.cargo/bin/cargo build --bin server -r

FROM scratch

WORKDIR /app

COPY --from=frontend-build /kingpin-ui/dist kingpin
COPY --from=rust-build /rust/target/release/server server
COPY --from=rust-build /rust/log4rs.yaml log4rs.yaml

ENTRYPOINT ["./app/server"]

#FROM maven:3.9.9-amazoncorretto-23 AS build
#
#WORKDIR /kingpin
#COPY . .
#
#RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve-plugins dependency:resolve
#RUN --mount=type=cache,target=/root/.m2 \
#    --mount=type=cache,target=/kingpin/kingpin-app/node \
#    --mount=type=cache,target=/kingpin/kingpin-app/node_modules \
#    mvn -pl kingpin-app clean package
#RUN --mount=type=cache,target=/root/.m2 mvn clean package
#
#FROM amazoncorretto:23-alpine
#
#WORKDIR /scoreboard
#COPY --from=build /kingpin/scoreboard/config/penalties config/penalties
#COPY --from=build /kingpin/scoreboard/html html
#COPY --from=build /kingpin/kingpin-app/target/dist html/new-ui
#COPY --from=build /kingpin/scoreboard/target target
#RUN mkdir logs
#RUN chmod +x /scoreboard/target/scoreboard-*-jar-with-dependencies.jar
#
#ENTRYPOINT ["sh", "-c", "java -jar /scoreboard/target/scoreboard-*-jar-with-dependencies.jar"]

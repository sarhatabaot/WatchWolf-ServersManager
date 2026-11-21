FROM gradle:9.7.1-jdk17 AS manager_build
WORKDIR /src/manager
COPY . .

RUN mkdir -p lib \
    && curl -fL --retry 3 \
        https://github.com/watch-wolf/WatchWolf-Core/releases/download/v0.3.1/watchwolf-core-0.3.1.jar \
        -o lib/watchwolf-core-0.3.1.jar \
    && gradle --no-daemon shadowJar -x test

FROM gradle:9.7.1-jdk17 AS plugin_asset
ARG WATCHWOLF_SERVER_VERSION=0.4.2
RUN mkdir -p /plugin \
    && curl -fL --retry 3 \
        "https://github.com/sarhatabaot/WatchWolf-Server/releases/download/v${WATCHWOLF_SERVER_VERSION}/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar" \
        -o "/plugin/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar" \
    && curl -fL --retry 3 \
        "https://github.com/sarhatabaot/WatchWolf-Server/releases/download/v${WATCHWOLF_SERVER_VERSION}/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
        -o "/plugin/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
    && cd /plugin && sha256sum -c "WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
    && rm "WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256"

FROM eclipse-temurin:17-jdk
ARG MANAGER_VERSION=0.4.2
LABEL org.opencontainers.image.source="https://github.com/sarhatabaot/WatchWolf-ServersManager"
WORKDIR /servers
COPY --from=manager_build /src/manager/build/libs/watchwolf-servers-manager-${MANAGER_VERSION}.jar ServersManager.jar
COPY --from=plugin_asset /plugin/ usual-plugins/
RUN mkdir -p server-types tmp logs
CMD ["java", "-cp", "ServersManager.jar", "dev.watchwolf.serversmanager.ServersManager"]

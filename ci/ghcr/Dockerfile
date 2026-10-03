FROM maven:3.9-eclipse-temurin-17 AS manager_build
WORKDIR /src/manager
COPY . .

RUN mkdir -p lib \
    && curl -fL --retry 3 \
        https://github.com/watch-wolf/WatchWolf-Core/releases/download/v0.3.1/watchwolf-core-0.3.1.jar \
        -o lib/watchwolf-core-0.3.1.jar \
    && mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
        -Dfile=lib/watchwolf-core-0.3.1.jar -DgroupId=dev.watchwolf \
        -DartifactId=watchwolf-core -Dversion=0.3.1 -Dpackaging=jar \
    && mvn -B -DskipTests package

FROM maven:3.9-eclipse-temurin-17 AS plugin_asset
ARG WATCHWOLF_SERVER_VERSION=0.4.1
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
ARG MANAGER_VERSION=0.4.1
LABEL org.opencontainers.image.source="https://github.com/sarhatabaot/WatchWolf-ServersManager"
WORKDIR /servers
COPY --from=manager_build /src/manager/target/watchwolf-servers-manager-${MANAGER_VERSION}.jar ServersManager.jar
COPY --from=plugin_asset /plugin/ usual-plugins/
RUN mkdir -p server-types tmp logs
CMD ["java", "-cp", "ServersManager.jar", "dev.watchwolf.serversmanager.ServersManager"]

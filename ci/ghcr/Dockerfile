# Stage 1: Build ServersManager jar
FROM gradle:9.7.1-jdk17 AS manager_build
ARG CORE_VERSION=0.3.1
USER root
WORKDIR /src/manager
COPY . .
RUN mkdir -p lib \
    && curl -fL --retry 3 \
        "https://github.com/watch-wolf/WatchWolf-Core/releases/download/v${CORE_VERSION}/watchwolf-core-${CORE_VERSION}.jar" \
        -o "lib/watchwolf-core-${CORE_VERSION}.jar" \
    && gradle --no-daemon shadowJar -x test

# Create the final required directory structure in this stage
RUN mkdir -p /out/server-types /out/tmp /out/logs /out/usual-plugins

# Stage 2: Download and verify plugin jar
FROM cgr.dev/chainguard/wolfi-base:latest AS plugin_asset
ARG WATCHWOLF_SERVER_VERSION=0.4.3
RUN apk add --no-cache curl \
    && mkdir -p /plugin \
    && curl -fL --retry 3 \
        "https://github.com/sarhatabaot/WatchWolf-Server/releases/download/v${WATCHWOLF_SERVER_VERSION}/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar" \
        -o "/plugin/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar" \
    && curl -fL --retry 3 \
        "https://github.com/sarhatabaot/WatchWolf-Server/releases/download/v${WATCHWOLF_SERVER_VERSION}/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
        -o "/plugin/WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
    && cd /plugin && sha256sum -c "WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256" \
    && rm "WatchWolf-${WATCHWOLF_SERVER_VERSION}-1.8-LATEST.jar.sha256"

# Stage 3: Runtime (jre has no shell, only run)
FROM cgr.dev/chainguard/jre:latest
ARG MANAGER_VERSION=0.4.3
LABEL org.opencontainers.image.source="https://github.com/sarhatabaot/WatchWolf-ServersManager"
WORKDIR /app

# The mounted Docker socket is root-owned (0660) in the supported deployment. ServersManager
# must be able to create and clean up sibling Minecraft containers.
USER 0

# Copy pre-created directory structure
COPY --from=manager_build --chown=65532:65532 /out/ /app/

# Copy the jar and plugin
COPY --from=manager_build --chown=65532:65532 \
    /src/manager/build/libs/watchwolf-servers-manager-${MANAGER_VERSION}.jar \
    /app/ServersManager.jar
COPY --from=plugin_asset --chown=65532:65532 \
    /plugin/ /app/usual-plugins/

# jre entrypoint is already /usr/bin/java, just pass arguments
CMD ["-cp", "ServersManager.jar", "dev.watchwolf.serversmanager.ServersManager"]

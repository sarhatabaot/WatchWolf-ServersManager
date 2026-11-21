FROM cgr.dev/chainguard/jre:latest-dev

# The manager needs root access to the root-owned Docker socket mounted by run.sh.
USER 0
WORKDIR /servers
COPY --chown=65532:65532 ServersManager.jar /servers/ServersManager.jar

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["echo \"[v] MACHINE_IP: $MACHINE_IP\"; echo \"[v] PUBLIC_IP: <REDACTED>\"; echo \"[v] PARENT_PWD: $PARENT_PWD\"; echo \"[v] SERVER_PATH_SHIFT: $SERVER_PATH_SHIFT\"; exec java -cp ServersManager.jar dev.watchwolf.serversmanager.ServersManager"]

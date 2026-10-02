package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;
import dev.watchwolf.serversmanager.server.runtime.ItzgContainerSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Docker lifecycle for the itzg image; keeps legacy port and readiness behavior. */
public final class ItzgDockerServerInstantiator implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ItzgDockerServerInstantiator.class);
    private static final String VOLUME_PREFIX = "watchwolf-itzg-";
    private final List<DockerizedServerInstantiator.DockerContainerStoppedObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> containerIds = new CopyOnWriteArrayList<>();
    private static boolean orphansRecovered;

    public Server startServer(Path preparedFolder, ItzgContainerSpec spec) throws IOException {
        String instanceId = spec.labels().get("watchwolf.instance-id");
        String serverId = "MC_Server-" + instanceId;
        String volumeName = VOLUME_PREFIX + instanceId;
        DockerClient docker = DockerClientBuilder.getInstance(
                DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();
        String containerId = null;
        boolean volumeCreated = false;

        try {
            recoverOrphans(docker);
            ensureImage(docker, spec.image());
            docker.createVolumeCmd().withName(volumeName).exec();
            volumeCreated = true;
            seedVolume(docker, preparedFolder, spec.image(), volumeName, instanceId);
            final int port;
            synchronized (DockerizedServerInstantiator.class) {
                port = DockerizedServerInstantiator.getNextServerPort();
                int socketPort = port + 1;
                List<String> env = spec.environment().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue()).toList();
                CreateContainerResponse container = docker.createContainerCmd(spec.image())
                        .withName(serverId)
                        .withEnv(env)
                        .withLabels(spec.labels())
                        .withHostConfig(new HostConfig()
                                .withPortBindings(
                                        PortBinding.parse(port + ":25565/tcp"),
                                        PortBinding.parse(port + ":25565/udp"),
                                        PortBinding.parse(socketPort + ":25566/tcp"))
                                .withAutoRemove(true)
                                .withBinds(Bind.parse(volumeName + ":/data")))
                        .withExposedPorts(new ExposedPort(25565, InternetProtocol.TCP),
                                new ExposedPort(25565, InternetProtocol.UDP),
                                new ExposedPort(25566, InternetProtocol.TCP))
                        .exec();
                containerId = container.getId();
                docker.startContainerCmd(containerId).exec();
            }

            String launchedId = containerId;
            Server server = new Server("127.0.0.1:" + port);
            server.setStopper(() -> DockerizedServerInstantiator.killContainer(launchedId));
            DockerizedServerInstantiator.attachStdio(docker, containerId,
                    new DockerizedServerInstantiator.StdioAdapter(serverId, (line, err) -> {
                        logger.info("[{}] {}{}", serverId, err ? "(err) " : "", line);
                        server.raiseServerMessageEvent(line);
                    }));
            DockerizedServerInstantiator.DockerContainerStoppedObserver observer =
                    new DockerizedServerInstantiator.DockerContainerStoppedObserver(serverId, server,
                            () -> {
                                removeVolume(docker, volumeName);
                                containerIds.remove(launchedId);
                            });
            observers.add(observer);
            containerIds.add(containerId);
            observer.start();
            logger.info("Launched {} with image {}, volume {}, port {}", serverId, spec.image(), volumeName, port);
            return server;
        } catch (IOException | RuntimeException ex) {
            if (containerId != null) removeContainer(docker, containerId);
            if (volumeCreated) removeVolume(docker, volumeName);
            throw ex;
        }
    }

    private static synchronized void recoverOrphans(DockerClient docker) {
        if (orphansRecovered) return;
        // ServersManager is a singleton for this deployment. Anything with these labels
        // predates this provider instance and has no in-memory owner after a restart.
        for (Container container : docker.listContainersCmd().withShowAll(true)
                .withLabelFilter(Map.of("watchwolf.managed", "true", "watchwolf.runtime", "itzg"))
                .exec()) {
            logger.warn("Removing orphaned itzg container {}", container.getId());
            removeContainer(docker, container.getId());
        }
        var volumes = docker.listVolumesCmd().exec().getVolumes();
        if (volumes != null) {
            for (var volume : volumes) {
                if (!volume.getName().startsWith(VOLUME_PREFIX)) continue;
                logger.warn("Removing orphaned itzg volume {}", volume.getName());
                removeVolume(docker, volume.getName());
                String instanceId = volume.getName().substring(VOLUME_PREFIX.length());
                try {
                    dev.watchwolf.serversmanager.server.ServerRequirements.clearFolder("tmp/" + instanceId);
                } catch (IOException ex) {
                    logger.warn("Could not clear orphaned scratch folder {}", instanceId, ex);
                }
            }
        }
        orphansRecovered = true;
    }

    private static void seedVolume(DockerClient docker, Path preparedFolder, String image,
                                   String volumeName, String instanceId) throws IOException {
        // A running helper mounts the volume before files are copied into /data. The helper
        // skips the image's Minecraft entrypoint and is removed before the real server starts.
        String helperId = null;
        try {
            CreateContainerResponse helper = docker.createContainerCmd(image)
                    .withName("watchwolf-seed-" + instanceId)
                    .withEntrypoint("/bin/sh", "-c")
                    .withCmd("sleep 3600")
                    .withHostConfig(new HostConfig().withBinds(Bind.parse(volumeName + ":/data")))
                    .exec();
            helperId = helper.getId();
            docker.startContainerCmd(helperId).exec();
            // docker-java reads each source inside ServersManager. No nested host bind path is
            // passed to the Docker daemon, including on Docker Desktop.
            try (var entries = Files.list(preparedFolder)) {
                for (Path entry : entries.toList()) {
                    docker.copyArchiveToContainerCmd(helperId)
                            .withHostResource(entry.toAbsolutePath().toString())
                            .withRemotePath("/data")
                            .exec();
                }
            }
            String ownership = docker.execCreateCmd(helperId)
                    .withCmd("chown", "-R", "1000:1000", "/data").exec().getId();
            try (var callback = new ResultCallback.Adapter<Frame>()) {
                docker.execStartCmd(ownership).exec(callback).awaitCompletion();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while setting itzg volume ownership", interrupted);
            }
            Long exitCode = docker.inspectExecCmd(ownership).exec().getExitCodeLong();
            if (exitCode == null || exitCode != 0) {
                throw new IOException("Could not set itzg volume ownership; chown exit code " + exitCode);
            }
        } finally {
            if (helperId != null) removeContainer(docker, helperId);
        }
    }

    private static void ensureImage(DockerClient docker, String image) throws IOException {
        try {
            docker.inspectImageCmd(image).exec();
        } catch (NotFoundException absent) {
            logger.info("Pulling Minecraft runtime image {}", image);
            try {
                docker.pullImageCmd(image).exec(new PullImageResultCallback()).awaitCompletion();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while pulling " + image, interrupted);
            }
        }
    }

    private static void removeContainer(DockerClient docker, String id) {
        try {
            docker.removeContainerCmd(id).withForce(true).exec();
        } catch (RuntimeException ex) {
            logger.warn("Could not remove itzg container {}", id, ex);
        }
    }

    private static void removeVolume(DockerClient docker, String name) {
        try {
            docker.removeVolumeCmd(name).exec();
        } catch (RuntimeException ex) {
            logger.warn("Could not remove itzg volume {}", name, ex);
        }
    }

    @Override
    public void close() {
        for (String id : new ArrayList<>(containerIds)) DockerizedServerInstantiator.killContainer(id);
        for (Thread observer : observers) {
            try {
                observer.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

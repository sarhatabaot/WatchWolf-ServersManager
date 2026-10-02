package dev.watchwolf.serversmanager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.serversmanager.server.ServerRequirements;
import dev.watchwolf.serversmanager.server.instantiator.ItzgDockerServerInstantiator;
import dev.watchwolf.serversmanager.server.instantiator.ThrowableServer;
import dev.watchwolf.serversmanager.server.runtime.ItzgRuntimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10 * 60)
public class ITItzgRuntimeProviderShould {
    @Test
    public void startPaperWithWatchWolfAndRemoveItsVolumeOnStop() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        String folder = null;
        String instanceId = null;
        try (ItzgRuntimeProvider provider = new ItzgRuntimeProvider(new ItzgDockerServerInstantiator(),
                "itzg/minecraft-server", "java21")) {
            var launched = provider.startServer("Paper", "1.20.6", List.of(), WorldType.FLAT,
                    "1", List.of(), List.of());
            folder = launched.folder();
            instanceId = ServerRequirements.getHashFromServerPath(folder);
            assertFalse(Files.exists(Path.of(folder).resolve("server.jar")));
            ThrowableServer server = new ThrowableServer(launched.server());
            server.subscribeToServerStartedEvents(ready::countDown);
            server.subscribeToServerStoppedEvents(stopped::countDown);

            try {
                assertTrue(ready.await(7, TimeUnit.MINUTES), "Paper did not signal Minecraft readiness");
                String host = System.getenv("WATCHWOLF_TEST_DOCKER_HOST");
                if (host == null || host.isBlank()) host = System.getenv("MACHINE_IP");
                if (host == null || host.isBlank()) host = "127.0.0.1";
                int port = Integer.parseInt(server.getIp().substring(server.getIp().lastIndexOf(':') + 1));
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port + 1), 15_000);
                }
            } finally {
                server.stop();
                assertTrue(stopped.await(60, TimeUnit.SECONDS), "Container did not stop");
            }
        } finally {
            if (folder != null) ServerRequirements.clearFolder(folder);
        }

        DockerClient docker = DockerClientBuilder.getInstance(
                DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();
        String volume = "watchwolf-itzg-" + instanceId;
        assertThrows(NotFoundException.class, () -> docker.inspectVolumeCmd(volume).exec());
    }
}

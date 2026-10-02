package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerizedServerInstantiatorShould {
    /**
     * Docker reports container names with a leading '/'. Comparing them raw never matched, so
     * `closeAllLaunchedServers` skipped every container and left the Tester's servers running --
     * and the ports they held made the *next* run fail on connect.
     */
    @Test
    public void matchDockerNamesDespiteTheirLeadingSlash() {
        assertEquals("MC_Server-1700000000000", DockerizedServerInstantiator.stripDockerNamePrefix("/MC_Server-1700000000000"));
    }

    @Test
    public void leaveNamesWithoutASlashAlone() {
        assertEquals("MC_Server-1700000000000", DockerizedServerInstantiator.stripDockerNamePrefix("MC_Server-1700000000000"));
        assertNull(DockerizedServerInstantiator.stripDockerNamePrefix(null));
    }

    @Test
    public void ignoreContainersWithoutPublishedPorts() {
        Container noPorts = mock(Container.class);
        Container withUnpublishedPort = mock(Container.class);
        ContainerPort unpublished = new ContainerPort().withPrivatePort(25565);
        when(withUnpublishedPort.getPorts()).thenReturn(new ContainerPort[]{unpublished});

        assertEquals(0, DockerizedServerInstantiator.getUsedPublicPorts(
                List.of(noPorts, withUnpublishedPort)).size());
    }

    @Test
    public void preserveTheWindowsDriveLetterInALegacyFolderBind() {
        var bind = DockerizedServerInstantiator.serverFolderBind(Path.of("H:/WatchWolf/tmp/123"));
        assertEquals("H:/WatchWolf/tmp/123", bind.getPath());
        assertEquals("/server", bind.getVolume().getPath());
    }
}

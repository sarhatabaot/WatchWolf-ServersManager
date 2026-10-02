package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.utils.DockerUtilities;
import dev.watchwolf.serversmanager.server.ServerRequirements;
import dev.watchwolf.serversmanager.server.instantiator.Server;
import dev.watchwolf.serversmanager.server.instantiator.ServerInstantiator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LegacyRuntimeProviderShould {
    @Test
    public void preserveThePreparedJarAndJavaVersionContract() throws Exception {
        ServerInstantiator instantiator = mock(ServerInstantiator.class);
        Server server = mock(Server.class);
        List<Plugin> plugins = List.of();
        List<ConfigFile> maps = List.of();
        List<ConfigFile> configFiles = List.of();
        try (MockedStatic<ServerRequirements> requirements = mockStatic(ServerRequirements.class);
             MockedStatic<DockerUtilities> javaVersions = mockStatic(DockerUtilities.class)) {
            requirements.when(() -> ServerRequirements.setupFolder("Paper", "1.20", plugins,
                    WorldType.FLAT, "42", maps, configFiles, "server.jar")).thenReturn("tmp/123");
            javaVersions.when(() -> DockerUtilities.getJavaVersion("1.20")).thenReturn(17);
            when(instantiator.startServer(Paths.get("tmp/123"), "server.jar", 17)).thenReturn(server);

            LegacyRuntimeProvider provider = new LegacyRuntimeProvider(instantiator);
            MinecraftRuntimeProvider.StartedServer started = provider.startServer("Paper", "1.20",
                    plugins, WorldType.FLAT, "42", maps, configFiles);

            assertEquals("tmp/123", started.folder());
            assertSame(server, started.server());
            verify(instantiator).startServer(Paths.get("tmp/123"), "server.jar", 17);
            provider.close();
            verify(instantiator).close();
        }
    }

    @Test
    public void defaultToLegacyAndSelectItzgExplicitly() {
        assertInstanceOf(LegacyRuntimeProvider.class, MinecraftRuntimeProviders.select(null));
        assertInstanceOf(LegacyRuntimeProvider.class, MinecraftRuntimeProviders.select("legacy"));
        assertInstanceOf(ItzgRuntimeProvider.class, MinecraftRuntimeProviders.select("itzg"));
        assertThrows(IllegalArgumentException.class, () -> MinecraftRuntimeProviders.select("unknown"));
    }
}

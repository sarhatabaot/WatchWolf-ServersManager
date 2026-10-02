package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.utils.DockerUtilities;
import dev.watchwolf.serversmanager.server.ServerJarUnavailableException;
import dev.watchwolf.serversmanager.server.ServerRequirements;
import dev.watchwolf.serversmanager.server.instantiator.ServerInstantiator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

/** Existing prepared-JAR and Java-image runtime. */
public final class LegacyRuntimeProvider implements MinecraftRuntimeProvider {
    private static final String SERVER_JAR = "server.jar";
    private final ServerInstantiator serverInstantiator;

    public LegacyRuntimeProvider(ServerInstantiator serverInstantiator) {
        this.serverInstantiator = serverInstantiator;
    }

    @Override
    public StartedServer startServer(String serverType, String serverVersion, Collection<Plugin> plugins,
                                     WorldType worldType, String seed, Collection<ConfigFile> maps,
                                     Collection<ConfigFile> configFiles) throws IOException, ServerJarUnavailableException {
        String folder = ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType,
                seed, maps, configFiles, SERVER_JAR);
        System.out.println("Starting " + serverType + " " + serverVersion + " server on " + folder + "...");
        return new StartedServer(folder, serverInstantiator.startServer(
                Paths.get(folder), SERVER_JAR, DockerUtilities.getJavaVersion(serverVersion)));
    }

    @Override
    public void close() {
        serverInstantiator.close();
    }
}

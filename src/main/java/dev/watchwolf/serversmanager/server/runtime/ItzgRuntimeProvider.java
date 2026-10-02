package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.serversmanager.server.ServerProvisioningException;
import dev.watchwolf.serversmanager.server.ServerRequirements;
import dev.watchwolf.serversmanager.server.instantiator.ItzgDockerServerInstantiator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

public final class ItzgRuntimeProvider implements MinecraftRuntimeProvider {
    private final ItzgDockerServerInstantiator instantiator;
    private final String image;
    private final String tag;

    public ItzgRuntimeProvider(ItzgDockerServerInstantiator instantiator, String image, String tag) {
        this.instantiator = instantiator;
        this.image = image;
        this.tag = tag;
    }

    @Override
    public StartedServer startServer(String serverType, String serverVersion, Collection<Plugin> plugins,
                                     WorldType worldType, String seed, Collection<ConfigFile> maps,
                                     Collection<ConfigFile> configFiles) throws IOException {
        // Reject unsupported requests before preparing a world or allocating Docker resources.
        try {
            ItzgContainerSpec.paper(serverType, serverVersion, image, tag, "validation");
        } catch (IllegalArgumentException ex) {
            throw new ServerProvisioningException(ex.getMessage(), ex);
        }

        String folder = null;
        try {
            folder = ServerRequirements.setupFolderWithoutJar(serverType, serverVersion, plugins,
                    worldType, seed, maps, configFiles);
            String instanceId = ServerRequirements.getHashFromServerPath(folder);
            Path localFolder = Path.of(folder);
            ensureWatchWolfPlugin(localFolder.resolve("plugins"));
            ItzgContainerSpec spec = ItzgContainerSpec.paper(serverType, serverVersion, image, tag, instanceId);
            return new StartedServer(folder, instantiator.startServer(localFolder, spec));
        } catch (IOException | RuntimeException ex) {
            if (folder != null) {
                try {
                    ServerRequirements.clearFolder(folder);
                } catch (IOException cleanupFailure) {
                    ex.addSuppressed(cleanupFailure);
                }
            }
            throw new ServerProvisioningException("Couldn't provision " + serverType + " " + serverVersion
                    + " using itzg", ex);
        }
    }

    static void ensureWatchWolfPlugin(Path pluginsFolder) throws IOException {
        try (Stream<Path> files = Files.list(pluginsFolder)) {
            if (files.noneMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().startsWith("WatchWolf-")
                    && path.getFileName().toString().endsWith(".jar"))) {
                throw new IOException("WatchWolf-Server plugin is missing from " + pluginsFolder);
            }
        }
    }

    @Override
    public void close() {
        instantiator.close();
    }
}

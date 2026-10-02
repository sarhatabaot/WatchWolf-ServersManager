package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.serversmanager.server.ServerJarUnavailableException;
import dev.watchwolf.serversmanager.server.instantiator.Server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/** Prepares and launches one Minecraft instance without changing the RPC contract. */
public interface MinecraftRuntimeProvider extends Closeable {
    StartedServer startServer(String serverType, String serverVersion, Collection<Plugin> plugins,
                              WorldType worldType, String seed, Collection<ConfigFile> maps,
                              Collection<ConfigFile> configFiles) throws IOException, ServerJarUnavailableException;

    record StartedServer(String folder, Server server) {}

    @Override
    default void close() {}
}

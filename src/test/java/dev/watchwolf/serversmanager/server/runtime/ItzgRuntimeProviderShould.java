package dev.watchwolf.serversmanager.server.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ItzgRuntimeProviderShould {
    @TempDir Path plugins;

    @Test
    public void requireTheWatchWolfServerPluginBeforeLaunching() throws Exception {
        assertThrows(IOException.class, () -> ItzgRuntimeProvider.ensureWatchWolfPlugin(plugins));

        Files.createFile(plugins.resolve("WorldGuard-7.0.8.jar"));
        assertThrows(IOException.class, () -> ItzgRuntimeProvider.ensureWatchWolfPlugin(plugins));

        Files.createFile(plugins.resolve("WatchWolf-0.3.3-1.8-LATEST.jar"));
        assertDoesNotThrow(() -> ItzgRuntimeProvider.ensureWatchWolfPlugin(plugins));
    }
}
